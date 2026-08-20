from sls.content import load_content_registry


def test_committed_registry_is_valid() -> None:
    registry = load_content_registry()
    assert registry.categories
    assert "cards" in registry.categories
