"""Misspelled manifest IDs must fail before a protected game is launched."""
import pytest

from tools.run_act2_encounter_batch import verify_scene_encounters


def test_manifest_encounter_is_checked_instead_of_default_cli_encounters():
    manifest = {'scenes': [{'id': 'late', 'encounter': 'DARKLINGS'}]}
    with pytest.raises(ValueError, match='no game launched'):
        verify_scene_encounters(manifest, ['late'], 'THREE_DARKLINGS\t3 Darklings\n')
    manifest['scenes'][0]['encounter'] = 'THREE_DARKLINGS'
    verify_scene_encounters(manifest, ['late'], 'THREE_DARKLINGS\t3 Darklings\n')


def test_unselected_historical_scene_does_not_change_selected_allowlist_check():
    manifest = {'scenes': [{'id': 'old', 'encounter': 'UNKNOWN'},
                           {'id': 'live', 'encounter': 'REPTOMANCER'}]}
    verify_scene_encounters(manifest, ['live'], 'REPTOMANCER\tReptomancer\n')
