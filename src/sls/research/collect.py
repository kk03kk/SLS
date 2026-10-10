"""Frozen teacher collection; anchor selection never inspects future outcomes."""
from __future__ import annotations

import time
from pathlib import Path

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.diagnostics.cpu import decision_record, learning_reward, write_json
from sls.model.encoding import ACTION_TYPE_IDS
from sls.research.bank import BANK_SCHEMA, coverage, score, write_gzip
from sls.research.protocol import NATIVE_SHA256, PARENT_SHA256, RANGES, digest
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import git_state, native_source_digest, sha256_file


def anchor_stratum(decision, selected_room):
    observation = decision.observation
    if observation.run.floor == 17 and observation.screen.value in {"BOSS_REWARD", "ACT_TRANSITION"}:
        return "entry"
    if observation.run.act != 2:
        return None
    if observation.screen.value != "COMBAT":
        return "entry"
    enemies = {e.monster_id for e in observation.enemies}
    if enemies & {"THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"}:
        return "boss"
    if selected_room in {"ELITE", "BURNING_ELITE"}:
        return "elite"
    if selected_room == "MONSTER":
        return "ordinary"
    return None


def collect_bank(root, model, ppo, *, stop_requested=lambda: False):
    root = Path(root)
    if root.exists():
        raise FileExistsError("refuse to overwrite training corpus")
    if native_source_digest() != NATIVE_SHA256:
        raise ValueError("teacher environment identity mismatch")
    root.mkdir(parents=True)
    model.eval()
    anchors, trajectories = [], []
    begin = time.perf_counter()
    for seed in range(*RANGES["bank"]):
        if stop_requested():
            raise InterruptedError("bank collection stopped; partial bank is ineligible")
        backend = SimulatorBackend(IRONCLAD_A20_ACT2)
        decision = backend.reset(seed)
        limits = EpisodeLimitState.initial(decision)
        memory = model.initial_memory(1, next(model.parameters()).device)
        previous_action, previous_reward, room = 0, 0., None
        rows, seen = [], set()
        for step in range(ppo.max_episode_steps):
            if stop_requested():
                raise InterruptedError("bank collection interrupted")
            stratum = anchor_stratum(decision, room)
            if stratum and stratum not in seen:
                seen.add(stratum)
                key = digest({"seed": seed, "stratum": stratum, "step": step})
                public = {"prefix": list(rows), "boundary": decision_record(decision)}
                private = {"native": backend.checkpoint(), "limits": limits.to_dict()}
                public_path, private_path = f"public/{key}.json.gz", f"private/{key}.json.gz"
                write_gzip(root / public_path, public)
                write_gzip(root / private_path, private)
                anchors.append({"id": key, "seed": seed, "stratum": stratum, "step": step,
                    "boss": decision.observation.run.visible_boss_id if stratum == "boss" else None,
                    "boundary_sha256": digest(public["boundary"]),
                    "public_path": public_path, "public_sha256": sha256_file(root / public_path),
                    "private_path": private_path, "private_sha256": sha256_file(root / private_path)})
            output = score(model, decision, memory, previous_action, previous_reward, step == 0)
            action = decision.actions[int(output.logits.argmax(1)[0])]
            if action.node_id:
                node = next((n for n in decision.observation.map_nodes if n.node_id == action.node_id), None)
                room = node.visible_room_type if node else None
            transition = backend.step(action)
            reason = None if transition.terminated or transition.truncated else limits.observe(
                transition.decision, max_steps=ppo.max_episode_steps, max_boundary_visits=ppo.max_boundary_visits)
            terminal = transition.terminated or transition.truncated or reason is not None
            row = {**decision_record(decision), "chosen_action": action.to_dict(),
                   "raw_reward": float(transition.reward),
                   "shaped_reward": float(torch.tensor(learning_reward(decision, transition, ppo.to_dict(), limit=reason), dtype=torch.float32)),
                   "terminal": terminal}
            rows.append(row)
            memory = output.next_memory
            previous_action = ACTION_TYPE_IDS[action.kind.value] + 1
            previous_reward = float(torch.tensor(transition.reward, dtype=torch.float32))
            decision = transition.decision
            if transition.truncated:
                raise RuntimeError("backend fault invalidates teacher collection")
            if terminal:
                break
        path = root / f"trajectories/{seed}.json.gz"
        write_gzip(path, {"rows": rows, "success": bool(transition.info.get("success")),
                          "reason": reason or transition.info.get("reason"), "steps": len(rows)})
        trajectories.append({"seed": seed, "path": str(path.relative_to(root)), "sha256": sha256_file(path), "steps": len(rows)})
        print(f"teacher seed={seed} steps={len(rows)} anchors={sorted(seen)}", flush=True)
    manifest = {"schema": BANK_SCHEMA, "role": "training-only", "git": git_state(),
                "native_sha256": NATIVE_SHA256, "teacher_sha256": PARENT_SHA256,
                "seed_range": list(RANGES["bank"]), "anchors": anchors,
                "coverage": coverage(anchors), "trajectories": trajectories,
                "teacher_decisions": sum(t["steps"] for t in trajectories),
                "teacher_seconds": time.perf_counter() - begin,
                "selection": "earliest boundary per seed per stratum; no future labels or values"}
    write_json(root / "manifest.json", manifest)
    if not manifest["coverage"]["qualified"]:
        raise ValueError("natural-bank coverage failed; budget will not be enlarged")
    return manifest
