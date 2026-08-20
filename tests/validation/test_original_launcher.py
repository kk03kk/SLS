from argparse import Namespace
from pathlib import Path

from tools.run_original import _entry, launcher_command, pin_display_fps


def test_launcher_is_headless_strict_and_skips_intro() -> None:
    game = Path(r"D:\Steam\steamapps\common\SlayTheSpire")
    mts = Path(r"D:\Steam\steamapps\workshop\content\646570\1605060445\ModTheSpire.jar")
    command = launcher_command(game, mts, skip_intro=True)
    assert command[0].endswith("javaw.exe")
    assert "--skip-launcher" in command
    assert "--skip-intro" in command
    assert command[-1] == "basemod,CommunicationMod,spirecomm-parity"
    assert "SuperFastMode" not in " ".join(command)


def test_communication_mod_entry_uses_properties_safe_paths() -> None:
    args = Namespace(
        bundle=Path(r"D:\SLS\validation-results\truth\bundle"),
        anchor="a0001", to_step=3, continue_steps=2,
        game_root=Path(r"D:\Steam\steamapps\common\SlayTheSpire"),
    )
    entry, values = _entry("resume", args)
    path_values = [entry.as_posix(), values[0], values[values.index("--game-root") + 1]]
    assert all("\\" not in value for value in path_values)
    assert all(":" in value for value in path_values)


def test_intro_flag_can_be_disabled_for_equivalence_control() -> None:
    command = launcher_command(Path(r"D:\game"), Path(r"D:\mts.jar"), skip_intro=False)
    assert "--skip-launcher" in command
    assert "--skip-intro" not in command


def test_authoritative_fps_is_pinned_without_changing_other_display_settings(
    tmp_path: Path,
) -> None:
    config = tmp_path / "info.displayconfig"
    config.write_text("1600\n900\n144\nfalse\nfalse\ntrue\n", encoding="utf-8")
    pin_display_fps(config)
    assert config.read_text(encoding="utf-8").splitlines() == [
        "1600", "900", "60", "false", "false", "true",
    ]
