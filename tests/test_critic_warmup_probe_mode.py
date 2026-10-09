"""Pure mocked mode regression: no Torch, model, GPU or rollout execution."""
import ast
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace

import pytest

from tools.verify_act12_critic_warmup import fixed_actor_probe


class Scalar:
    def __init__(self, value):
        self.value = value

    def clone(self):
        return Scalar(self.value)


class ModeSensitivePolicy:
    """Model a mode-dependent inference path even without stochastic dropout."""
    def __init__(self):
        self.training = True
        self.grad_enabled = True
        self.actor_weight = 1.0
        self.memory_weight = 2.0
        self.value_weight = 0.0
        self.calls = []

    def eval(self):
        self.training = False
        return self

    @contextmanager
    def no_grad(self):
        before = self.grad_enabled
        self.grad_enabled = False
        try:
            yield
        finally:
            self.grad_enabled = before

    def __call__(self, token):
        assert token == "fixed input"
        self.calls.append((self.training, self.grad_enabled))
        rounding = 0.0000001 if self.training else 0.0
        return SimpleNamespace(logits=Scalar(self.actor_weight + rounding),
                               next_memory=Scalar(self.memory_weight + rounding))


def batch():
    return SimpleNamespace(model_inputs=lambda: ("fixed input",))


def test_original_mixed_mode_probe_reports_difference_with_unchanged_weights():
    model = ModeSensitivePolicy()
    with model.no_grad():
        reference = model(*batch().model_inputs())
    model.eval()  # PPO collector/warmup leaves the model in this mode.
    with model.no_grad():
        actual = model(*batch().model_inputs())
    assert reference.logits.value != actual.logits.value
    assert reference.next_memory.value != actual.next_memory.value
    assert model.actor_weight == 1.0 and model.memory_weight == 2.0


def test_both_probes_force_eval_no_grad_and_ignore_value_head_change():
    model = ModeSensitivePolicy()
    expected = fixed_actor_probe(model, batch(), model.no_grad)
    model.value_weight = 3.0
    model.training = True  # Do not depend on incidental trainer mode.
    actual = fixed_actor_probe(model, batch(), model.no_grad)
    assert [x.value for x in actual] == [x.value for x in expected]
    assert model.calls == [(False, False), (False, False)]
    assert model.grad_enabled  # no_grad context restored.


@pytest.mark.parametrize("attribute", ["actor_weight", "memory_weight"])
def test_matching_mode_does_not_hide_real_frozen_output_changes(attribute):
    model = ModeSensitivePolicy()
    expected = fixed_actor_probe(model, batch(), model.no_grad)
    setattr(model, attribute, getattr(model, attribute) + 0.00000001)
    actual = fixed_actor_probe(model, batch(), model.no_grad)
    assert [x.value for x in actual] != [x.value for x in expected]


def test_main_uses_shared_mode_probe_twice_and_retains_exact_tensor_equal():
    path = Path(__file__).resolve().parents[1] / "tools/verify_act12_critic_warmup.py"
    module = ast.parse(path.read_text())
    main = next(node for node in module.body if isinstance(node, ast.FunctionDef) and node.name == "main")
    probes = [node for node in ast.walk(main) if isinstance(node, ast.Call)
              and isinstance(node.func, ast.Name) and node.func.id == "fixed_actor_probe"]
    assert len(probes) == 2
    assert all([ast.unparse(arg) for arg in probe.args] == ["model", "batch", "torch.no_grad"]
               for probe in probes)
    source = ast.unparse(main)
    assert "actor_equal = torch.equal(actual_logits, expected_logits)" in source
    assert "gru_equal = torch.equal(actual_memory, expected_memory)" in source
    assert "if not actor_equal or not gru_equal:" in source
    assert "torch.allclose" not in source
    assert 'checks["actor_and_gru_frozen"]' in path.read_text()
