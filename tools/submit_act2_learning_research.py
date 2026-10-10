"""Torch-free login entry and fail-closed, archived single-allocation supervisor."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shlex
import signal
import subprocess
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
from sls.research.protocol import NATIVE_SHA256, PARENT_SHA256, scan_registrations


def sha(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1048576), b""):
            h.update(block)
    return h.hexdigest()


def exclusive_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2)


def package(output, status):
    """Inventory every completed/partial artifact; publish archive atomically."""
    files = [{"path": p.relative_to(output).as_posix(), "bytes": p.stat().st_size, "sha256": sha(p)}
             for p in sorted(output.rglob("*")) if p.is_file()]
    exclusive_json(output / "inventory.json", {"status": status, "files": files})
    archive = output.with_suffix(".tar.gz")
    if archive.exists():
        raise FileExistsError("archive already exists")
    temporary = archive.with_suffix(".tmp")
    with tarfile.open(temporary, "w:gz") as tar:
        tar.add(output, arcname=output.name)
    with temporary.open("r+b") as stream:
        os.fsync(stream.fileno())
    os.replace(temporary, archive)
    exclusive_json(archive.with_suffix(archive.suffix + ".sha256.json"), {"archive": archive.name, "sha256": sha(archive), "status": status})
    return archive


def supervise(commands, output, *, deadline, environment=None):
    """Signal the stage leader first so workers survive boundary checkpointing."""
    output.mkdir(parents=True, exist_ok=False)
    stopped, child = False, None
    def stop(signum, frame):
        nonlocal stopped
        stopped = True
        if child is not None and child.poll() is None:
            if os.name == "posix":
                os.kill(child.pid, signal.SIGTERM)
            else:
                child.terminate()
    previous = signal.signal(signal.SIGTERM, stop)
    status, stages = "failed", []
    try:
        for name, command in commands:
            began = time.perf_counter()
            if stopped or time.time() >= deadline:
                raise InterruptedError("supervisor time reserve reached")
            print(f"stage={name} command={shlex.join(command)}", flush=True)
            with (output / f"{name}.log").open("x", encoding="utf-8") as log:
                child = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                    env={**os.environ, **(environment or {}), "SLS_RESEARCH_DEADLINE": str(deadline)},
                    start_new_session=os.name == "posix")
                try:
                    returncode = child.wait(timeout=max(1, deadline - time.time()))
                except subprocess.TimeoutExpired:
                    stop(signal.SIGTERM, None)
                    try:
                        returncode = child.wait(timeout=300)
                    except subprocess.TimeoutExpired:
                        if os.name == "posix":
                            os.killpg(child.pid, signal.SIGKILL)
                        else:
                            child.kill()
                        child.wait()
                        raise RuntimeError("stage did not save/exit within shutdown grace period")
            stages.append({"stage": name, "returncode": returncode, "wall_seconds": time.perf_counter() - began})
            if returncode or stopped:
                if os.name == "posix":
                    try:
                        os.killpg(child.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
                raise RuntimeError(f"stage {name} failed/stopped (exit {returncode})")
        status = "completed"
    except BaseException as error:
        exclusive_json(output / "failure.json", {"error": repr(error), "stages": stages,
                      "training_restart": "never automatic; preserve prior qualified checkpoints"})
        raise
    finally:
        signal.signal(signal.SIGTERM, previous)
        exclusive_json(output / "stages.json", {"status": status, "stages": stages})
        archive = package(output, status)
        print(json.dumps({"archive": str(archive), "sha256": sha(archive), "status": status}), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--python", default="/home/h/hengzhi/venvs/sls/bin/python")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--execute-in-allocation", action="store_true")
    args = parser.parse_args()
    from sls.rl.training_contract import git_state, native_source_digest
    parent = args.source_root.resolve() / "local/runs/ironclad-a20-act1-win-90m-continuation/final.pt"
    if sha(parent) != PARENT_SHA256 or native_source_digest() != NATIVE_SHA256:
        raise ValueError("source environment or parent identity mismatch")
    if git_state()["dirty"]:
        raise ValueError("only clean committed research sources may be submitted")
    acceptance = ROOT / "docs/results/act2-learning-research/local-acceptance.json"
    if not acceptance.is_file() or not json.loads(acceptance.read_text())["qualified"]:
        raise ValueError("local research qualification has not been sealed")
    output = ROOT / "local/runs/act2-learning-research-r1"
    if output.exists() or output.with_suffix(".tar.gz").exists():
        raise FileExistsError("research execution namespace already exists")
    roots = []
    for checkout in args.source_root.resolve().parent.glob("SLS*"):
        for relative in ("configs", "local/operator", "local/runs", "local/reports"):
            path = checkout / relative
            if path.exists():
                roots.append(path)
    scanned = scan_registrations(roots, exempt=[ROOT / "configs/research/act2_learning_r1.json"])
    if "torch" in sys.modules:
        raise RuntimeError("login entry unexpectedly imported Torch")
    if args.execute_in_allocation:
        if not os.environ.get("SLURM_JOB_ID") or sys.platform != "linux":
            raise RuntimeError("execution requires a Linux Slurm GPU allocation")
        commands = [("build", [args.python, str(ROOT / "tools/build_native.py"), "--jobs", "16"])]
        commands.append(("signal-gate", [args.python, str(ROOT / "tools/verify_research_signals.py"), "--output", str(output)]))
        for stage in ("gpu-gate", "collect", "control", "curriculum", "evaluate"):
            commands.append((stage, [args.python, str(ROOT / "tools/run_act2_research.py"), stage,
                "--parent", str(parent), "--output", str(output), "--device", "cuda"]))
        # Two hours reserved for atomic boundary saves and a potentially large bank archive.
        supervise(commands, output, deadline=time.time() + 46 * 3600,
                  environment={"CUBLAS_WORKSPACE_CONFIG": ":4096:8", "PYTHONPATH": str(ROOT / "src") + os.pathsep + str(ROOT)})
        return
    command = ["sbatch", "--parsable", "--account=allusers", "--qos=normal", "--partition=gpu-long",
        "--constraint=xgpg", "--gres=gpu:a100-40:1", "--cpus-per-task=16", "--mem=64G", "--time=48:00:00",
        "--signal=B:TERM@300", "--export=ALL,CUBLAS_WORKSPACE_CONFIG=:4096:8", "--job-name=sls-act2-research",
        f"--chdir={ROOT}", f"--output={ROOT}/local/runs/slurm-logs/%x-%j.out", f"--error={ROOT}/local/runs/slurm-logs/%x-%j.err",
        "--wrap", "exec " + shlex.join([args.python, str(Path(__file__).resolve()), "--source-root", str(args.source_root.resolve()),
                                       "--python", args.python, "--execute-in-allocation"])]
    if args.dry_run:
        print(shlex.join(command))
        return
    receipt = ROOT / "local/operator/act2-learning-research-submission.json"
    exclusive_json(receipt, {"status": "SUBMITTING", "git": git_state(), "command": command,
                             "scanned_registrations": scanned, "parent_sha256": sha(parent)})
    (ROOT / "local/runs/slurm-logs").mkdir(parents=True, exist_ok=True)
    response = subprocess.check_output(command, cwd=ROOT, text=True).strip()
    job = response.split(";")[0]
    if not job.isdecimal():
        raise RuntimeError("ambiguous scheduler receipt; inspect instead of resubmitting")
    exclusive_json(receipt.with_name("act2-learning-research-submitted.json"), {"job": job, "git": git_state(), "training": True})
    print(json.dumps({"job": job, "training": True, "archive": str(output.with_suffix('.tar.gz'))}))


if __name__ == "__main__":
    main()
