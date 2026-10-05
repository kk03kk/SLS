"""Login commands must work even when importing Torch is forbidden."""

import os
import subprocess
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
BLOCK_TORCH = """
import importlib.abc
import sys
class NoTorch(importlib.abc.MetaPathFinder):
    def find_spec(self, fullname, path=None, target=None):
        if fullname == 'torch' or fullname.startswith('torch.'):
            raise RuntimeError('Torch forbidden on simulated login node')
sys.meta_path.insert(0, NoTorch())
"""


@pytest.mark.parametrize("module", ["tools.import_act12_parent", "tools.submit_act12_pilot"])
def test_login_cli_help_does_not_import_torch(module):
    command = BLOCK_TORCH + f"\nimport runpy\nsys.argv=['{module}', '--help']\nrunpy.run_module('{module}', run_name='__main__')"
    result = subprocess.run([sys.executable, "-c", command], cwd=ROOT,
                            env=os.environ | {"PYTHONPATH": str(ROOT / "src") + os.pathsep + str(ROOT)},
                            capture_output=True, text=True)
    assert result.returncode == 0, result.stderr


def test_hash_validation_remains_torch_free_and_rejects_changed_parent(tmp_path):
    # Synthetic bytes test the gate, never claim to be trained weights.
    command = BLOCK_TORCH + """
import json
from pathlib import Path
from tools import submit_act12_pilot as submit
from sls.rl.training_contract import sha256_file, source_sha256
root=Path(sys.argv[1])
(root/'parent').mkdir()
(root/'parent/final.pt').write_bytes(b'unit-fixture')
digest=sha256_file(root/'parent/final.pt')
(root/'parent/training-bundle.json').write_text(json.dumps({'files':{'final.pt':digest}}))
(root/'config.toml').write_text('[run]\\noutput="run"\\n')
(root/'recipe.json').write_text('{}')
submit.training_implementation_digest=lambda **kw:'a'*64
submit.local_source_digest=lambda *args,**kw:'b'*64
plan={'schema':'sls-act12-bound-plan-v1','status':'READY_FOR_LOCAL_VALIDATION',
      'config':'config.toml','config_sha256':source_sha256(root/'config.toml'),
      'target_training_implementation_sha256':'a'*64,
      'recipe_path':'recipe.json','recipe_sha256':source_sha256(root/'recipe.json'),'recipe':{},
      'parent':{'run':'parent','checkpoint':'parent/final.pt','sha256':digest,
                'target_native_source_sha256':'b'*64,'evidence':{'final.pt':digest}}}
assert submit.validate_plan(plan,root=root,deep=False)==root/'config.toml'
(root/'parent/final.pt').write_bytes(b'corrupt')
try:
    submit.validate_plan(plan,root=root,deep=False)
except ValueError as error:
    assert 'parent evidence changed' in str(error)
else:
    raise AssertionError('corrupt parent accepted')
"""
    result = subprocess.run([sys.executable, "-c", command, str(tmp_path)], cwd=ROOT,
                            env=os.environ | {"PYTHONPATH": str(ROOT / "src") + os.pathsep + str(ROOT)},
                            capture_output=True, text=True)
    assert result.returncode == 0, result.stderr


def test_submission_forwards_bound_plan_to_compute_node(tmp_path):
    from tools.submit_slurm import _parser, build_sbatch_command
    config = tmp_path / "config.toml"
    config.write_text("[run]\n")
    plan = tmp_path / "plan.json"
    command = build_sbatch_command(_parser().parse_args([
        "train", "--config", str(config), "--prepare", "--bound-plan", str(plan),
    ]), root=tmp_path)
    assert "--bound-plan" in command[-1]
    assert str(plan) in command[-1]
    with pytest.raises(ValueError, match="requires"):
        build_sbatch_command(_parser().parse_args(["train", "--bound-plan", str(plan)]), root=tmp_path)
