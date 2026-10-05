"""Is the Neow headroom learnable from the offered blessings, or does it need foresight?

``tools/intervene_policy_decision.py`` reports, for each seed, whether each of the
four forced blessing choices wins that run. Taking the best choice per seed is a
perfect-foresight oracle, so it is only an upper bound: a policy at the Neow
screen sees the four offers (their bonus and drawback flags) but not the future,
and part of that gap may be irreducible.

This script measures what is actually learnable. It turns the intervention output
into a supervised problem -- one row per (seed, option), the public offer flags as
features, "does forcing this option win this run" as the label -- and reports the
cross-validated win rate of the implied offer-conditional policy, grouped so that
no seed appears in both train and test. That is an implementation-free estimate of
the headroom reachable from the observable offer alone, holding the rest of the
policy fixed.

The offers themselves are read from the policy-visible observation, so the probe
uses exactly the information the policy has and nothing more.

Usage:
    python tools/audit_neow_headroom.py \\
        --intervention local/reports/neow.jsonl \\
        --seed-start 8000000000000 --seeds 512 --shards 8
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import numpy as np  # noqa: E402

from sls.content.neow import NEOW_BONUSES, NEOW_DRAWBACKS  # noqa: E402
from sls.curriculum import CURRICULUM_PROFILES_BY_ID  # noqa: E402
from sls.rl.workers import ShardedWorkerPool  # noqa: E402

OPTIONS = (0, 1, 2, 3)
FEATURE_NAMES = (
    *[f"bonus:{name}" for name in NEOW_BONUSES],
    *[f"drawback:{name}" for name in NEOW_DRAWBACKS],
)
UNKNOWN_OFFERS: list[tuple] = []


def collect_offers(seeds: list[int], profile, shards: int) -> dict[int, list[tuple]]:
    """Read each seed's four offers from the policy-visible observation."""

    with ShardedWorkerPool(profile, len(seeds), shard_count=shards) as pool:
        decisions = pool.reset(seeds)
    offers: dict[int, list[tuple]] = {}
    for seed, decision in zip(seeds, decisions):
        properties = {
            entity.instance_id: dict(entity.properties)
            for entity in decision.observation.event_options
        }
        per_option: list[tuple] = []
        for index in OPTIONS:
            props = properties.get(f"event-option:{index}", {})
            # sls.content.neow exposes the flags lower-cased; the enum tuples are
            # upper-cased, so normalize before lookup. An unset flag is absent,
            # not False.
            bonus = next(
                (key[len("neow_bonus_"):].upper() for key, on in props.items()
                 if key.startswith("neow_bonus_") and on), None,
            )
            drawback = next(
                (key[len("neow_drawback_"):].upper() for key, on in props.items()
                 if key.startswith("neow_drawback_") and on), None,
            )
            per_option.append((bonus, drawback))
        offers[seed] = per_option
    return offers


def feature_row(offer: tuple) -> list[float]:
    """One-hot the public offer flags; an unknown offer is recorded, not guessed."""

    bonus, drawback = offer
    row = [0.0] * len(FEATURE_NAMES)
    if bonus in NEOW_BONUSES:
        row[NEOW_BONUSES.index(bonus)] = 1.0
    if drawback in NEOW_DRAWBACKS:
        row[len(NEOW_BONUSES) + NEOW_DRAWBACKS.index(drawback)] = 1.0
    if (bonus not in NEOW_BONUSES) or (drawback not in NEOW_DRAWBACKS):
        UNKNOWN_OFFERS.append(offer)
    return row


def load_arm_outcomes(paths: list[Path]) -> dict[int, dict[int, bool]]:
    """seed -> option -> won, from recorded intervention arms."""

    outcomes: dict[int, dict[int, bool]] = {}
    for path in paths:
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            record = json.loads(line)
            arm = record.get("arm")
            if arm is None or not str(arm).isdigit():
                continue
            option = int(arm)
            for row in record["per_seed"]:
                outcomes.setdefault(int(row["seed"]), {})[option] = bool(row["success"])
    return outcomes


