from dataclasses import dataclass, make_dataclass

import pytest

from sls.contracts import observation


def test_cached_fields_keep_live_values_and_nested_types():
    @dataclass
    class Base:
        hp: int = 80

    @dataclass
    class Child(Base):
        properties: tuple = ()

    observation._dataclass_fields.cache_clear()
    value = Child(properties=(('cost', 1), ('items', [Base(7)])))
    assert observation._json_value(value) == {'hp': 80, 'properties': {'cost': 1, 'items': [{'hp': 7}]}}
    value.hp = 3
    value.properties[1][1][0].hp = 4
    assert observation._json_value(value) == {'hp': 3, 'properties': {'cost': 1, 'items': [{'hp': 4}]}}
    assert observation._dataclass_fields.cache_info().hits > 0
    assert observation._json_value(Base) == {'hp': 80}


def test_metadata_cache_eviction_does_not_change_serialization():
    observation._dataclass_fields.cache_clear()
    types = [make_dataclass(f'Probe{index}', [('hp', int)]) for index in range(130)]
    for index, cls in enumerate(types):
        assert observation._json_value(cls(index)) == {'hp': index}
    assert observation._dataclass_fields.cache_info().currsize == 128
    assert observation._json_value(types[0](999)) == {'hp': 999}


def test_serializer_preserves_order_and_empty_property_representation():
    @dataclass
    class Payload:
        z: tuple = ()
        a: tuple = (('second', [1, {'nested': ()}]), ('first', True))

    raw = observation._json_value(Payload())
    assert list(raw) == ['z', 'a']
    assert raw['z'] == {}
    assert list(raw['a']) == ['second', 'first']
    assert raw['a']['second'][1] == {'nested': {}}


def test_reused_forbidden_fields_are_immutable_and_keep_error_path():
    assert isinstance(observation._FORBIDDEN_PUBLIC_FIELDS, frozenset)
    with pytest.raises(ValueError) as error:
        observation._assert_public_tree({'allowed': [{'__SeEd': 1}]})
    assert str(error.value) == 'hidden field is forbidden at observation.allowed[0].__SeEd'


@pytest.mark.parametrize('key', ['seed', '_rng_state', 'action_bits', 'rng_extra'])
def test_hidden_fields_remain_rejected_after_metadata_cache_hit(key):
    @dataclass
    class Payload:
        properties: tuple = ()

    observation._json_value(Payload())
    raw = observation._json_value(Payload(properties=(('nested', [{key: 1}]),)))
    with pytest.raises(ValueError, match='hidden field'):
        observation._assert_public_tree(raw)
