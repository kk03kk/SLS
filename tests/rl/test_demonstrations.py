from __future__ import annotations

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.rl.demonstrations import TEACHER_CORPUS_SCHEMA
from tools.generate_teacher_corpus import generate


def test_native_teacher_corpus_labels_policy_visible_checkpoints() -> None:
    corpus = generate(0, 2, 16)
    assert corpus["schema"] == TEACHER_CORPUS_SCHEMA
    assert corpus["teacher_successes"] == 1
    assert corpus["examples"]
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    for example in corpus["examples"]:
        decision = backend.load_checkpoint(example["checkpoint"])
        assert example["candidate_id"] in {
            action.candidate_id for action in decision.actions
        }


def test_native_teacher_obeys_prismatic_and_neow_skip_policy() -> None:
    for seed in (518, 568, 666):
        corpus = generate(seed, 1, 4)
        assert corpus["rejected_labels"] == 0
        assert corpus["rejections"] == []