def exact_mcnemar(b: int, c: int) -> float:
    n = b + c
    if n == 0:
        return 1.0
    tail = sum(math.comb(n, i) for i in range(0, min(b, c) + 1)) / 2 ** n
    return min(1.0, 2 * tail)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--intervention", type=Path, action="append", required=True,
        help="a JSONL file written by tools/intervene_policy_decision.py",
    )
    parser.add_argument("--seed-start", type=int, required=True)
    parser.add_argument("--seeds", type=int, default=512)
    parser.add_argument("--shards", type=int, default=8)
    parser.add_argument(
        "--profile", choices=tuple(sorted(CURRICULUM_PROFILES_BY_ID)),
        default="IRONCLAD_A20_ACT1",
    )
    parser.add_argument("--folds", type=int, default=5)
    args = parser.parse_args(argv)

    outcomes = load_arm_outcomes(args.intervention)
    seeds = [
        seed for seed in range(args.seed_start, args.seed_start + args.seeds)
        if all(option in outcomes.get(seed, {}) for option in OPTIONS)
    ]
    if len(seeds) < 40:
        raise ValueError(
            f"need at least 40 seeds with all four forced arms recorded, got {len(seeds)}"
        )
    print(f"seeds with all four forced arms: {len(seeds)}")

    profile = CURRICULUM_PROFILES_BY_ID[args.profile]
    offers = collect_offers(seeds, profile, args.shards)

    labels = np.zeros((len(seeds), len(OPTIONS)), dtype=np.int64)
    features = np.zeros(
        (len(seeds), len(OPTIONS), len(FEATURE_NAMES)), dtype=np.float64,
    )
    for row, seed in enumerate(seeds):
        for column, option in enumerate(OPTIONS):
            labels[row, column] = int(outcomes[seed][option])
            features[row, column] = feature_row(offers[seed][option])

    flat_x = features.reshape(-1, len(FEATURE_NAMES))
    flat_y = labels.reshape(-1)
    print(f"supervised rows: {flat_x.shape[0]}  positive rate: {flat_y.mean():.4f}")
    if UNKNOWN_OFFERS:
        print(f"WARNING unknown offers encountered (features left zeroed): "
              f"{sorted(set(UNKNOWN_OFFERS))[:8]}")

    offer_marginals = features.sum(axis=0)
    print("\noffer-flag availability per option slot (share of seeds):")
    for column, option in enumerate(OPTIONS):
        top = np.argsort(-offer_marginals[column])[:3]
        described = ", ".join(
            f"{FEATURE_NAMES[index]}={offer_marginals[column][index] / len(seeds):.2f}"
            for index in top
        )
        print(f"  slot {option}: {described}")

    from sklearn.ensemble import HistGradientBoostingClassifier
    from sklearn.linear_model import LogisticRegression
    from sklearn.model_selection import GroupKFold

    splits = list(GroupKFold(n_splits=args.folds).split(
        flat_x, flat_y, groups=np.repeat(np.arange(len(seeds)), len(OPTIONS)),
    ))

    results: dict[str, np.ndarray] = {}
    for name, factory in (
        ("logistic", lambda: LogisticRegression(max_iter=2000, C=1.0)),
        ("gradient-boosting", lambda: HistGradientBoostingClassifier(
            max_depth=3, max_iter=200, learning_rate=0.05, l2_regularization=1.0,
            random_state=0,
        )),
    ):
        chosen = np.full(len(seeds), -1, dtype=np.int64)
        best = np.full(len(seeds), -np.inf)
        for train, test in splits:
            model = factory()
            model.fit(flat_x[train], flat_y[train])
            scores = model.predict_proba(flat_x[test])[:, 1]
            # flat_x is seed-major; GroupKFold keeps whole seeds together but does
            # not guarantee ascending index order, so map each row back to its seed.
            for row, score in zip(test.tolist(), scores.tolist()):
                seed_index, option = divmod(row, len(OPTIONS))
                if score > best[seed_index]:
                    best[seed_index] = score
                    chosen[seed_index] = option
        if (chosen < 0).any():
            raise RuntimeError("cross-validation did not score every seed")
        results[name] = chosen

    constant = labels[:, 1]  # the arm the policy already behaves like
    oracle = labels.max(axis=1)
    print()
    print(f"{'policy':<34} {'wins':>6} {'rate':>8} {'paired vs constant-1':>24}")
    print(f"{'constant: option 1':<34} {constant.sum():>6} "
          f"{constant.mean() * 100:>7.2f}%")
    for name, chosen in results.items():
        picked = labels[np.arange(len(seeds)), chosen]
        b = int((constant & ~picked).sum())
        c = int((~constant & picked).sum())
        print(f"{'learned from offer: ' + name:<34} {picked.sum():>6} "
              f"{picked.mean() * 100:>7.2f}% "
              f"({b} lost / {c} won, p={exact_mcnemar(b, c):.3f})")
    print(f"{'perfect-foresight oracle':<34} {oracle.sum():>6} "
          f"{oracle.mean() * 100:>7.2f}%")

    print()
    print("Interpretation: these are particular cross-validated probes, not an upper "
          "bound on all offer-conditional policies. The oracle assumes foresight; "
          "its gap proves neither learnable headroom nor the impossibility of "
          "a better conditional policy.")

    # Per-cell rates are the interpretable form of the same question, and they do
    # not depend on a model's inductive bias or on cross-validation noise.
    print("\n=== empirical win rate by (slot, bonus), all seeds ===")
    for column, option in enumerate(OPTIONS):
        print(f"  slot {option}:")
        cells: dict[str, list[int]] = {}
        for row, seed in enumerate(seeds):
            bonus = offers[seed][option][0] or "?"
            cells.setdefault(bonus, []).append(int(labels[row, column]))
        for bonus, values in sorted(cells.items(), key=lambda kv: -len(kv[1])):
            rate = sum(values) / len(values)
            print(f"    {bonus:<24} n={len(values):>4} win={rate * 100:>6.2f}%")

    # A single-feature rule is the cheapest possible conditional policy. If none
    # of these beats taking slot 1 unconditionally, the slot choice carries no
    # exploitable offer signal at this sample size.
    print("\n=== best single-feature override rule ===")
    print(f"{'rule':<46} {'n overridden':>13} {'wins':>6} {'rate':>8}")
    baseline = labels[:, 1]
    print(f"{'always slot 1 (policy behaviour)':<46} {0:>13} {baseline.sum():>6} "
          f"{baseline.mean() * 100:>7.2f}%")
    for index, name in enumerate(FEATURE_NAMES):
        for preferred in OPTIONS:
            if preferred == 1:
                continue
            overridden = 0
            wins = 0
            for row, seed in enumerate(seeds):
                if features[row, preferred, index] > 0:
                    overridden += 1
                    wins += int(labels[row, preferred])
                else:
                    wins += int(labels[row, 1])
            if overridden < 20:
                continue
            rate = wins / len(seeds)
            if rate > baseline.mean() + 0.005:
                print(f"{name + ' -> slot ' + str(preferred):<46} {overridden:>13} "
                      f"{wins:>6} {rate * 100:>7.2f}%")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
