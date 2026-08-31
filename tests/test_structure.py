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
        "native/simulator/CMakeLists.txt",
        "native/simulator/python/module.cpp",
        "src/sls/contracts/decision.py",
        "src/sls/content/scope.json",
        "tools/train_full_run.py",
        "tools/play_live.py",
        "configs/train/ironclad_a0_fullrun.toml",
        "docs/nus-training-zh.md",
    ):
        assert (ROOT / relative).is_file(), relative
