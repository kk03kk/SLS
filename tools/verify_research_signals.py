"""Allocated Linux signal qualification, before teacher collection or training."""
from __future__ import annotations

import argparse
import json
import os
import signal
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from tools.submit_act2_learning_research import supervise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if os.name != "posix":
        raise RuntimeError("real process-group signal qualification requires Linux")
    scratch = args.output / "signal-check"
    ready = scratch / "ready"
    checkpoint = scratch / "saved-checkpoint.json"
    code = ("import signal,time,os; from pathlib import Path; stopped=False\n"
            "def stop(*args):\n global stopped\n stopped=True\n"
            "signal.signal(signal.SIGTERM,stop)\n"
            f"Path({str(ready)!r}).write_text('ready')\n"
            "while not stopped: time.sleep(.01)\n"
            f"p=Path({str(checkpoint)!r}); t=p.with_suffix('.tmp'); t.write_text('{{\"saved\": true}}')\n"
            "with t.open('r+b') as f: os.fsync(f.fileno())\n"
            "os.replace(t,p)\n")
    def interrupt():
        for _ in range(1000):
            if ready.exists():
                os.kill(os.getpid(), signal.SIGTERM)
                return
            time.sleep(.01)
    thread = threading.Thread(target=interrupt, daemon=True)
    thread.start()
    try:
        supervise([("boundary-save", [sys.executable, "-c", code])], scratch, deadline=time.time() + 20)
    except RuntimeError as error:
        if "failed/stopped" not in str(error):
            raise
    thread.join(timeout=1)
    if not checkpoint.exists() or json.loads(checkpoint.read_text()) != {"saved": True}:
        raise RuntimeError("stage did not atomically save after forwarded SIGTERM")
    if not scratch.with_suffix(".tar.gz").exists():
        raise RuntimeError("signal failure archive was not produced")
    (args.output / "signal-gate.json").write_text(json.dumps({"passed": True,
        "signal_forwarding": "leader receives SIGTERM; boundary save completes before worker-group cleanup",
        "atomic_save": True, "failure_archive": True}))


if __name__ == "__main__":
    main()
