"""Collect independent Original/native samples for policy-transfer-v1."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend  # noqa: E402
from sls.backends.simulator import IRONCLAD_A0_ACT1  # noqa: E402
from sls.backends.simulator import native  # noqa: E402
from sls.rl.training_contract import canonical_digest  # noqa: E402
from sls.content.normalize import (  # noqa: E402
    normalize_card_id, normalize_content_id, normalize_encounter_id,
    normalize_event_id, normalize_potion_id,
)
from sls.validation.transfer_gate import STOCHASTIC_CATEGORIES  # noqa: E402


SCHEMA = "sls-stochastic-samples-v1"


def _normalize(category: str, value: object) -> list[str]:
    if category == "draw_shuffle":
        return [str(value)[0]]
    if category == "card_rewards":
        return [normalize_card_id(part.rstrip("+")) + ("+" if part.endswith("+") else "")
                for part in str(value).split("|")]
    normalizer = {
        "potion_rewards": normalize_potion_id,
        "relic_rewards": normalize_content_id,
        "random_events": normalize_event_id,
        "encounters": normalize_encounter_id,
    }[category]
    return [normalizer(value)]


def collect(base_seeds: list[int], samples_per_seed: int) -> dict[str, object]:
    if len(set(base_seeds)) < 32:
        raise ValueError("at least 32 independent base seeds are required")
    if samples_per_seed * len(base_seeds) < 2_000:
        raise ValueError("at least 2,000 samples per category are required")
    original = {category: [] for category in STOCHASTIC_CATEGORIES}
    simulator = {category: [] for category in STOCHASTIC_CATEGORIES}
    backend = OriginalBackend(profile=IRONCLAD_A0_ACT1)
    try:
        backend.reset(base_seeds[0])
        for seed in base_seeds:
            payload = backend.session.execute(
                f"parity_distribution {seed & ((1 << 64) - 1)} {samples_per_seed}"
            )
            scenario = dict(payload.get("_parity_scenario") or {})
            if scenario.get("source") != "STOCK_INDEPENDENT_STOCHASTIC_V1":
                raise RuntimeError("Original did not attest the stochastic probe")
            samples = dict(scenario.get("samples") or {})
            native_samples = native.stochastic_distribution_probe(seed, samples_per_seed)
            if set(samples) != STOCHASTIC_CATEGORIES or set(native_samples) != STOCHASTIC_CATEGORIES:
                raise RuntimeError("stochastic probe returned incomplete categories")
            for category in STOCHASTIC_CATEGORIES:
                for value in samples[category]:
                    original[category].extend(_normalize(category, value))
                for value in native_samples[category]:
                    simulator[category].extend(_normalize(category, value))
    finally:
        backend.return_to_menu()
    payload: dict[str, object] = {
        "schema": SCHEMA,
        "base_seeds": base_seeds,
        "samples_per_seed": samples_per_seed,
        "categories": {
            category: {
                "original": {"seeds": base_seeds, "samples": original[category]},
                "simulator": {"seeds": base_seeds, "samples": simulator[category]},
            }
            for category in sorted(STOCHASTIC_CATEGORIES)
        },
    }
    payload["samples_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-start", type=int, default=30_000)
    parser.add_argument("--seed-count", type=int, default=32)
    parser.add_argument("--samples-per-seed", type=int, default=63)
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "runs/policy_transfer_stochastic_samples.json",
    )
    args = parser.parse_args()
    payload = collect(
        list(range(args.seed_start, args.seed_start + args.seed_count)),
        args.samples_per_seed,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    temporary.replace(args.output)
    print(json.dumps({
        "output": str(args.output), "samples_sha256": payload["samples_sha256"],
        "samples_per_category": args.seed_count * args.samples_per_seed,
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion

    try:
        code = main()
    except Exception as error:
        write_completion(
            2, entry="stochastic-transfer",
            error=f"{type(error).__name__}: {error}", argv=sys.argv,
        )
        raise
    else:
        write_completion(code, entry="stochastic-transfer")
        raise SystemExit(code)
