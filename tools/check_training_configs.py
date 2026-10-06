"""Validate checked-in TOML configs and JSON plans without touching the simulator.

Nothing else in the repository parses all of `configs/`, so a malformed PPO or
model section, an unknown profile, stale experiment hash or overlapping evaluation seed ranges is
otherwise only discovered on the compute node after a job has been submitted.

Exit status is nonzero if any configuration fails. Historical configurations must
keep parsing, so this checks structure and contracts, not whether a configuration
is still a good experiment.

Usage:
    python tools/check_training_configs.py
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.curriculum import CURRICULUM_PROFILES_BY_ID  # noqa: E402
from sls.model import ModelConfig  # noqa: E402
from sls.rl.ppo import PPOConfig  # noqa: E402


def _digest(value: object, length: int = 64) -> bool:
    return isinstance(value, str) and re.fullmatch(f"[0-9a-fA-F]{{{length}}}", value) is not None


def _check_ranges(ranges: list[tuple[str, int, int]], limit: int) -> list[str]:
    problems = []
    for name, start, stop in ranges:
        if start < limit or stop <= start:
            problems.append(f"{name} has an invalid or training-overlapping seed range")
    for index, (name, start, stop) in enumerate(ranges):
        for other, lower, upper in ranges[index + 1:]:
            if start < upper and lower < stop:
                problems.append(f"{name} and {other} overlap")
    return problems


def validate_json(path: Path, payload: object) -> list[str]:
    """Check registered plans and directional exceptions without requalifying them."""
    problems: list[str] = []
    if path.parent.name == "compatibility":
        keys = {
            "state-preserving-source-transitions.json": ("from", "to"),
            "training-validation-transitions.json": (
                "from_source_tree_sha256", "to_training_validation_sha256"),
        }.get(path.name)
        if keys is None or not isinstance(payload, list):
            return ["unknown compatibility file or invalid record list"]
        seen = set()
        for record in payload:
            if not isinstance(record, dict):
                problems.append("compatibility record is not an object")
                continue
            pair = tuple(record.get(key) for key in keys)
            if not all(_digest(value) for value in pair):
                problems.append("compatibility record has an invalid SHA256")
                continue
            if pair in seen or pair[0] == pair[1]:
                problems.append("duplicate or self compatibility transition")
            seen.add(pair)
            if not record.get("reason"):
                problems.append("compatibility record has no reason")
            if "from_git_commit" in record and not _digest(record["from_git_commit"], 40):
                problems.append("invalid from_git_commit")
            if "review" in record and not (ROOT / record["review"]).is_file():
                problems.append("compatibility review does not exist")
        return problems
    if not isinstance(payload, dict):
        return ["experiment plan is not an object"]
    schema = payload.get("schema")
    if schema == "sls-act12-bound-plan-v1":
        historical = payload.get('status') == 'COMPLETED_HISTORICAL'
        if historical:
            evidence = payload.get('historical_evidence', {})
            original_path = ROOT / evidence['original_plan']
            manifest_path = ROOT / evidence['completed_manifest']
            for evidence_path, key in ((original_path, 'original_plan_sha256'),
                                       (manifest_path, 'completed_manifest_sha256')):
                actual = hashlib.sha256(evidence_path.read_bytes().replace(b'\r\n', b'\n')).hexdigest()
                if actual != evidence[key]:
                    problems.append(f'historical evidence hash changed: {key}')
            original = json.loads(original_path.read_text(encoding='utf-8'))
            restored_plan = {k: v for k, v in payload.items() if k != 'historical_evidence'}
            restored_plan['status'] = original.get('status')
            if restored_plan != original:
                problems.append('historical plan bindings differ from original evidence')
            manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
            if (manifest.get('status') != 'COMPLETE'
                    or manifest.get('training_implementation_sha256') !=
                        payload['target_training_implementation_sha256']
                    or manifest.get('config_sha256') != payload['config_sha256']
                    or manifest.get('native_source_sha256') != payload['parent']['target_native_source_sha256']):
                problems.append('historical completion identity mismatch')
        # Clean CI clones have no private parent weights; check the portable
        # bindings here. The submitter additionally verifies actual parent bytes.
        config = ROOT / payload["config"]
        recipe_path = ROOT / payload["recipe_path"]
        for path_key, hash_key in ((config, "config_sha256"),
                                   (recipe_path, "recipe_sha256")):
            actual = hashlib.sha256(path_key.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
            if actual != payload[hash_key]:
                problems.append(f"bound {hash_key} changed")
        recipe = json.loads(recipe_path.read_text(encoding="utf-8"))
        if payload["recipe"] != recipe:
            problems.append("bound recipe differs from registered recipe")
        for name, digest in (() if historical else payload.get("operator_sources", {}).items()):
            actual = hashlib.sha256((ROOT / name).read_bytes().replace(b"\r\n", b"\n")).hexdigest()
            if actual != digest:
                problems.append(f"bound operator source changed: {name}")
        problems.extend(validate_json(recipe_path, recipe))
        run = tomllib.loads(config.read_text(encoding="utf-8"))
        if (run["warm_start"]["checkpoint_sha256"] != payload["parent"]["sha256"]
                or run["stages"]["train"]["target_environment_steps"]
                != payload["parent"]["environment_steps"] + recipe["additional_decisions"]):
            problems.append("parent or cumulative budget disagrees with bound plan")
        from sls.rl.training_contract import training_implementation_digest
        if not historical and training_implementation_digest() != payload["target_training_implementation_sha256"]:
            problems.append("bound implementation changed")
        return problems
    budget = schema in {"sls-win-70m-budget-experiment-v1", "sls-win-90m-budget-experiment-v1"}
    if not budget and schema != "sls-act12-pilot-recipe-v1":
        return [f"unsupported experiment schema: {schema!r}"]
    limit = int(payload.get("training_seed_limit", 2_000_000_000_000))
    if not 0 <= int(payload["training_seed"]) < limit:
        problems.append("training_seed is outside its namespace")
    ranges = []
    if budget:
        config_path = ROOT / payload["config"]
        actual = hashlib.sha256(config_path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
        if actual != payload["config_sha256"]:
            problems.append("experiment config_sha256 does not match the LF-normalized config")
        run = tomllib.loads(config_path.read_text(encoding="utf-8"))["run"]
        limit = int(run.get("training_seed_limit", limit))
        if not 0 <= int(payload["training_seed"]) < limit:
            problems.append("training_seed is outside the config training namespace")
        for name, prefix in (("periodic_selection_seeds", "periodic"),
                             ("development_confirmation_seeds", "final")):
            start, stop = payload[name]
            expected = [run[f"{prefix}_evaluation_seed_start"],
                        run[f"{prefix}_evaluation_seed_start"] + run[f"{prefix}_evaluation_seed_count"]]
            if [start, stop] != expected:
                problems.append(f"{name} disagrees with training config")
            ranges.append((name, start, stop))
        if payload["training_seed"] != run["seed"]:
            problems.append("training_seed disagrees with training config")
        if payload.get("evidence") and not (ROOT / payload["evidence"]).is_file():
            problems.append("experiment evidence does not exist")
    else:
        for key in ("source_profile", "target_profile"):
            if payload[key] not in CURRICULUM_PROFILES_BY_ID:
                problems.append(f"unknown {key}")
        for prefix in ("periodic", "confirmation"):
            start = payload[f"{prefix}_seed_start"]
            ranges.append((prefix, start, start + payload[f"{prefix}_seed_count"]))
        if not (ROOT / payload["simulator_transition_evidence"]).is_file():
            problems.append("simulator transition evidence does not exist")
    ranges.append(("reserved_final_seeds", *payload["reserved_final_seeds"]))
    problems.extend(_check_ranges(ranges, limit))
    if int(payload["additional_decisions"]) <= 0:
        problems.append("additional_decisions must be positive")
    return problems


def _seed_range(run: dict, *, key: str, count_key: str) -> tuple[int, int] | None:
    start = run.get(key)
    if start is None:
        return None
    count = int(run.get(count_key, 0))
    if count <= 0:
        raise ValueError(f"{key} is set but {count_key} is not positive")
    return int(start), int(start) + count


def validate_auxiliary(path: Path, payload: dict) -> list[str]:
    """Validate a non-training configuration that points at a training config.

    `configs/diagnostics/*.toml` uses a flat schema, so it cannot be validated as
    a run configuration, but its references must still resolve.
    """

    problems: list[str] = []
    profile = payload.get("profile")
    if profile is not None and profile not in CURRICULUM_PROFILES_BY_ID:
        problems.append(f"unknown curriculum profile: {profile!r}")
    referenced = payload.get("preflight_config")
    if referenced is not None:
        if not (ROOT / str(referenced)).is_file():
            problems.append(f"preflight_config does not exist: {referenced}")
        else:
            preflight = tomllib.loads((ROOT / str(referenced)).read_text(encoding="utf-8"))
            referenced_profile = (preflight.get("run") or {}).get("profile")
            if profile is not None and referenced_profile != profile:
                problems.append(
                    "profile does not match its preflight_config "
                    f"({profile!r} vs {referenced_profile!r})"
                )
    pinned = payload.get("native_source_sha256")
    if pinned is not None and not _digest(pinned):
        problems.append("native_source_sha256 is not a SHA256 digest")
    return problems


def validate(path: Path, payload: dict) -> list[str]:
    run = payload.get("run") or {}
    if not run and "ppo" not in payload:
        # Not a training run configuration.
        return validate_auxiliary(path, payload)
    problems: list[str] = []
    stages = payload.get("stages") or {}

    if "ppo" not in payload:
        problems.append("missing [ppo] section")
    try:
        PPOConfig(**payload["ppo"])
    except (KeyError, TypeError, ValueError) as error:
        problems.append(f"[ppo] is invalid: {error}")
    if "model" in payload:
        try:
            ModelConfig(**payload["model"])
        except (TypeError, ValueError) as error:
            problems.append(f"[model] is invalid: {error}")

    profiles = [run.get("profile")]
    profiles += [stage.get("profile") for stage in stages.values()]
    for profile in profiles:
        if profile is not None and profile not in CURRICULUM_PROFILES_BY_ID:
            problems.append(f"unknown curriculum profile: {profile!r}")

    training = int(run.get("seed", 0))
    training_limit = run.get("training_seed_limit")
    held_out: list[tuple[str, int, int]] = []
    for key, count_key in (
        ("periodic_evaluation_seed_start", "periodic_evaluation_seed_count"),
        ("final_evaluation_seed_start", "final_evaluation_seed_count"),
        ("diagnostic_evaluation_seed_start", "diagnostic_evaluation_seed_count"),
    ):
        try:
            found = _seed_range(run, key=key, count_key=count_key)
        except ValueError as error:
            problems.append(str(error))
            continue
        if found is not None:
            held_out.append((key, *found))

    for name, start, stop in held_out:
        if start < training:
            problems.append(f"{name} starts below the training seed base ({start} < {training})")
        if training_limit is not None and start < int(training_limit):
            problems.append(
                f"{name} starts inside the training seed namespace "
                f"({start} < training_seed_limit {training_limit})"
            )
    for index, (name_a, start_a, stop_a) in enumerate(held_out):
        for name_b, start_b, stop_b in held_out[index + 1:]:
            if start_a < stop_b and start_b < stop_a:
                problems.append(f"{name_a} and {name_b} overlap")

    if not run.get("output"):
        problems.append("[run] has no output directory")
    return problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root", type=Path, default=ROOT / "configs",
        help="directory tree to scan for TOML configs and JSON plans/contracts",
    )
    args = parser.parse_args(argv)

    paths = sorted([*args.root.rglob("*.toml"), *args.root.rglob("*.json")])
    if not paths:
        raise SystemExit(f"no configurations found under {args.root}")

    failures = 0
    for path in paths:
        try:
            if path.suffix == ".json":
                payload = json.loads(path.read_text(encoding="utf-8"))
                problems = validate_json(path, payload)
            else:
                payload = tomllib.loads(path.read_text(encoding="utf-8"))
                problems = validate(path, payload)
        except Exception as error:  # noqa: BLE001 - report, never crash the scan
            problems = [f"unreadable: {type(error).__name__}: {error}"]
        resolved = path.resolve()
        relative = (resolved.relative_to(ROOT).as_posix() if resolved.is_relative_to(ROOT)
                    else resolved.as_posix())
        if problems:
            failures += 1
            print(f"FAIL {relative}")
            for problem in problems:
                print(f"     {problem}")
        else:
            print(f"ok   {relative}")
    print(f"\n{len(paths) - failures}/{len(paths)} configurations valid")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
