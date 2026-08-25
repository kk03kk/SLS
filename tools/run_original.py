"""Own one recoverable Original-game launch from preparation through restore."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.runtime import RuntimeJournal, prepare_runtime, process_identity, recover_pending


AUTHORITATIVE_FPS = 60


def pin_display_fps(path: Path, fps: int = AUTHORITATIVE_FPS) -> None:
    """Pin Original's frame clock after the journal has protected the file."""

    lines = path.read_text(encoding="utf-8").splitlines()
    if len(lines) < 6:
        raise ValueError(f"invalid Original display config: {path}")
    int(lines[0])
    int(lines[1])
    int(lines[2])
    lines[2] = str(fps)
    path.write_text("\n".join(lines[:6]) + "\n", encoding="utf-8")


def launcher_command(game_root: Path, mod_the_spire: Path, *, skip_intro: bool) -> list[str]:
    command = [
        str(game_root / "jre" / "bin" / "javaw.exe"),
        "-Xmx2G", "-Dfile.encoding=UTF-8", "-jar", str(mod_the_spire),
        "--skip-launcher",
    ]
    if skip_intro:
        command.append("--skip-intro")
    command.extend(["--mods", "basemod,CommunicationMod,spirecomm-parity"])
    return command


def _entry(mode: str, args: argparse.Namespace) -> tuple[Path, list[str]]:
    if mode == "card-audit":
        return ROOT / "tools" / "audit_card_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.audit_output.resolve().as_posix(),
        ]
    if mode == "potion-audit":
        return ROOT / "tools" / "audit_potion_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.potion_audit_output.resolve().as_posix(),
        ]
    if mode == "relic-audit":
        return ROOT / "tools" / "audit_relic_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.relic_audit_output.resolve().as_posix(),
        ]
    if mode == "relic-spawn-audit":
        return ROOT / "tools" / "audit_relic_spawn_semantics.py", [
            "--seed", str(args.seed),
            "--artifact", args.relic_audit_output.resolve().as_posix(),
        ]
    if mode == "mechanism-audit":
        return ROOT / "tools" / "audit_mechanism_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.mechanism_audit_output.resolve().as_posix(),
        ]
    if mode == "encounter-audit":
        return ROOT / "tools" / "audit_encounter_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.encounter_audit_output.resolve().as_posix(),
        ]
    if mode == "event-audit":
        return ROOT / "tools" / "audit_event_semantics.py", [
            "--seed", str(args.seed),
            "--output", args.event_audit_output.resolve().as_posix(),
        ]
    if mode == "capture":
        values = [
            "--seed", str(args.seed), "--profile", args.profile,
            "--max-steps", str(args.max_steps),
            "--variant", str(getattr(args, "variant", 0)),
            "--truth-root", (ROOT / "validation-results" / "truth").resolve().as_posix(),
            "--output", (ROOT / "validation-results" / "full-run.json").resolve().as_posix(),
        ]
        if args.require_clean:
            values.append("--require-clean")
        return ROOT / "tools" / "validate_full_run.py", values
    if mode == "survey":
        return ROOT / "tools" / "capture_original_survey.py", [
            "--seed", str(args.seed), "--profile", args.profile,
            "--max-steps", str(args.max_steps), "--max-act", str(args.max_act),
            "--variant", str(args.variant),
        ]
    if args.bundle is None or not args.anchor or args.to_step is None:
        raise ValueError("resume requires --bundle, --anchor and --to-step")
    values = [
        args.bundle.resolve().as_posix(), "--anchor", args.anchor,
        "--to-step", str(args.to_step), "--game-root", args.game_root.resolve().as_posix(),
        "--continue-steps", str(args.continue_steps),
    ]
    action_plan = getattr(args, "action_plan", None)
    if action_plan is not None:
        values = values[:-2]
        values.extend(["--action-plan", action_plan.resolve().as_posix()])
        values.extend(["--action-plan-offset", str(getattr(args, "action_plan_offset", 0))])
    return ROOT / "tools" / "replay_original_segment.py", values


