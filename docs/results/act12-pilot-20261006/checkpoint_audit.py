# ruff: noqa: E402
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[3]
sys.path[:0] = [str(root / "src"), str(root)]
import json

import torch

from sls.rl.training_contract import (
    native_source_digest,
    training_implementation_digest,
)

run = root / "local/runs/ironclad-a20-act12-win-pilot-r1"
manifest = json.loads((run / "run-manifest.json").read_text())
assert native_source_digest() == manifest["native_source_sha256"]
assert training_implementation_digest() == manifest["training_implementation_sha256"]


def finite(obj):
    if isinstance(obj, torch.Tensor):
        assert torch.isfinite(obj).all()
    elif isinstance(obj, dict):
        for v in obj.values():
            finite(v)
    elif isinstance(obj, (list, tuple)):
        for v in obj:
            finite(v)


data = {}
for name in ["final.pt", "latest.pt", "stages/train/selection/best_progress.pt"]:
    p = torch.load(run / name, map_location="cpu", weights_only=False)
    finite(p["model"])
    finite(p["optimizer"])
    finite(p["trainer"])
    assert p["contract"]["git_commit"] == manifest["git"]["commit"]
    assert len(p["environments"]) == 64
    assert 130000000 <= p["trainer"]["next_seed"] < 2000000000000
    data[name] = {
        "finite_model_optimizer_trainer": True,
        "environment_states": len(p["environments"]),
        "next_seed": p["trainer"]["next_seed"],
        "steps": p["trainer"]["environment_steps"],
    }
    if name == "final.pt":
        final = p
    if name == "latest.pt":
        latest = p
assert final["model"].keys() == latest["model"].keys()
assert all(torch.equal(v, latest["model"][k]) for k, v in final["model"].items())
out = {
    "current_native_and_implementation_match_server": True,
    "final_latest_tensor_equal": True,
    "checkpoints": data,
}
with (root / "local/reports/act12-checkpoints-recomputed.json").open("x") as f:
    json.dump(out, f, indent=2)
print(out)
