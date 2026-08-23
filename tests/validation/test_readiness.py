from pathlib import Path

from sls.validation.readiness import BundleRecord, evaluate_route


def _boundary(sequence: int, act: int, *, status: str = "MATCH", boss: str = "SLIME_BOSS"):
    return {
        "sequence": sequence,
        "cursor": {
            "act": act,
            "floor": 16 if act == 1 else 17,
            "screen": "BOSS_REWARD" if act == 1 else "MAP",
            "room": "com.megacrit.cardcrawl.rooms.MonsterRoomBoss",
        },
        "canonical_public_state": {"run": {"boss": boss}},
        "selected_action": {"kind": "CHOOSE_BOSS_RELIC"},
        "comparison": {"status": status},
    }


def _records(
    *, child_hash: str = "anchor-hash", child_resume_hash: str = "resume-hash",
    child_class: str = "RESUMED_AUTOSAVE",
):
    root = BundleRecord(
        Path("root"),
        {
            "seed": 0,
            "profile_id": "IRONCLAD_A0_ACT1",
            "evidence_class": "LIVE_FULLRUN",
            "capture_mode": "PAIRED",
            "anchors": [{
                "anchor_id": "boss", "sequence": 1,
                "boundary_hash": "anchor-hash", "resume_boundary_hash": "resume-hash",
            }],
        },
        [_boundary(0, 1)],
    )
    child = BundleRecord(
        Path("child"),
        {
            "seed": 0,
            "profile_id": "IRONCLAD_A0_ACT1",
            "evidence_class": child_class,
            "capture_mode": "PAIRED",
            "provenance": {"source_run_id": "root", "source_anchor": "boss"},
            "start_state": {"boundary_hash": child_hash},
            "anchors": [{
                "anchor_id": "start", "sequence": 0,
                "boundary_hash": child_hash, "resume_boundary_hash": child_resume_hash,
                "capability": "RESUME_VERIFIED",
            }],
        },
        [
            _boundary(0, 1),
            _boundary(1, 2),
            _boundary(2, 2, status="DIFFERENCE"),
        ],
    )
    return {"root": root, "child": child}


def test_route_stops_at_first_act_two_boundary() -> None:
    result = evaluate_route(_records(), "child")
    assert result["valid"] is True
    assert result["reached_act_two"] is True
    assert result["used_boundaries"] == 3
    assert result["segments"] == [
        {"bundle": "root", "from_step": 0, "to_step": 0},
        {"bundle": "child", "from_step": 0, "to_step": 1},
    ]
    assert result["coverage"]["bosses"] == ["SLIME_BOSS"]


def test_route_rejects_tampered_provenance_boundary() -> None:
    result = evaluate_route(
        _records(child_hash="wrong", child_resume_hash="wrong-resume"), "child",
    )
    assert result["valid"] is False
    assert any(value.startswith("SOURCE_BOUNDARY_HASH_MISMATCH") for value in result["failures"])


def test_route_rejects_non_authoritative_evidence() -> None:
    result = evaluate_route(_records(child_class="ORIGINAL_SURVEY"), "child")
    assert result["valid"] is False
    assert any(value.startswith("INELIGIBLE_EVIDENCE") for value in result["failures"])