def main() -> int:
    local = Path.home() / "AppData" / "Local" / "ModTheSpire"
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("capture", "resume", "survey", "card-audit", "potion-audit", "relic-audit", "relic-spawn-audit", "mechanism-audit", "encounter-audit", "event-audit"))
    parser.add_argument("--game-root", type=Path, default=Path(r"D:\Steam\steamapps\common\SlayTheSpire"))
    parser.add_argument("--python", type=Path, default=Path(r"D:\Anaconda\envs\DL\python.exe"))
    parser.add_argument("--config", type=Path, default=local / "CommunicationMod" / "config.properties")
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--profile", default="IRONCLAD_A0_HEART")
    parser.add_argument("--max-steps", type=int, default=10)
    parser.add_argument("--max-act", type=int, default=1)
    parser.add_argument("--variant", type=int, default=0)
    parser.add_argument("--bundle", type=Path)
    parser.add_argument("--anchor")
    parser.add_argument("--to-step", type=int)
    parser.add_argument("--continue-steps", type=int, default=0)
    parser.add_argument("--action-plan", type=Path)
    parser.add_argument("--action-plan-offset", type=int, default=0)
    parser.add_argument("--require-clean", action="store_true")
    parser.add_argument("--timeout", type=int, default=300)
    parser.add_argument(
        "--audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_card_semantics.json",
    )
    parser.add_argument(
        "--potion-audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_potion_semantics.json",
    )
    parser.add_argument(
        "--relic-audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_relic_semantics.json",
    )
    parser.add_argument(
        "--mechanism-audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_mechanism_semantics.json",
    )
    parser.add_argument(
        "--encounter-audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_encounter_semantics.json",
    )
    parser.add_argument(
        "--event-audit-output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_event_semantics.json",
    )
    parser.add_argument("--skip-intro", action=argparse.BooleanOptionalAction, default=True)
    args = parser.parse_args()

    journals = ROOT / "validation-results" / "runtime-journals"
    recover_pending(journals)
    entry, entry_args = _entry(args.mode, args)
    save = args.game_root / "saves" / "IRONCLAD.autosave"
    display_config = args.game_root / "info.displayconfig"
    journal_path = prepare_runtime(
        repository=ROOT, game_root=args.game_root, config=args.config,
        python=args.python, max_steps=args.max_steps, profile=args.profile,
        save_files=(save, Path(str(save) + ".backUp"), display_config), entry=entry,
        entry_args=entry_args, external_owner=True,
    )
    journal = RuntimeJournal.open(journal_path)
    completion = journal_path.parent / "completion.json"
    log_root = ROOT / "validation-results" / "launcher"
    log_root.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    stdout_path, stderr_path = log_root / f"{stamp}.stdout.log", log_root / f"{stamp}.stderr.log"
    javaw = args.game_root / "jre" / "bin" / "javaw.exe"
    mts = args.game_root.parents[1] / "workshop" / "content" / "646570" / "1605060445" / "ModTheSpire.jar"
    if not javaw.is_file() or not mts.is_file():
        journal.recover()
        raise FileNotFoundError(f"missing launcher prerequisite: {javaw if not javaw.is_file() else mts}")
    command = launcher_command(args.game_root, mts, skip_intro=args.skip_intro)
    environment = os.environ.copy()
    environment["SLS_RUN_COMPLETION"] = str(completion)
    environment["SLS_GAME_ROOT"] = str(args.game_root.resolve())
    environment["SLS_SKIP_INTRO"] = "1" if args.skip_intro else "0"
    environment["SLS_ORIGINAL_FPS"] = str(AUTHORITATIVE_FPS)
    process: subprocess.Popen[bytes] | None = None
    marker: dict[str, object] | None = None
    try:
        pin_display_fps(display_config)
        with stdout_path.open("wb") as stdout, stderr_path.open("wb") as stderr:
            process = subprocess.Popen(
                command, cwd=args.game_root, env=environment, stdout=stdout, stderr=stderr,
            )
            identity = None
            for _ in range(20):
                identity = process_identity(process.pid)
                if identity is not None:
                    break
                time.sleep(0.05)
            journal.record_process(
                pid=process.pid, executable=javaw, command=command, identity=identity,
            )
            deadline = time.monotonic() + args.timeout
            while time.monotonic() < deadline:
                if completion.is_file():
                    marker = json.loads(completion.read_text(encoding="utf-8"))
                    break
                if process.poll() is not None:
                    raise RuntimeError(f"Original exited before validator completion: {process.returncode}")
                time.sleep(0.25)
            if marker is None:
                raise TimeoutError(f"Original validation timed out after {args.timeout}s")
    finally:
        if process is not None and process.poll() is None:
            if marker is not None:
                journal.restore_under(
                    args.game_root / name
                    for name in ("preferences", "betaPreferences", "saves")
                )
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=15)
        journal.recover()
    result = {
        "mode": args.mode, "completion": marker, "journal": str(journal_path),
        "stdout_log": str(stdout_path), "stderr_log": str(stderr_path),
        "launcher": {"executable": str(javaw), "arguments": command[1:]},
    }
    print(json.dumps(result, ensure_ascii=False, indent=2), file=sys.stderr)
    return int((marker or {}).get("exit_code", 2))


if __name__ == "__main__":
    raise SystemExit(main())
