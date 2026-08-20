from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_legacy_architectures_are_absent() -> None:
    for relative in (
        "src/sls/architecture",
        "src/sls/environment",
        "src/sls/interaction",
        "src/sls/policy",
        "src/sls/verification",
        "scripts",
    ):
        assert not (ROOT / relative).exists(), relative


def test_canonical_assets_are_present() -> None:
    for relative in (
        "cpp/simulator/CMakeLists.txt",
        "cpp/simulator/python/module.cpp",
        "java/oracle-mod/ModTheSpire.json",
        "reference/original-game/manifest.json",
        "src/sls/contracts/decision.py",
    ):
        assert (ROOT / relative).is_file(), relative
