"""Replay every recorded decision and summarize diagnostic behavior, not policy rules."""

from __future__ import annotations

import argparse
import gzip
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A0_ACT1


def load_rows(path):
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        return [json.loads(line) for line in stream]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("corpus", type=Path)
    args = parser.parse_args()
    counters = Counter()
    groups = defaultdict(list)
    examples = defaultdict(list)
    episodes = []
    for path in sorted(args.corpus.glob("seed-*.jsonl.gz")):
        rows = load_rows(path)
        seed = rows[0]["seed"]
        backend = SimulatorBackend(IRONCLAD_A0_ACT1)
        decision = backend.reset(seed)
        assert decision.observation.to_dict() == rows[0]["observation"]
        previous = rows[0]["observation"]
        stats = Counter()
        entry = None
        for index, row in enumerate(rows[1:]):
            action = Action.from_dict(row["action"])
            assert [a.to_dict() for a in decision.actions] == row["legal_actions"]
            assert backend._candidate_bits[action.candidate_id] == row["native_bits"]
            snapshot = args.corpus / f"replay-{seed}-{index}.json.gz"
            if snapshot.exists():
                with gzip.open(snapshot, "rt", encoding="utf-8") as stream:
                    assert json.loads(json.dumps(backend.checkpoint())) == json.load(
                        stream
                    ), (seed, index, "native checkpoint/RNG")
                counters["verified_native_snapshots"] += 1
            before = previous
            after = row["observation"]
            kind = action.kind.value
            counters["action:" + kind] += 1
            stats[kind] += 1
            if (
                before["screen"] == "COMBAT"
                and before["run"]["floor"] == 16
                and entry is None
            ):
                entry = {
                    "hp": before["player"]["current_hp"],
                    "max_hp": before["player"]["max_hp"],
                    "deck_size": len(before["deck"]),
                    "potions": len(before["potions"]),
                    "deck": [c["card_id"] for c in before["deck"]],
                }
            hand = {c["instance_id"]: c for c in before["hand"]}
            card = hand.get(action.subject_id)
            powers = {
                p["content_id"]: p["properties"].get("amount", 0)
                for p in before["powers"]
            }
            if (
                kind == "REST"
                and before["player"]["current_hp"] == before["player"]["max_hp"]
            ):
                counters["full_hp_rest"] += 1
            if kind == "END_TURN" and before["player"]["energy"] > 0:
                counters["end_turn_with_energy"] += 1
                if any(a["kind"] == "PLAY_CARD" for a in row["legal_actions"]):
                    counters["end_turn_with_playable_card"] += 1
            if before["screen"] == "COMBAT" and before["run"]["floor"] == 16:
                boss = before["run"]["visible_boss_id"]
                stats["boss_actions"] += 1
                if card:
                    counters["boss_card:" + boss + ":" + card["card_id"]] += 1
                if "SHARP_HIDE" in powers and card and kind == "PLAY_CARD":
                    hp_loss = (
                        before["player"]["current_hp"] - after["player"]["current_hp"]
                    )
                    if hp_loss > 0:
                        stats["hp_lost_on_sharp_hide_card_actions"] += hp_loss
                        stats["sharp_hide_hp_loss_actions"] += 1
                        if len(examples["sharp_hide"]) < 12:
                            examples["sharp_hide"].append(
                                {
                                    "seed": seed,
                                    "step": index,
                                    "card": card["card_id"],
                                    "hp_loss": hp_loss,
                                    "block": before["player"]["block"],
                                    "energy": before["player"]["energy"],
                                }
                            )
                    # Prove a narrowly scoped ordering opportunity with native replay.
                    if (
                        card["card_id"] == "STRIKE_RED"
                        and index + 2 < len(rows)
                        and len(examples["ordering_counterfactual"]) < 12
                    ):
                        following = rows[index + 2]
                        next_card = next(
                            (
                                c
                                for c in after["hand"]
                                if c["instance_id"] == following["action"]["subject_id"]
                            ),
                            None,
                        )
                        if (
                            next_card
                            and next_card["card_id"] == "DEFEND_RED"
                            and following["action"]["kind"] == "PLAY_CARD"
                        ):
                            clone = SimulatorBackend(IRONCLAD_A0_ACT1)
                            alt = clone.load_checkpoint(backend.checkpoint())

                            def by_card(d, name):
                                ids = {
                                    c.instance_id
                                    for c in d.observation.hand
                                    if c.card_id == name
                                }
                                return next(
                                    (
                                        a
                                        for a in d.actions
                                        if a.kind.value == "PLAY_CARD"
                                        and a.subject_id in ids
                                    ),
                                    None,
                                )

                            defend = by_card(alt, "DEFEND_RED")
                            if defend:
                                alt = clone.step(defend).decision
                                strike = by_card(alt, "STRIKE_RED")
                                if strike:
                                    alt = clone.step(strike).decision
                                    end = following["observation"]
                                    ao = alt.observation.to_dict()
                                    if (
                                        ao["player"]["current_hp"]
                                        > end["player"]["current_hp"]
                                        and ao["enemies"] == end["enemies"]
                                    ):
                                        examples["ordering_counterfactual"].append(
                                            {
                                                "seed": seed,
                                                "step": index,
                                                "actual_hp": end["player"][
                                                    "current_hp"
                                                ],
                                                "defend_first_hp": ao["player"][
                                                    "current_hp"
                                                ],
                                                "actual_block": end["player"]["block"],
                                                "defend_first_block": ao["player"][
                                                    "block"
                                                ],
                                                "actual_energy": end["player"][
                                                    "energy"
                                                ],
                                                "defend_first_energy": ao["player"][
                                                    "energy"
                                                ],
                                            }
                                        )
            transition = backend.step(action)
            assert transition.decision.observation.to_dict() == after, (
                seed,
                index,
                "observation",
            )
            assert transition.reward == row["reward"]
            assert dict(transition.info) == row["info"]
            assert (
                transition.terminated == row["terminated"]
                and transition.truncated == row["truncated"]
            )
            # Public draw piles must not encode hidden shuffled positions.
            assert all(c["visible_order"] is None for c in after["draw_pile"])
            assert not any(
                r["content_id"] == "PRISMATIC_SHARD" for r in after["relics"]
            )
            assert len({a.candidate_id for a in transition.decision.actions}) == len(
                transition.decision.actions
            )
            counters["automatic_note_leave"] += (
                row["info"]
                .get("automatic_actions", [])
                .count("NOTE_FOR_YOURSELF:LEAVE")
            )
            counters["replayed_steps"] += 1
            previous = after
            decision = transition.decision
        final = rows[-1]
        won = bool(final["info"].get("success"))
        boss = previous["run"]["visible_boss_id"]
        if entry:
            groups[boss + (":win" if won else ":loss")].append(entry)
        episodes.append(
            {
                "seed": seed,
                "boss": boss,
                "won": won,
                "floor": previous["run"]["floor"],
                "entry": entry,
                "stats": dict(stats),
            }
        )
        print(
            json.dumps(
                {"replayed_seeds": len(episodes), "steps": counters["replayed_steps"]}
            ),
            flush=True,
        )
    summary = {
        key: {
            "n": len(values),
            **{
                field: sum(v[field] for v in values) / len(values)
                for field in ["hp", "max_hp", "deck_size", "potions"]
            },
        }
        for key, values in groups.items()
    }
    output = {
        "counters": dict(counters),
        "boss_entries": summary,
        "examples": dict(examples),
        "episodes": episodes,
        "qualification": "Stratified diagnostic cohort, not an unbiased population estimate; action-order examples are local counterfactuals, not proven whole-run improvements.",
    }
    (args.corpus / "analysis.json").write_text(
        json.dumps(output, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
