"""Record bounded Act2 native rule scenarios against an identified stock JAR.

This is not stock-game execution, a policy evaluation, or complete parity proof.
Independent stock expectations are tested in test_a20_late_act_parity.py.
"""

import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

from sls.backends.simulator import native
from sls.rl.training_contract import native_source_digest

STOCK_SHA = "cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673"


def capture(battle):
    snapshot = battle.snapshot()
    combat = snapshot["game_state"]["combat_state"]
    return {"player": combat["player"], "monsters": combat["monsters"], "rng": snapshot["_rng"]}


def battle(encounter, ascension=20, *, move=None, artifact=False, monster_hp=None):
    result = native.LightspeedBattle()
    result.reset(0, encounter, ascension, replace_relics=True,
                 relics=["CLOCKWORK_SOUVENIR"] if artifact else [])
    result.set_player_health(5000, 5000)
    snapshot = result.snapshot()
    for monster in snapshot["game_state"]["combat_state"]["monsters"]:
        if move and monster["monster_id"] == move[0]:
            monster["move_id"] = move[1]
        if monster_hp is not None and monster["monster_id"] == "THE_CHAMP":
            monster["current_hp"] = monster_hp
    result.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", required=True, type=Path)
    parser.add_argument("--javap", required=True, type=Path)
    parser.add_argument("--disassembly-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("refusing to overwrite rule evidence")
    digest = hashlib.sha256(args.stock_jar.read_bytes()).hexdigest()
    if digest != STOCK_SHA:
        raise ValueError("stock JAR differs from independently reviewed rule reference")
    args.disassembly_dir.mkdir(parents=True, exist_ok=True)
    classes = {}
    with zipfile.ZipFile(args.stock_jar) as archive:
        for name in ("Champ", "TheCollector", "BronzeAutomaton", "SphericGuardian"):
            qualified = "com.megacrit.cardcrawl.monsters.city." + name
            command = [str(args.javap), "-classpath", str(args.stock_jar), "-c", "-p", qualified]
            data = subprocess.check_output(command)
            target = args.disassembly_dir / (name + ".txt")
            if target.exists():
                raise ValueError("refusing to overwrite stock disassembly")
            target.write_bytes(data)
            classes[name] = {
                "class_sha256": hashlib.sha256(archive.read(qualified.replace(".", "/") + ".class")).hexdigest(),
                "disassembly_sha256": hashlib.sha256(data).hexdigest(), "command": command,
            }
    cases = {}
    for ascension in (18, 19, 20):
        current = battle("AUTOMATON", ascension)
        trace = []
        for _ in range(13):
            trace.append(capture(current))
            current.step("end_turn")
        cases[f"automaton-two-cycles-a{ascension}"] = trace
    for ascension in (16, 17, 20):
        current = battle("SPHERIC_GUARDIAN", ascension)
        trace = []
        for _ in range(5):
            trace.append(capture(current))
            current.step("end_turn")
        cases[f"spheric-opening-a{ascension}"] = trace
    for hp in (220, 219):
        current = battle("CHAMP", move=("THE_CHAMP", "THE_CHAMP_GLOAT"), monster_hp=hp)
        trace = [capture(current)]
        for _ in range(2):
            current.step("end_turn")
            trace.append(capture(current))
        cases[f"champ-threshold-hp{hp}"] = trace
    for encounter, monster, move in (("CHAMP", "THE_CHAMP", "FACE_SLAP"),
                                     ("CHAMP", "THE_CHAMP", "TAUNT"),
                                     ("COLLECTOR", "THE_COLLECTOR", "MEGA_DEBUFF")):
        current = battle(encounter, move=(monster, monster + "_" + move), artifact=True)
        before = capture(current)
        current.step("end_turn")
        cases[f"artifact-{monster}-{move}"] = [before, capture(current)]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result = {"schema": "sls-act2-bounded-rule-scenarios-v1", "stock_jar_sha256": digest,
              "native_source_sha256": native_source_digest(), "stock_classes": classes,
              "controlled_state": "seed 0, high player HP, no relics except explicit Artifact cases",
              "cases": cases,
              "limitations": "Bytecode review plus native scenarios; no stock-game execution, policy win rate or full Act2 qualification."}
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Recorded {len(cases)} bounded rule scenarios to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
