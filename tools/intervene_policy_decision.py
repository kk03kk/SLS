"""Measure what a single decision point is worth by intervening on it.

The project's diagnostics are correlational: failed runs entered the boss with
less HP, successful runs carried more potions. Turning such a correlation into a
causal estimate needs an intervention, and a single decision point is cheap to
intervene on: hold the checkpoint fixed, hold the seed fixed, force the action at
one screen type, and let the unmodified greedy policy play the rest of the run.

Because the seed determines everything else, every arm is paired with every
other arm on the same seeds, so the comparison is a paired McNemar test rather
than two independent proportion estimates.

Interventions are written ``SCREEN:SELECTOR``:

    NEOW:option:event-option:1     force Neow's second offer
    CARD_REWARD:kind:SKIP_CARD_REWARD
    REST:kind:REST
    CARD_REWARD:subject-prefix:reward-card:0
    MAP:kind:CHOOSE_MAP_NODE        (no-op unless narrowed further)

``SCREEN`` matches ``Observation.screen.value``. Selectors:

* ``kind:K``            the action whose ``ActionKind`` is ``K``
* ``option:ID``         the action whose ``option_id`` is ``ID``
* ``subject-prefix:P``  the first action whose ``subject_id`` starts with ``P``
* ``target-none``       the first action whose ``target_id`` is ``None``

Only the first matching action is forced. If no action matches, the decision is
left to the policy and the fallback is counted, so a mistyped selector shows up
as a nonzero fallback instead of silently becoming a control run.

Usage::

    python tools/intervene_policy_decision.py \\
        --checkpoint local/runs/<run>/stages/train/selection/best_progress.pt \\
        --profile IRONCLAD_A20_ACT1 --seed-start 8000000000000 --episodes 512 \\
        --arm control --arm NEOW:option:event-option:1 \\
        --arm NEOW:option:event-option:3 --output local/reports/neow.jsonl

Add ``--report`` to print the paired analysis and the perfect-foresight oracle
bound over the arms already present in the output file.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch  # noqa: E402

from sls.curriculum import CURRICULUM_PROFILES_BY_ID  # noqa: E402
from sls.model import PolicyBatch  # noqa: E402
from sls.model.encoding import ACTION_TYPE_IDS  # noqa: E402
from sls.rl import policy_from_training_checkpoint  # noqa: E402
from sls.rl.episode_limit import EpisodeLimitState  # noqa: E402
from sls.rl.training_contract import evaluation_identity, sha256_file  # noqa: E402
from sls.rl.workers import ShardedWorkerPool  # noqa: E402

CONTROL_LABEL = "control"


@dataclass(frozen=True, slots=True)
class Intervention:
    """One forced action choice at one screen type."""

    screen: str
    selector: str
    argument: str | None
    spec: str

    @classmethod
    def parse(cls, spec: str) -> "Intervention":
        parts = spec.split(":", 2)
        if len(parts) < 2 or not parts[0] or not parts[1]:
            raise ValueError(
                f"intervention must look like SCREEN:SELECTOR[:ARGUMENT], got {spec!r}"
            )
        screen, selector = parts[0].upper(), parts[1].lower()
        argument = parts[2] if len(parts) > 2 else None
        if selector not in {"option", "kind", "subject-prefix", "target-none"}:
            raise ValueError(f"unsupported intervention selector: {selector!r}")
        if selector in {"option", "kind", "subject-prefix"} and not argument:
            raise ValueError(f"intervention {spec!r} requires an argument")
        return cls(screen, selector, argument, spec)

    def index_for(self, decision) -> int | None:
        actions = decision.actions
        if decision.observation.screen.value != self.screen:
            return None
        for index, action in enumerate(actions):
            if self.selector == "kind" and action.kind.value == self.argument:
                return index
            if self.selector == "option" and action.option_id == self.argument:
                return index
            if self.selector == "subject-prefix" and (action.subject_id or "").startswith(
                self.argument or ""
            ):
                return index
            if self.selector == "target-none" and action.target_id is None:
                return index
        return None


def _wilson(k: int, n: int) -> tuple[float, float]:
    if n <= 0:
        return (0.0, 1.0)
    z = 1.959963984540054
    p = k / n
    denominator = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denominator
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator
    return centre - half, centre + half


def exact_mcnemar(b: int, c: int) -> float:
    """Two-sided exact McNemar p-value (binomial test on discordant pairs)."""

    n = b + c
    if n == 0:
        return 1.0
    tail = sum(math.comb(n, i) for i in range(0, min(b, c) + 1)) / 2 ** n
    return min(1.0, 2 * tail)


@torch.no_grad()
def run_arm(
    model,
    profile,
    seeds: list[int],
    intervention: Intervention | None,
    *,
    device: str,
    shards: int,
    max_steps: int,
    max_boundary_visits: int,
) -> dict:
    size = len(seeds)
    model.eval().to(device)
    with ShardedWorkerPool(profile, size, shard_count=shards) as pool:
        decisions = pool.reset(seeds)
        memory = model.initial_memory(size, device)
        starts = torch.ones(size, dtype=torch.bool, device=device)
        previous_actions = torch.zeros(size, dtype=torch.long, device=device)
        previous_rewards = torch.zeros(size, dtype=torch.float32, device=device)
        limits = [EpisodeLimitState.initial(d) for d in decisions]
        active = list(range(size))
        steps = [0] * size
        won = [False] * size
        reasons = ["timeout"] * size
        floors = [0] * size
        matched = 0
        fallback = 0
        candidate_counts: dict[str, int] = {}
        started = time.monotonic()
        # One iteration beyond the step limit, matching src/sls/rl/evaluate.py.
        for _ in range(max_steps + 1):
            if not active:
                break
            batch = PolicyBatch.from_decisions(
                (decisions[i] for i in active), model.config,
            ).to(device)
            output = model(
                *batch.model_inputs(),
                memory=memory[active],
                episode_start_mask=starts[active],
                previous_action_types=previous_actions[active],
                previous_rewards=previous_rewards[active],
            )
            memory[active] = output.next_memory
            starts[active] = False
            logits = output.logits
            picks = logits.argmax(dim=1).cpu().tolist()
            if intervention is not None:
                logits = logits.clone()
                for row, index in enumerate(active):
                    decision = decisions[index]
                    if decision.observation.screen.value != intervention.screen:
                        continue
                    candidate_counts[str(len(decision.actions))] = (
                        candidate_counts.get(str(len(decision.actions)), 0) + 1
                    )
                    target = intervention.index_for(decision)
                    if target is None:
                        fallback += 1
                        continue
                    matched += 1
                    logits[row] = torch.finfo(logits.dtype).min
                    logits[row, target] = 0.0
                picks = logits.argmax(dim=1).cpu().tolist()
            sparse: list[str | None] = [None] * size
            for row, index in enumerate(active):
                sparse[index] = (
                    decisions[index].actions[int(picks[row])].candidate_id
                )
            transitions = pool.step_sparse(sparse)
            still_active = []
            for row, index in enumerate(active):
                transition = transitions[index]
                if transition is None:
                    raise RuntimeError("active slot was not stepped")
                action = decisions[index].actions[int(picks[row])]
                steps[index] += 1
                previous_actions[index] = ACTION_TYPE_IDS[action.kind.value] + 1
                previous_rewards[index] = float(transition.reward)
                decisions[index] = transition.decision
                if transition.terminated or transition.truncated:
                    won[index] = bool(transition.info.get("success"))
                    reasons[index] = str(
                        transition.info.get("reason") or "backend_truncation"
                    )
                    floors[index] = transition.decision.observation.run.floor
                    continue
                limit = limits[index].observe(
                    transition.decision,
                    max_steps=max_steps,
                    max_boundary_visits=max_boundary_visits,
                )
                if limit is not None:
                    reasons[index] = limit
                    floors[index] = transition.decision.observation.run.floor
                    continue
                still_active.append(index)
            active = still_active
        for index in active:
            floors[index] = decisions[index].observation.run.floor
    successes = sum(won)
    return {
        "arm": CONTROL_LABEL if intervention is None else intervention.spec,
        "episodes": size,
        "successes": successes,
        "success_rate": successes / size,
        "ci95": list(_wilson(successes, size)),
        "reasons": {r: reasons.count(r) for r in sorted(set(reasons))},
        "mean_steps": sum(steps) / size,
        "intervention_matches": matched,
        "intervention_fallbacks": fallback,
        "intervention_candidate_counts": candidate_counts,
        "elapsed_seconds": time.monotonic() - started,
        "per_seed": [
            {"seed": s, "success": w, "reason": r, "steps": st, "floor": f}
            for s, w, r, st, f in zip(seeds, won, reasons, steps, floors)
        ],
    }


def _load_arms(path: Path) -> dict[str, dict]:
    arms: dict[str, dict] = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.strip():
                record = json.loads(line)
                arms[record["arm"]] = record
    return arms


def report(arms: dict[str, dict]) -> None:
    control = arms.get(CONTROL_LABEL)
    print(f"\n{'arm':<44} {'episodes':>8} {'wins':>6} {'rate':>8} {'95% CI':>18}")
    for name in sorted(arms, key=lambda n: (n != CONTROL_LABEL, n)):
        a = arms[name]
        print(f"{name:<44} {a['episodes']:>8} {a['successes']:>6} "
              f"{a['success_rate'] * 100:>7.2f}% "
              f"[{a['ci95'][0] * 100:5.2f}, {a['ci95'][1] * 100:5.2f}]")
    if control is None:
        print("\n(no control arm recorded; run one to enable paired comparisons)")
        return
    reference = {row["seed"]: row["success"] for row in control["per_seed"]}
    print(f"\n=== paired exact McNemar against the {CONTROL_LABEL} arm ===")
    print(f"{'arm':<44} {'ctrl win/arm lose':>18} {'ctrl lose/arm win':>18} "
          f"{'net':>6} {'p':>9}")
    for name in sorted(arms):
        if name == CONTROL_LABEL:
            continue
        other = {row["seed"]: row["success"] for row in arms[name]["per_seed"]}
        common = sorted(set(reference) & set(other))
        b = sum(reference[s] and not other[s] for s in common)
        c = sum(not reference[s] and other[s] for s in common)
        print(f"{name:<44} {b:>18} {c:>18} {c - b:>+6} {exact_mcnemar(b, c):>9.4f}")

    maps = {
        name: {row["seed"]: row["success"] for row in arm["per_seed"]}
        for name, arm in arms.items()
    }
    seeds = sorted(set.intersection(*(set(m) for m in maps.values())))
    oracle = sum(any(m[s] for m in maps.values()) for s in seeds)
    always = sum(all(m[s] for m in maps.values()) for s in seeds)
    control_wins = sum(maps[CONTROL_LABEL][s] for s in seeds)
    print(f"\n=== perfect-foresight oracle over all recorded arms "
          f"({len(seeds)} paired seeds) ===")
    print(f"  control policy                : {control_wins:>5} "
          f"({control_wins / len(seeds) * 100:5.2f}%)")
    print(f"  best arm per seed (oracle)    : {oracle:>5} "
          f"({oracle / len(seeds) * 100:5.2f}%)  "
          f"headroom {(oracle - control_wins) / len(seeds) * 100:+.2f}pp")
    print(f"  every arm succeeds            : {always:>5} "
          f"({always / len(seeds) * 100:5.2f}%)")
    print("\n  The oracle is an upper bound: it assumes perfect foresight of which "
          "arm suits\neach seed. It bounds the value of conditioning this decision, "
          "not an achievable\nwin rate.")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument(
        "--profile", choices=tuple(sorted(CURRICULUM_PROFILES_BY_ID)),
        default="IRONCLAD_A20_ACT1",
    )
    parser.add_argument("--seed-start", type=int, required=True)
    parser.add_argument("--episodes", type=int, default=512)
    parser.add_argument("--shards", type=int, default=16)
    parser.add_argument("--device", default="cuda" if torch.cuda.is_available() else "cpu")
    parser.add_argument("--max-steps", type=int, default=4096)
    parser.add_argument("--max-boundary-visits", type=int, default=4)
    parser.add_argument(
        "--arm", action="append", default=None,
        help="an intervention spec, or 'control' for the unmodified policy",
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--report", action="store_true",
        help="print the paired analysis of the arms already in --output and exit",
    )
    args = parser.parse_args(argv)

    if args.report:
        report(_load_arms(args.output))
        return 0

    arms_spec = args.arm or [CONTROL_LABEL]
    if args.episodes <= 0 or args.seed_start < 0:
        raise ValueError("evaluation seed range is invalid")
    if args.shards <= 0:
        raise ValueError("shard count must be positive")

    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    payload = torch.load(args.checkpoint, map_location="cpu", weights_only=False)
    model = policy_from_training_checkpoint(payload)
    profile = CURRICULUM_PROFILES_BY_ID[args.profile]
    seeds = list(range(args.seed_start, args.seed_start + args.episodes))
    identity = evaluation_identity(
        device=args.device, environment_shards=args.shards,
        ascension=profile.ascension,
    )
    contract = payload.get("contract") or {}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        raise ValueError(
            f"refusing to overwrite existing intervention evidence: {args.output}"
        )
    with args.output.open("a", encoding="utf-8") as stream:
        for spec in arms_spec:
            intervention = None
            if spec != CONTROL_LABEL:
                intervention = Intervention.parse(spec)
            result = run_arm(
                model, profile, seeds, intervention,
                device=args.device, shards=args.shards,
                max_steps=args.max_steps,
                max_boundary_visits=args.max_boundary_visits,
            )
            result["seed_range"] = [seeds[0], seeds[-1] + 1]
            result["profile"] = profile.profile_id
            result["checkpoint_sha256"] = sha256_file(args.checkpoint)
            result["checkpoint_native_source_sha256"] = contract.get("native_source_sha256")
            result.update(identity)
            stream.write(json.dumps(result, sort_keys=True) + "\n")
            stream.flush()
            summary = {k: v for k, v in result.items() if k != "per_seed"}
            print(json.dumps(summary, sort_keys=True), flush=True)
    report(_load_arms(args.output))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
