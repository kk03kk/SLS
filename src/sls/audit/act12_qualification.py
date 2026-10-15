"""Independent finite Act1–2 rule gate; no PPO or mixed-entry qualification.

The 72 obligations and eight historical coverage canaries are fixed before this
release. Reusing their raw captures never inherits an old replay's pass status.
"""

from __future__ import annotations

from collections import Counter

EXPECTED_CONTROLLED = set(range(131100000, 131100063))
EXPECTED_SYSTEMS = set(range(131100063, 131100072))
EXPECTED_NATURAL = {8000011000000, 8000011000001, 8000011000002, 8000011000004,
                    8000011000005, 8000011000006, 8000011000009, 8000011000069}
PROFILE = "IRONCLAD_A20_ACT2"
DISCOVERY = "DISCOVERY_FIXED_RETRIEVAL_UPDATES_14_V1"


def _index(rows, expected):
    result = {}
    for row in rows:
        seed = row["seed"]
        if seed in result:
            raise ValueError("duplicate rule evidence seed")
        result[seed] = row
    if set(result) != expected:
        raise ValueError("missing or unexpected rule obligation")
    return result


def assess(*, controlled, systems, conditional_systems, replacement, conditional_replacement,
           natural, conditional_natural, gremlin, boundaries, source, model_sha256):
    from tools.qualify_act12_foundation import timing_contract_row

    reports = [controlled, *systems, *conditional_systems, replacement, conditional_replacement, gremlin,
               *natural, *conditional_natural]
    if any(report.get("native_source_sha256") != source for report in reports):
        raise ValueError("stale or missing native source in rule evidence")
    if controlled.get("native_view") != "production-public" or controlled.get("continue_after_divergence"):
        raise ValueError("controlled evidence uses a diagnostic projection")
    combat = _index(controlled["runs"], EXPECTED_CONTROLLED)
    if any(row.get("status") != "HARNESS_MATCH" or row.get("first_divergence") is not None
           or row.get("terminal_comparison_scope") is not None for row in combat.values()):
        raise ValueError("controlled mechanism missing, divergent or ending-subset-only")
    plain = _index([row for report in systems for row in report["runs"]], EXPECTED_SYSTEMS)
    conditional = _index([row for report in conditional_systems for row in report["runs"]], EXPECTED_SYSTEMS)
    # This documented replacement completes the old incomplete Neow->Act2
    # prefix. Retain both reports, and never drop the obligation from the gate.
    old_prefix = plain[131100063]
    for target, report in ((plain, replacement), (conditional, conditional_replacement)):
        rows = _index(report["runs"], {131100063})
        target[131100063] = rows[131100063]
    for raw, conditioned in [*zip(systems, conditional_systems, strict=True),
                             (replacement, conditional_replacement)]:
        if not raw.get("stock_capture_sha256") or raw["stock_capture_sha256"] != conditioned.get("stock_capture_sha256"):
            raise ValueError("conditional system capture differs")
    explained = []
    unconditioned = 0
    for seed, row in plain.items():
        if (row.get("status") == "SYSTEM_BRANCH_MATCH" and row.get("first_divergence") is None
                and not row.get("controlled_runtime_inputs") and row.get("terminal_reason_verified")
                and row.get("stock_action_unavailable") is None
                and row.get("qualification_scope") == "UNCONDITIONAL_STOCK_SYSTEM_REPLAY"):
            unconditioned += 1
        elif timing_contract_row(row, conditional[seed], natural=False, source=source):
            explained.append({"seed": seed, "role": "system", "unconditional_match": False,
                              "full_clock_conditioned_replay": True})
        else:
            raise ValueError(f"unexplained or incomplete system obligation: {seed}")
    if (not any(plain[s].get("act2_boundaries", 0) for s in range(131100063, 131100066))
            or not any(plain[s].get("reward_boundaries", 0) for s in range(131100066, 131100069))
            or not all(plain[s].get("checkpoint_replays", 0) for s in range(131100069, 131100072))):
        raise ValueError("Act2 transition, rewards or continuation obligation not witnessed")
    natural_index = _index(natural, EXPECTED_NATURAL)
    conditional_index = _index(conditional_natural, {row["seed"] for row in conditional_natural})
    if set(conditional_index) - EXPECTED_NATURAL:
        raise ValueError("unexpected conditional natural seed")
    raw_matches = 0
    for seed, row in natural_index.items():
        env = row.get("evaluation_environment", {})
        if env.get("profile_id") != PROFILE or env.get("frozen_model_sha256") != model_sha256:
            raise ValueError("wrong natural profile or policy identity")
        if (row.get("status") == "TRAJECTORY_MATCH" and row.get("first_divergence") is None
                and row.get("terminal_reason_verified") and not row.get("clock_inputs")
                and "clock-conditioned" not in row.get("schema", "")):
            raw_matches += 1
        elif seed in conditional_index and timing_contract_row(
                row, conditional_index[seed], natural=True, source=source):
            explained.append({"seed": seed, "role": "natural", "unconditional_match": False,
                              "full_clock_conditioned_replay": True})
        else:
            raise ValueError(f"unexplained natural trajectory: {seed}")
    elite = _index(gremlin["runs"], {132300000, 132300001, 132300002})
    if any(row.get("status") != "HARNESS_MATCH" or row.get("first_divergence") is not None
           or row.get("terminal_comparison_scope") is not None for row in elite.values()):
        raise ValueError("third Act2 elite evidence incomplete")
    validate_boundaries(boundaries, source)
    return {"controlled_obligations": len(combat), "system_obligations": len(plain),
            "system_unconditional_matches": unconditioned, "natural_canaries": len(natural_index),
            "natural_unconditional_matches": raw_matches, "contract_explanations": explained,
            "superseded_prefix_retained": {"seed": 131100063, "status": old_prefix["status"],
                                            "counted_as_pass": False},
            "system_restore_actions": sum(r["checkpoint_replays"] for r in conditional.values()),
            "supplementary_elite_trajectories": len(elite)}


def validate_boundaries(report, source):
    profile = report.get("profile", {})
    if (report.get("native_source_sha256") != source or report.get("device") != "cpu"
            or profile.get("profile_id") != PROFILE or profile.get("ascension") != 20
            or profile.get("start_distribution") != "NATURAL_RUN_START"
            or report.get("training_updates") != 0 or report.get("decisions", 0) < 10000):
        raise ValueError("wrong profile, source or insufficient legal boundaries")
    counts, restores = report.get("screen_counts", {}), report.get("restore_screen_counts", {})
    for act in (1, 2):
        if (sum(value for name, value in counts.items() if name.startswith(f"ACT{act}:")) < 100
                or sum(value for name, value in restores.items() if name.startswith(f"ACT{act}:")) < 25):
            raise ValueError("insufficient per-act decision or restore evidence")
    if any(not restores.get(name) for name in counts if name.startswith("ACT")):
        raise ValueError("reachable screen lacks action continuation evidence")
    if (report.get("continuous_restores", 0) < 100 or not report.get("controlled_multi_selection")
            or not all(row.get("passed") and row.get("continuous_actions", 0) >= 4
                       for row in report["controlled_multi_selection"])):
        raise ValueError("continuous or pending multi-selection restoration incomplete")


def outcome_counts(report):
    return dict(Counter(row["role"] for row in report["contract_explanations"]))
