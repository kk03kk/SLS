"""Read-only evidence collection for this audit; no policy evaluation or training.

Run from an installed SLS checkout with the Conda DL Python. Outputs are JSON;
checkpoints, logs, native sources and the game installation are never modified.
"""

from __future__ import annotations

import argparse
import dataclasses
import hashlib
import json
import math
import subprocess
from collections import Counter
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend, native
from sls.backends.simulator.environment import _combat_cards
from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Enemy,
    Observation,
    Player,
    PublicEntity,
    RunContext,
    ScreenType,
)
from sls.model import encode_decision
from sls.rl.training_contract import native_artifact, native_source_digest, sha256_file

ROOT = Path(__file__).resolve().parents[3]


def canonical(value):
    if dataclasses.is_dataclass(value):
        return canonical(dataclasses.asdict(value))
    if isinstance(value, dict):
        return {str(key): canonical(item) for key, item in value.items()}
    if isinstance(value, (tuple, list)):
        return [canonical(item) for item in value]
    return value


def probes():
    observation = Observation(
        Player("IRONCLAD", 70, 80, 0, 3, 3),
        RunContext(0, 1, 5, 99, False, False, False), ScreenType.COMBAT,
        enemies=(Enemy("MONSTER:0", "CULTIST", 40, 40, 0, "BUFF", 0, 0),),
    )
    encoded = []
    for owner in ("PLAYER_POWER:0", "MONSTER:0:POWER:0"):
        decision = Decision(dataclasses.replace(
            observation, powers=(PublicEntity(owner, "STRENGTH", (("amount", 3),)),),
        ), (Action(ActionKind.END_TURN),))
        encoded.append(encode_decision(decision))
    owner_collision = all(
        torch.equal(getattr(encoded[0], field.name), getattr(encoded[1], field.name))
        for field in dataclasses.fields(encoded[0])
    )
    battle = native.LightspeedBattle()
    battle.reset(123, "CULTIST", 0)
    battle.set_card_piles(["Bloodletting", "Armaments+", "Blood for Blood"], [], [], [])
    battle.step("play", card_index=1, target_index=0)
    before = battle.snapshot()["game_state"]["combat_state"]["hand"][-1]
    battle.step("play", card_index=1, target_index=0)
    after = battle.snapshot()["game_state"]["combat_state"]["hand"][-1]
    card = {"content_id": "RITUAL_DAGGER", "upgrades": 0, "base_cost": 1,
            "cost": 1, "is_playable": True, "special_data": 15}
    dynamic_collision = _combat_cards([card], "HAND") == _combat_cards(
        [{**card, "special_data": 45}], "HAND",
    )
    return {
        "power_owner_different_but_all_encoded_tensors_equal": owner_collision,
        "ritual_dagger_different_damage_but_equal_public_cards": dynamic_collision,
        "blood_for_blood": {"before": before, "after": after,
                           "stock_expected_upgraded_cost": 2},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path,
                        default=ROOT / "runs/9m/checkpoint-steps-000009007104.pt")
    parser.add_argument("--log", type=Path, default=ROOT / "runs/9m/sls-train-820787.out")
    parser.add_argument("--output", type=Path,
                        default=ROOT / "local/audits/repository-20260905/evidence.json")
    args = parser.parse_args()
    payload = torch.load(args.checkpoint, map_location="cpu", weights_only=False)
    state, contract = payload["trainer"], payload["contract"]
    rows, malformed = [], []
    for line_number, line in enumerate(args.log.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        try:
            record = json.loads(line)
        except json.JSONDecodeError:
            malformed.append(line_number)
            continue
        rows.append({"line": line_number, **record})
    updates = [row for row in rows if "loss" in row]
    evaluations = [row for row in rows if "evaluation" in row or "diagnostic_evaluation" in row]
    workers = []
    for index, checkpoint in enumerate(payload["environments"]):
        first, second = (SimulatorBackend(contract["profile"]) for _ in range(2))
        a, b = first.load_checkpoint(checkpoint), second.load_checkpoint(checkpoint)
        encode_decision(a)
        ta, tb = first.step(a.actions[0]), second.step(b.actions[0])
        workers.append({"worker": index, "act": a.observation.run.act,
                        "floor": a.observation.run.floor, "screen": a.observation.screen.value,
                        "initial_equal": a == b, "one_step_equal": ta == tb,
                        "one_step_checkpoint_equal": first.checkpoint() == second.checkpoint()})
    paths = subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines()
    inventory = []
    for relative in paths:
        path = ROOT / relative
        data = path.read_bytes()
        inventory.append({"path": relative, "bytes": len(data),
                          "lines": len(data.splitlines()),
                          "sha256": hashlib.sha256(data).hexdigest()})
    tensors = [value for item in payload["optimizer"]["state"].values()
               for value in item.values() if isinstance(value, torch.Tensor)]
    evaluations = [{key: value for key, value in row.items() if key in {
        "line", "environment_steps", "update", "evaluation", "diagnostic_evaluation",
        "diagnostic_seed_range", "baseline", "best_checkpoint_updated",
    }} for row in evaluations]
    result = {
        "schema": "sls-independent-repository-audit-20260905",
        "git_commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "working_tree_status": subprocess.check_output(
            ["git", "status", "--short"], cwd=ROOT, text=True,
        ).splitlines(),
        "inputs": {str(path): sha256_file(path) for path in (
            args.checkpoint, args.log, args.log.with_suffix(".err"),
            ROOT / "local/external/original-game/desktop-1.0.jar",
        )},
        "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
        "checkpoint_schema": payload["schema"], "contract": canonical(contract),
        "trainer": {key: state[key] for key in (
            "update", "environment_steps", "episodes", "next_seed", "termination_counts",
        )},
        "model_finite": all(bool(torch.isfinite(v).all()) for v in payload["model"].values()),
        "optimizer_finite": all(bool(torch.isfinite(v).all()) for v in tensors),
        "recurrent_memory_finite": bool(torch.isfinite(state["memory"]).all()),
        "workers": workers, "probes": probes(), "inventory": inventory,
        "inventory_groups": dict(Counter(row["path"].split("/")[0] for row in inventory)),
        "log": {
            "updates": len(updates), "first": updates[0], "last": updates[-1],
            "non_json_lines": malformed,
            "update_gaps": [[a["update"], b["update"]] for a, b in zip(updates, updates[1:])
                            if b["update"] != a["update"] + 1],
            "non_finite": [[row["line"], key] for row in updates for key, value in row.items()
                           if isinstance(value, float) and not math.isfinite(value)],
            "termination_totals": {key: sum(row[key] for row in updates)
                                   for key in updates[0] if key.startswith("terminations_")},
            "epochs_histogram": dict(Counter(row["epochs_completed"] for row in updates)),
            "kl_flagged_updates": sum(row["kl_early_stop"] for row in updates),
            "means": {key: sum(row[key] for row in updates) / len(updates) for key in (
                "entropy", "approx_kl_final", "decisions_per_second", "value_explained_variance",
            )},
            "checkpoint_rows": [row for row in updates if row["environment_steps"] == state["environment_steps"]],
            "evaluations": evaluations,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(args.output), "workers_checked": len(workers),
                      "updates": len(updates), "probes": result["probes"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
