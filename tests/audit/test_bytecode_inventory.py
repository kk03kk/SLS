from __future__ import annotations

import zipfile
from pathlib import Path

from sls.audit.bytecode_inventory import (
    _build_source_reference_index,
    _canonical_symbol,
    _class_names,
    _symbol_variants,
)


def test_class_names_only_returns_top_level_package_classes(tmp_path: Path) -> None:
    archive_path = tmp_path / "stock.jar"
    with zipfile.ZipFile(archive_path, "w") as archive:
        archive.writestr("com/megacrit/cardcrawl/cards/red/Anger.class", b"a")
        archive.writestr("com/megacrit/cardcrawl/cards/red/Anger$1.class", b"b")
        archive.writestr("com/megacrit/cardcrawl/relics/Akabeko.class", b"c")
    with zipfile.ZipFile(archive_path) as archive:
        assert _class_names(archive, "com/megacrit/cardcrawl/cards/") == [
            "com.megacrit.cardcrawl.cards.red.Anger",
        ]


def test_source_reference_index_scans_each_enum_reference(tmp_path: Path) -> None:
    source = tmp_path / "native" / "simulator" / "src" / "content.cpp"
    source.parent.mkdir(parents=True)
    source.write_text(
        "auto card = CardId::ANGER;\n"
        "auto pair = std::pair{RelicId::AKABEKO, Potion::BLOCK_POTION};\n",
        encoding="utf-8",
    )
    index = _build_source_reference_index(tmp_path)
    assert index[("CardId", "ANGER")] == [
        "native/simulator/src/content.cpp:1",
    ]
    assert index[("RelicId", "AKABEKO")] == [
        "native/simulator/src/content.cpp:2",
    ]
    assert index[("Potion", "BLOCK_POTION")] == [
        "native/simulator/src/content.cpp:2",
    ]


def test_stock_names_can_be_matched_to_cpp_symbols() -> None:
    assert _canonical_symbol("AcidSlime_L") == "ACIDSLIMEL"
    assert _symbol_variants("THE_CHAMP") == {"THECHAMP", "CHAMP"}
