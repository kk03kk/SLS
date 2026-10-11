"""Real Act2 entry episodes, mixed with normal starts; never modify game resources."""
from __future__ import annotations

import gzip
import hashlib
import json
from pathlib import Path

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.training_contract import native_source_digest

SCHEMA = "sls-real-act2-entry-bank-v1"


class StartCurriculum:
    def __init__(self, path: Path, sha256: str, *, normal_fraction: float = 0.5) -> None:
        if normal_fraction != 0.5:
            raise ValueError("this curriculum version requires exactly half normal episodes")
        raw = path.read_bytes()
        if hashlib.sha256(raw).hexdigest() != sha256:
            raise ValueError("entry bank SHA256 mismatch")
        bank = json.loads(gzip.decompress(raw))
        if (bank.get("schema") != SCHEMA or bank.get("native_source_sha256") != native_source_digest()
                or bank.get("profile") != IRONCLAD_A20_ACT2.profile_id
                or bank.get("selection") != "ALL_REACHED_ENTRIES_NO_OUTCOME_FILTER"
                or bank.get("policy_memory") != "RESET_AT_NEW_EPISODE"
                or not bank.get("entries")):
            raise ValueError("entry bank source/profile/provenance contract mismatch")
        self.entries = bank["entries"]
        seeds = [row["seed"] for row in self.entries]
        if (len(seeds) != len(set(seeds))
                or any(type(seed) is not int or not 0 <= seed < 2_000_000_000_000 for seed in seeds)):
            raise ValueError("entry bank has duplicate or held-out source seeds")
        for row in self.entries:
            if not row.get("actions") or not isinstance(row.get("checkpoint"), dict):
                raise ValueError("entry bank lacks legal prefix/state")
        self.contract = {"schema": SCHEMA, "bank_sha256": sha256, "entries": len(self.entries),
                         "normal_fraction": normal_fraction, "policy_memory": bank["policy_memory"]}
        self.started = self.normal_started = self.entry_started = 0
        self.active_origins = {}
        self.outcomes = {"normal": {}, "entry": {}}

    def apply(self, workers, indices, seeds, decisions):
        if workers.profile != IRONCLAD_A20_ACT2:
            raise ValueError("entry curriculum requires the Act2 task")
        result = list(decisions)
        for position, (index, _seed) in enumerate(zip(indices, seeds, strict=True)):
            if self.started % 2:
                # Visit every entry: modulo the alternating reset seed could
                # permanently exclude half the bank when its size is even.
                row = self.entries[self.entry_started % len(self.entries)]
                decision = workers.load_one(index, row["checkpoint"])
                obs = decision.observation
                if decision.terminal or obs.run.act != 2 or obs.screen.value != "MAP":
                    raise ValueError("curriculum checkpoint is not a live Act2 map entry")
                result[position] = decision
                self.active_origins[str(index)] = {"kind": "entry", "source_seed": row["seed"]}
                self.entry_started += 1
            else:
                self.active_origins[str(index)] = {"kind": "normal", "source_seed": int(_seed)}
                self.normal_started += 1
            self.started += 1
        return result

    def state_dict(self):
        return {"started": self.started, "normal_started": self.normal_started,
                "entry_started": self.entry_started, "active_origins": dict(self.active_origins),
                "outcomes": {k: dict(v) for k, v in self.outcomes.items()}}

    def note_terminal(self, index, reason):
        kind = self.active_origins[str(index)]["kind"]
        counts = self.outcomes[kind]
        counts[reason] = counts.get(reason, 0) + 1

    def load_state_dict(self, state):
        if (set(state) != {"started", "normal_started", "entry_started", "active_origins", "outcomes"}
                or any(type(state[k]) is not int or state[k] < 0
                       for k in ("started", "normal_started", "entry_started"))
                or state["normal_started"] != (state["started"] + 1) // 2
                or state["entry_started"] != state["started"] // 2):
            raise ValueError("invalid curriculum continuation counters")
        if (set(state["outcomes"]) != {"normal", "entry"}
                or any(type(v) is not int or v < 0 for values in state["outcomes"].values() for v in values.values())
                or any(v.get("kind") not in {"normal", "entry"}
                       or type(v.get("source_seed")) is not int for v in state["active_origins"].values())):
            raise ValueError("invalid curriculum origin/outcome continuation")
        self.started = state["started"]
        self.normal_started = state["normal_started"]
        self.entry_started = state["entry_started"]
        self.active_origins = {k: dict(v) for k, v in state["active_origins"].items()}
        self.outcomes = {k: dict(v) for k, v in state["outcomes"].items()}


def from_config(config: dict, root: Path) -> StartCurriculum | None:
    spec = config.get("start_curriculum")
    if spec is None:
        return None
    if set(spec) != {"bank", "bank_sha256", "normal_fraction"}:
        raise ValueError("unknown or missing start curriculum configuration")
    path = (root / spec["bank"]).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("entry bank must be inside the project")
    return StartCurriculum(path, spec["bank_sha256"], normal_fraction=spec["normal_fraction"])
