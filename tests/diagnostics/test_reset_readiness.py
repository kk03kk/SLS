"""A natural reset keeps history and counters without selecting teacher wins."""
import copy
import gzip
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from sls.diagnostics.cpu import digest
from sls.diagnostics.reset_readiness import check_prefix, episode_anchors, readiness


def state(identifier, trajectory, step, stratum="act2:COMBAT:SNECKO"):
    return {"id": identifier, "trajectory": trajectory, "step": step, "stratum": stratum}


def test_episode_anchors_do_not_overweight_long_trajectories():
    states = [state("a", "long", 200), state("b", "long", 180),
              state("c", "short", 60), state("d", "act1", 10, "act1:COMBAT:-")]
    selected = episode_anchors(states)
    assert selected == [states[1], states[2]]
    assert episode_anchors(list(reversed(states))) == selected
    # Neither successes nor teacher value estimates participate in selection.
    for item in states:
        item.update(success=True, value=100)
    assert episode_anchors(states) == selected


def test_reset_prefix_rejects_previous_terminal_and_public_tampering():
    rows = [{"terminal": False, "observation": {"run": {"act": 2}}, "actions": [{"kind": "END_TURN"}]}] * 3
    boundary = state("a", "episode", 2)
    boundary["public_sha256"] = digest({key: rows[2][key] for key in ("observation", "actions")})
    assert check_prefix(rows, boundary)["observation"]["run"]["act"] == 2
    tampered = copy.deepcopy(rows)
    tampered[0]["terminal"] = True
    with pytest.raises(ValueError, match="crosses termination"):
        check_prefix(tampered, boundary)
    tampered = copy.deepcopy(rows)
    tampered[2]["actions"] = []
    with pytest.raises(ValueError, match="digest mismatch"):
        check_prefix(tampered, boundary)
    for invalid in (-1, 3, True):
        with pytest.raises(ValueError, match="invalid reset boundary"):
            check_prefix(rows, dict(boundary, step=invalid))


def test_readiness_never_promotes_diagnostic_states_to_training(monkeypatch, tmp_path):
    import sls.diagnostics.reset_readiness as module

    corpus = tmp_path / "corpus"
    corpus.mkdir()
    (corpus / "manifest.json").write_text("{}", encoding="utf-8")
    manifest = {"states": [state("a", "teacher1", 10), state("b", "teacher2", 12)],
                "trajectories": [{"id": "teacher1", "seed": 100}, {"id": "teacher2", "seed": 100}],
                "native_source_sha256": "source"}
    monkeypatch.setattr(module, "validated_corpus", lambda _: manifest)
    monkeypatch.setattr(module, "qualify_reset", lambda *args: {"checked": args[2]["id"]})
    output = tmp_path / "report.json"
    report = readiness(corpus, output, {"identity": {}}, {"device": "cpu"}, 1)
    assert report["training_eligible"] is False
    assert report["available_episode_anchors"] == 2 and report["unique_act2_seeds"] == 1
    assert len(report["checks"]) == 1 and report["prefix_decisions_median"] == 11
    with pytest.raises(FileExistsError):
        readiness(corpus, output, {}, {})
    with pytest.raises(ValueError, match="bounds"):
        readiness(corpus, Path(tmp_path / "new.json"), {}, {}, 0)


@pytest.mark.parametrize("corrupt", [None, "native", "limits"])
def test_reset_preserves_full_private_state_counters_and_current_memory(monkeypatch, tmp_path, corrupt):
    from sls.contracts import Action, ActionKind, Decision, Observation
    from sls.contracts.observation import Player, RunContext, ScreenType
    from sls.diagnostics import reset_readiness as module
    from sls.diagnostics.cpu import decision_record
    from sls.rl.episode_limit import EpisodeLimitState

    decision = Decision(Observation(Player("IRONCLAD", 60, 80, 0, 3, 3),
        RunContext(20, 2, 20, 99, False, False, False), ScreenType.COMBAT),
        (Action(ActionKind.END_TURN),))
    public = decision_record(decision)
    rows = [dict(public, terminal=False, raw_reward=0.0, chosen_action=decision.actions[0].to_dict())] * 2
    boundary = dict(state("a", "teacher", 1), public_sha256=digest(public),
                    private_path="private.gz", private_sha256="unused")
    limits = EpisodeLimitState.initial(decision)
    limits.observe(decision, max_steps=4096, max_boundary_visits=4)
    private = {"native": {"rng": 123}, "limits": limits.to_dict()}
    if corrupt == "native":
        private["native"]["rng"] = 456  # Same visible observation, wrong hidden RNG.
    if corrupt == "limits":
        private["limits"]["steps"] = 0
    with gzip.open(tmp_path / "private.gz", "wt", encoding="utf-8") as stream:
        json.dump(private, stream)

    class Backend:
        def __init__(self, profile):
            pass

        def reset(self, seed):
            return decision

        def step(self, action):
            return SimpleNamespace(decision=decision, reward=0.0, terminated=False, truncated=False)

        def checkpoint(self):
            return {"rng": 123}

        def load_checkpoint(self, native):
            return decision

    calls = []

    def scoring(model, decision, memory, previous_action, previous_reward, start):
        calls.append((memory, previous_action, previous_reward, start))
        return SimpleNamespace(next_memory="current-model-prefix", value=[0.25])

    monkeypatch.setattr("sls.backends.simulator.SimulatorBackend", Backend)
    monkeypatch.setattr(module, "read_history", lambda _: (rows, {}))
    monkeypatch.setattr(module, "safe_path", lambda root, relative, _: root / relative)
    monkeypatch.setattr(module, "score", scoring)
    entry = {"model": SimpleNamespace(initial_memory=lambda *_: "current-model-initial"),
             "ppo": {"max_episode_steps": 4096, "max_boundary_visits": 4}}
    trajectory = {"id": "teacher", "seed": 10, "public_path": "public.gz", "sha256": "unused"}
    if corrupt:
        with pytest.raises(ValueError, match="private native state|episode counters"):
            module.qualify_reset(tmp_path, trajectory, boundary, entry)
    else:
        report = module.qualify_reset(tmp_path, trajectory, boundary, entry)
        assert report["preserved_episode_steps"] == 1 and not report["boundary_start_mask"]
        assert calls[0] == ("current-model-initial", 0, 0.0, True)
        assert calls[1] == ("current-model-prefix", module.ACTION_TYPE_IDS["END_TURN"] + 1, 0.0, False)
