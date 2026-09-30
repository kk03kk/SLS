"""Recompute the old audit's statistical claims without trusting its prose."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from compare_run_arms import exact_mcnemar


def rows(path: Path) -> dict[int, dict]:
    payload = json.loads(path.read_text(encoding="utf-8"))["result"]
    results = payload["seed_results"]
    mapping = {int(row["seed"]): row for row in results}
    if len(mapping) != len(results) or sum(row["success"] for row in results) != payload["successes"]:
        raise ValueError(f"invalid seed evidence: {path}")
    return mapping


def main() -> int:
    from scipy.stats import chi2_contingency, fisher_exact

    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    audit = ROOT / "local/audit-2026-09-29"
    dev = rows(audit / "eval-dev-8e12.json")
    current = rows(audit / "eval-final-7e12-current-env.json")
    original = rows(ROOT / "local/runs/ironclad-a20-act1-v4-60m-stable/final-evaluation.json")
    if set(original) != set(current) or set(dev) & set(current):
        raise ValueError("unexpected seed pairing")
    a, n = sum(row["success"] for row in dev.values()), len(dev)
    b, m = sum(row["success"] for row in current.values()), len(current)
    pooled = (a + b) / (n + m)
    z = (a / n - b / m) / math.sqrt(pooled * (1 - pooled) * (1 / n + 1 / m))
    lost = sum(original[s]["success"] and not current[s]["success"] for s in original)
    gained = sum(not original[s]["success"] and current[s]["success"] for s in original)
    reward = sum(1 if row["success"] else -1 + 0.8 * min(16, row["floor"]) / 16
                 for row in original.values()) / len(original)
    code = ROOT / "local/audits/stock-decompilation-tree/desktop-1.0/source/com/megacrit/cardcrawl"
    source_paths = [code / "actions/common/ApplyPowerAction.java", code / "powers/AbstractPower.java",
                    code / "powers/StrengthPower.java", code / "powers/WeakPower.java",
                    code / "powers/PenNibPower.java", code / "cards/AbstractCard.java"]
    source = {path.name: path.read_text(encoding="utf-8") for path in source_paths}
    assertions = {
        "powers_sorted_after_insertion": "Collections.sort(this.target.powers);" in source["ApplyPowerAction.java"],
        "priority_comparator": "return this.priority - other.priority;" in source["AbstractPower.java"],
        "default_strength_priority_5": "public int priority = 5;" in source["AbstractPower.java"]
        and "this.priority" not in source["StrengthPower.java"],
        "pen_nib_priority_6": "this.priority = 6;" in source["PenNibPower.java"],
        "weak_priority_99": "this.priority = 99;" in source["WeakPower.java"],
    }
    if not all(assertions.values()):
        raise ValueError("stock power priority evidence changed")
    # Normal approximation power is an assumption about discordance, not a
    # property of sample count alone. The observed screen has q=104/512.
    q = 104 / 512
    power = {}
    def cdf(x: float) -> float:
        return (1 + math.erf(x / math.sqrt(2))) / 2
    for count in (512, 2048, 4096):
        effect = 0.03 * math.sqrt(count / q)
        power[str(count)] = 1 - cdf(1.959963984540054 - effect) + cdf(-1.959963984540054 - effect)
    result = {
        "schema": "sls-audit-independent-recheck-v1",
        "stock_projection_assertions": assertions,
        "stock_projection_sha256": {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                                    for path in source_paths},
        "stock_bytecode_executed": False,
        "unpaired_seed_blocks": {"dev": [a, n], "current": [b, m], "z": z,
                                 "fisher_p": fisher_exact([[a, n - a], [b, m - b]]).pvalue,
                                 "pearson_p": chi2_contingency([[a, n - a], [b, m - b]], correction=False).pvalue},
        "same_seed_environment_comparison": {"lost": lost, "gained": gained,
                                             "exact_mcnemar_p": exact_mcnemar(lost, gained)},
        "objective": {"original_terminal_mean": reward, "original_win_rate": 1585 / 2048,
                      "same_return_if_fail_at_floor_0": (reward + 1) / 2,
                      "same_return_if_fail_at_floor_1": (reward + 0.95) / 1.95,
                      "interpretation": "hypothetical indifference; not observed achievable headroom"},
        "lambda": {"half_life_at_098": math.log(.5) / math.log(.98), "direct_terminal_weight_at_170": .98 ** 170,
                   "interpretation": "direct residual weight only; critic bootstraps can propagate longer credit"},
        "paired_power_for_3pp_assuming_q_104_over_512": power,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key != "stock_projection_sha256"}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
