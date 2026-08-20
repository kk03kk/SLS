"""CommunicationMod payload adapter for the canonical FullRun contract."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Sequence

from sls.content.normalize import normalize_card_id, normalize_content_id, normalize_potion_id
from sls.contracts import (
    Action,
    ActionKind,
    Card,
    Decision,
    Enemy,
    MapNode,
    Observation,
    Player,
    PublicEntity,
    RunContext,
    ScreenType,
    ShopItem,
)


ROOM_SYMBOLS = {
    "M": "MONSTER",
    "?": "EVENT",
    "$": "SHOP",
    "E": "ELITE",
    "T": "TREASURE",
    "R": "REST",
}


@dataclass(frozen=True, slots=True)
class AdaptedOriginalDecision:
    decision: Decision
    commands: Mapping[str, tuple[str, ...]]


def adapt_original(payload: Mapping[str, Any]) -> AdaptedOriginalDecision:
    """Convert one ready CommunicationMod state without exposing wire commands."""

    game = _mapping(payload.get("game_state"))
    combat = _mapping(game.get("combat_state"))
    screen_state = _mapping(game.get("screen_state"))
    screen = _screen_type(payload, game, combat)
    parity_run = _mapping(game.get("_parity_run") or payload.get("_parity_run"))
    visible_player = _mapping(combat.get("player")) if combat else game

    hand = _cards(combat.get("hand"), "HAND", preserve_order=True)
    has_frozen_eye = any(
        normalize_content_id(item.get("id")) == "FROZEN_EYE"
        for item in _sequence(game.get("relics"))
    )
    draw = _cards(
        combat.get("draw_pile"), "DRAW", preserve_order=has_frozen_eye,
        visible_order=has_frozen_eye,
    )
    discard = _cards(combat.get("discard_pile"), "DISCARD")
    exhaust = _cards(combat.get("exhaust_pile"), "EXHAUST")
    deck = _cards(game.get("deck"), "DECK", preserve_order=True)
    parity_intents = _mappings(payload.get("_monster_intents"))
    enemies = tuple(
        _enemy(monster, index, parity_intents[index] if index < len(parity_intents) else {})
        for index, monster in enumerate(_mappings(combat.get("monsters")))
    )
    powers = _powers(_mapping(combat.get("player")).get("powers"), "PLAYER_POWER")
    for monster_index, monster in enumerate(_mappings(combat.get("monsters"))):
        powers += _powers(monster.get("powers"), f"MONSTER:{monster_index}:POWER")

    actions, commands = _actions(payload, game, combat, screen_state, screen, hand)
    options = _screen_entities(game, combat, screen_state, screen)
    outcome = str(game.get("screen_type") or "").upper()
    terminal = screen is ScreenType.GAME_OVER or outcome in {"DEATH", "VICTORY"}
    if terminal:
        actions = ()
        commands = {}

    observation = Observation(
        player=Player(
            normalize_content_id(game.get("class", game.get("character", "IRONCLAD"))),
            _integer(visible_player.get("current_hp", game.get("current_hp"))),
            _integer(visible_player.get("max_hp", game.get("max_hp"))),
            _integer(visible_player.get("block")),
            _integer(visible_player.get("energy")),
            _integer(visible_player.get("max_energy", 3)),
        ),
        run=RunContext(
            _integer(game.get("ascension_level", game.get("ascension"))),
            _integer(game.get("act")),
            _integer(game.get("floor")),
            _integer(game.get("gold")),
            bool(parity_run.get("ruby_key", False)),
            bool(parity_run.get("emerald_key", False)),
            bool(parity_run.get("sapphire_key", False)),
            _optional_content_id(game.get("act_boss")),
        ),
        screen=screen,
        deck=deck,
        hand=hand,
        draw_pile=draw,
        discard_pile=discard,
        exhaust_pile=exhaust,
        enemies=enemies,
        powers=powers,
        relics=tuple(
            PublicEntity(
                f"RELIC:{index}", normalize_content_id(item.get("id")),
                (("counter", max(0, _integer(item.get("counter", 0)))),),
            )
            for index, item in enumerate(_mappings(game.get("relics")))
        ),
        potions=tuple(
            PublicEntity(
                f"POTION:{index}", normalize_potion_id(item.get("id")),
                (("slot", index),),
            )
            for index, item in enumerate(_mappings(game.get("potions")))
            if normalize_potion_id(item.get("id")) != "EMPTY_POTION_SLOT"
        ),
        map_nodes=_map_nodes(game, parity_run),
        choice_options=options["choice"],
        reward_options=options["reward"],
        shop_items=options["shop"],
        event_options=options["event"],
        rest_options=options["rest"],
        boss_relic_options=options["boss"],
        public_context=(("turn", _integer(combat.get("turn"))),) if combat else (),
    )
    return AdaptedOriginalDecision(Decision(observation, actions, terminal), commands)


def _actions(
    payload: Mapping[str, Any],
    game: Mapping[str, Any],
    combat: Mapping[str, Any],
    state: Mapping[str, Any],
    screen: ScreenType,
    hand: tuple[Card, ...],
) -> tuple[tuple[Action, ...], dict[str, tuple[str, ...]]]:
    available = {str(item).lower() for item in _sequence(payload.get("available_commands"))}
    result: list[Action] = []
    commands: dict[str, tuple[str, ...]] = {}

    def add(action: Action, *wire: str) -> None:
        if action.candidate_id in commands:
            raise ValueError(f"Original actions collapse to {action.candidate_id}")
        result.append(action)
        commands[action.candidate_id] = tuple(wire)

    if combat:
        monsters = _mappings(combat.get("monsters"))
        if "play" in available:
            for index, (card, raw) in enumerate(zip(hand, _mappings(combat.get("hand")))):
                if not bool(raw.get("is_playable", False)):
                    continue
                if bool(raw.get("has_target", raw.get("requires_target", False))):
                    for target, monster in enumerate(monsters):
                        if _integer(monster.get("current_hp")) > 0 and not monster.get("is_gone"):
                            add(
                                Action(
                                    ActionKind.PLAY_CARD,
                                    subject_id=card.instance_id,
                                    target_id=f"MONSTER:{target}",
                                ),
                                f"play {index + 1} {target}",
                            )
                else:
                    add(Action(ActionKind.PLAY_CARD, subject_id=card.instance_id), f"play {index + 1}")
        choice = _mappings(_mapping(combat.get("card_select")).get("cards"))
        if not choice:
            choice = _mappings(game.get("choice_list"))
        if "choose" in available and choice:
            for index, _ in enumerate(choice):
                add(Action(ActionKind.SELECT_CARD, subject_id=f"CHOICE:{index}"), f"choose {index}")
        if "confirm" in available:
            add(Action(ActionKind.CONFIRM, option_id="combat-selection"), "confirm")
        if "potion" in available:
            for slot, potion in enumerate(_mappings(game.get("potions"))):
                if normalize_potion_id(potion.get("id")) == "EMPTY_POTION_SLOT":
                    continue
                if bool(potion.get("can_use", True)):
                    if bool(potion.get("requires_target", False)):
                        for target, monster in enumerate(monsters):
                            if _integer(monster.get("current_hp")) > 0 and not monster.get("is_gone"):
                                add(
                                    Action(
                                        ActionKind.USE_POTION,
                                        subject_id=f"POTION:{slot}",
                                        target_id=f"MONSTER:{target}",
                                    ),
                                    f"potion use {slot} {target}",
                                )
                    else:
                        add(
                            Action(ActionKind.USE_POTION, subject_id=f"POTION:{slot}"),
                            f"potion use {slot}",
                        )
                add(
                    Action(ActionKind.DISCARD_POTION, subject_id=f"POTION:{slot}"),
                    f"potion discard {slot}",
                )
        if "end" in available:
            add(Action(ActionKind.END_TURN), "end")
        return tuple(result), commands

    choices = _sequence(game.get("choice_list"))
    if screen in {ScreenType.NEOW, ScreenType.EVENT} and "choose" in available:
        kind = ActionKind.CHOOSE_NEOW_OPTION if screen is ScreenType.NEOW else ActionKind.CHOOSE_EVENT_OPTION
        for index, _ in enumerate(choices):
            add(Action(kind, option_id=f"event-option:{index}"), f"choose {index}")
    elif screen is ScreenType.MAP and "choose" in available:
        nodes = _mappings(state.get("next_nodes"))
        for index, node in enumerate(nodes):
            add(
                Action(
                    ActionKind.CHOOSE_MAP_NODE,
                    node_id=f"map:{_integer(node.get('x'))}:{_integer(node.get('y'))}",
                ),
                f"choose {index}",
            )
        if bool(state.get("boss_available")) and not nodes:
            add(Action(ActionKind.CHOOSE_MAP_NODE, node_id="map:boss"), "choose 0")
    elif screen is ScreenType.CARD_REWARD:
        cards = _mappings(state.get("cards"))
        kind = {
            "UPGRADE": ActionKind.UPGRADE_CARD,
            "PURGE": ActionKind.REMOVE_CARD,
            "REMOVE": ActionKind.REMOVE_CARD,
        }.get(str(state.get("type") or state.get("select_type") or "").upper(), ActionKind.SELECT_CARD)
        for index, _ in enumerate(cards):
            add(Action(kind, subject_id=f"select-card:{index}"), f"choose {index}")
        if "skip" in available:
            add(Action(ActionKind.SKIP_CARD_REWARD), "skip")
    elif screen is ScreenType.COMBAT_REWARD:
        counters: dict[str, int] = {}
        for choice_index, reward in enumerate(_mappings(state.get("rewards"))):
            reward_type = str(reward.get("reward_type") or "UNKNOWN").upper()
            occurrence = counters.get(reward_type, 0)
            counters[reward_type] = occurrence + 1
            if reward_type == "CARD":
                cards = _mappings(reward.get("cards"))
                for card_index, _ in enumerate(cards):
                    add(
                        Action(
                            ActionKind.CHOOSE_CARD_REWARD,
                            subject_id=f"reward-card:{occurrence}:{card_index}",
                        ),
                        f"choose {choice_index}", f"choose {card_index}",
                    )
                add(
                    Action(
                        ActionKind.SKIP_CARD_REWARD,
                        option_id=f"reward-card:{occurrence}",
                    ),
                    f"choose {choice_index}",
                    "skip",
                )
            elif reward_type == "GOLD":
                add(Action(ActionKind.TAKE_REWARD, reward_id=f"reward-gold:{occurrence}"), f"choose {choice_index}")
            elif reward_type == "POTION":
                add(Action(ActionKind.TAKE_REWARD, reward_id=f"reward-potion:{occurrence}"), f"choose {choice_index}")
            elif reward_type == "RELIC":
                add(Action(ActionKind.TAKE_REWARD, reward_id=f"reward-relic:{occurrence}"), f"choose {choice_index}")
            elif "SAPPHIRE" in reward_type:
                add(Action(ActionKind.TAKE_BLUE_KEY, reward_id="reward-key:sapphire"), f"choose {choice_index}")
            elif "KEY" in reward_type:
                add(Action(ActionKind.TAKE_REWARD, reward_id="reward-key:emerald"), f"choose {choice_index}")
        if "proceed" in available:
            add(Action(ActionKind.SKIP_REWARD), "proceed")
    elif screen is ScreenType.BOSS_REWARD and "choose" in available:
        relics = _sequence(state.get("relics") or choices)
        for index, _ in enumerate(relics):
            add(Action(ActionKind.CHOOSE_BOSS_RELIC, subject_id=f"boss-relic:{index}"), f"choose {index}")
    elif screen is ScreenType.SHOP:
        gold = _integer(game.get("gold"))
        compact: list[tuple[str, int, Mapping[str, Any]]] = []
        if state.get("purge_available") and _integer(state.get("purge_cost"), 10**9) <= gold:
            compact.append(("REMOVE", 0, {}))
        for label, key in (("CARD", "cards"), ("RELIC", "relics"), ("POTION", "potions")):
            for index, item in enumerate(_mappings(state.get(key))):
                if _integer(item.get("price"), 10**9) <= gold:
                    compact.append((label, index, item))
        for choice_index, (label, index, _) in enumerate(compact):
            if label == "CARD":
                action = Action(ActionKind.BUY_CARD, subject_id=f"shop-card:{index}")
            elif label == "RELIC":
                action = Action(ActionKind.BUY_RELIC, subject_id=f"shop-relic:{index}")
            elif label == "POTION":
                action = Action(ActionKind.BUY_POTION, subject_id=f"shop-potion:{index}")
            else:
                action = Action(ActionKind.CONFIRM, option_id="shop-remove")
            add(action, f"choose {choice_index}")
        if "leave" in available:
            add(Action(ActionKind.LEAVE_SHOP), "leave")
    elif screen is ScreenType.REST and "choose" in available:
        for index, choice in enumerate(choices):
            label = normalize_content_id(choice.get("text") if isinstance(choice, Mapping) else choice)
            kind = {
                "REST": ActionKind.REST,
                "SMITH": ActionKind.CONFIRM,
                "RECALL": ActionKind.RECALL,
                "LIFT": ActionKind.LIFT,
                "TOKE": ActionKind.CONFIRM,
                "DIG": ActionKind.DIG,
            }.get(label, ActionKind.PROCEED)
            add(Action(kind, option_id=f"rest-option:{index}"), f"choose {index}")
    elif screen is ScreenType.TREASURE:
        if "choose" in available:
            add(Action(ActionKind.OPEN_CHEST), "choose 0")
        if "proceed" in available:
            add(Action(ActionKind.PROCEED), "proceed")
    elif "proceed" in available:
        add(Action(ActionKind.PROCEED), "proceed")
    return tuple(result), commands


def _screen_type(
    payload: Mapping[str, Any], game: Mapping[str, Any], combat: Mapping[str, Any],
) -> ScreenType:
    if combat:
        return ScreenType.COMBAT
    if not payload.get("in_game", True):
        return ScreenType.GAME_OVER
    raw = str(game.get("screen_type") or "NONE").upper()
    if raw in {"DEATH", "VICTORY", "GAME_OVER", "COMPLETE"}:
        return ScreenType.GAME_OVER
    if raw in {"EVENT", "NEOW"}:
        return ScreenType.NEOW if _integer(game.get("floor")) == 0 else ScreenType.EVENT
    return {
        "MAP": ScreenType.MAP,
        "COMBAT_REWARD": ScreenType.COMBAT_REWARD,
        "CARD_REWARD": ScreenType.CARD_REWARD,
        "GRID": ScreenType.CARD_REWARD,
        "BOSS_REWARD": ScreenType.BOSS_REWARD,
        "SHOP_SCREEN": ScreenType.SHOP,
        "SHOP_ROOM": ScreenType.SHOP,
        "REST": ScreenType.REST,
        "REST_ROOM": ScreenType.REST,
        "CHEST": ScreenType.TREASURE,
    }.get(raw, ScreenType.ACT_TRANSITION)


def _cards(
    values: Any,
    zone: str,
    *,
    preserve_order: bool = False,
    visible_order: bool = False,
) -> tuple[Card, ...]:
    cards = list(_mappings(values))
    if not preserve_order:
        cards.sort(key=lambda value: (
            normalize_card_id(value.get("id")),
            _integer(value.get("upgrades")),
            _integer(value.get("cost")),
        ))
    return tuple(
        Card(
            f"{zone}:{index}" if zone != "DRAW" or visible_order else f"DRAW:HIDDEN:{index}",
            normalize_card_id(card.get("id")),
            zone,
            _integer(card.get("upgrades")),
            _integer(card.get("cost", card.get("base_cost"))),
            _integer(card.get("cost_for_turn", card.get("cost", card.get("base_cost")))),
            bool(card.get("is_playable", False)) if zone == "HAND" else False,
            index if zone == "DRAW" and visible_order else None,
            (("order_is_visible", True),) if zone == "DRAW" and visible_order else (),
        )
        for index, card in enumerate(cards)
    )


def _powers(values: Any, prefix: str) -> tuple[PublicEntity, ...]:
    return tuple(
        PublicEntity(
            f"{prefix}:{index}", normalize_content_id(value.get("id")),
            (("amount", _integer(value.get("amount"))),),
        )
        for index, value in enumerate(_mappings(values))
    )


def _enemy(monster: Mapping[str, Any], index: int, parity: Mapping[str, Any]) -> Enemy:
    monster_id = normalize_content_id(monster.get("id"))
    intent = str(parity.get("intent") or monster.get("intent") or "UNKNOWN").upper()
    if intent == "DEBUG" and (monster_id, _integer(monster.get("move_id"))) == ("CULTIST", 3):
        intent = "BUFF"
    non_attack = intent in {
        "BUFF", "DEBUFF", "DEFEND", "ESCAPE", "MAGIC", "SLEEP", "STUN", "UNKNOWN",
    }
    damage = _integer(
        parity.get("damage", monster.get("move_adjusted_damage", monster.get("intent_damage")))
    )
    hits = _integer(parity.get("hits", monster.get("move_hits", monster.get("intent_hits", 1))))
    if non_attack:
        damage, hits = 0, 0
    return Enemy(
        f"MONSTER:{index}", monster_id,
        _integer(monster.get("current_hp")), _integer(monster.get("max_hp")),
        _integer(monster.get("block")), intent, damage, hits,
        (("is_gone", bool(monster.get("is_gone", False))),),
    )


def _map_nodes(game: Mapping[str, Any], parity_run: Mapping[str, Any]) -> tuple[MapNode, ...]:
    nodes = []
    reachable = {
        (_integer(node.get("x")), _integer(node.get("y")))
        for node in _mappings(_mapping(game.get("screen_state")).get("next_nodes"))
    }
    if _integer(game.get("floor")) == 0 and not reachable:
        reachable = {
            (_integer(node.get("x")), 0)
            for node in _mappings(game.get("map")) if _integer(node.get("y")) == 0
        }
    burning = (
        _integer(parity_run.get("burning_elite_x"), -1),
        _integer(parity_run.get("burning_elite_y"), -1),
    )
    current = (
        _integer(parity_run.get("current_map_x"), -99),
        _integer(parity_run.get("current_map_y"), -99),
    )
    if current != (-99, -99) and not reachable:
        for node in _mappings(game.get("map")):
            if (_integer(node.get("x")), _integer(node.get("y"))) == current:
                reachable = {
                    (_integer(edge.get("x")), _integer(edge.get("y")))
                    for edge in _mappings(node.get("children"))
                }
                break
    for node in _mappings(game.get("map")):
        x, y = _integer(node.get("x")), _integer(node.get("y"))
        nodes.append(MapNode(
            f"map:{x}:{y}", x, y,
            (
                "BURNING_ELITE" if (x, y) == burning else
                ROOM_SYMBOLS.get(str(node.get("symbol")), str(node.get("symbol") or "UNKNOWN"))
            ),
            (x, y) in reachable,
            tuple(
                f"map:{_integer(edge.get('x'))}:{15 if _integer(edge.get('y')) == 16 else _integer(edge.get('y'))}"
                for edge in _mappings(node.get("children"))
            ),
        ))
    return tuple(nodes)


def _screen_entities(
    game: Mapping[str, Any], combat: Mapping[str, Any], state: Mapping[str, Any], screen: ScreenType,
) -> dict[str, tuple[Any, ...]]:
    result: dict[str, tuple[Any, ...]] = {
        "choice": (), "reward": (), "shop": (), "event": (), "rest": (), "boss": (),
    }
    choices = _sequence(game.get("choice_list"))
    if combat and choices:
        result["choice"] = tuple(
            PublicEntity(f"CHOICE:{index}", normalize_content_id(_choice_id(value)))
            for index, value in enumerate(choices)
        )
    elif screen in {ScreenType.NEOW, ScreenType.EVENT}:
        event_id = "NEOW" if screen is ScreenType.NEOW else normalize_content_id(
            state.get("event_id") or game.get("event_id") or "EVENT"
        )
        result["event"] = tuple(
            PublicEntity(f"event-option:{index}", f"{event_id}:OPTION:{index}")
            for index, value in enumerate(choices)
        )
    elif screen is ScreenType.REST:
        result["rest"] = tuple(
            PublicEntity(f"rest-option:{index}", normalize_content_id(_choice_id(value)))
            for index, value in enumerate(choices)
        )
    elif screen is ScreenType.BOSS_REWARD:
        result["boss"] = tuple(
            PublicEntity(f"boss-relic:{index}", normalize_content_id(_choice_id(value)))
            for index, value in enumerate(_sequence(state.get("relics") or choices))
        )
    elif screen is ScreenType.COMBAT_REWARD:
        entities: list[PublicEntity] = []
        counters: dict[str, int] = {}
        for reward in _mappings(state.get("rewards")):
            kind = str(reward.get("reward_type") or "UNKNOWN").upper()
            index = counters.get(kind, 0)
            counters[kind] = index + 1
            if kind == "CARD":
                entities.extend(
                    PublicEntity(
                        f"reward-card:{index}:{card_index}",
                        normalize_card_id(card.get("id")),
                        (("upgrades", _integer(card.get("upgrades"))),),
                    )
                    for card_index, card in enumerate(_mappings(reward.get("cards")))
                )
            elif kind == "GOLD":
                entities.append(PublicEntity(
                    f"reward-gold:{index}", "GOLD",
                    (("amount", _integer(reward.get("gold", reward.get("amount")))),),
                ))
            elif kind == "RELIC":
                entities.append(PublicEntity(
                    f"reward-relic:{index}",
                    normalize_content_id(reward.get("id", reward.get("relic"))),
                ))
            elif kind == "POTION":
                entities.append(PublicEntity(
                    f"reward-potion:{index}",
                    normalize_potion_id(reward.get("id", reward.get("potion"))),
                ))
            elif "SAPPHIRE" in kind:
                entities.append(PublicEntity("reward-key:sapphire", "SAPPHIRE_KEY"))
            elif "KEY" in kind:
                entities.append(PublicEntity("reward-key:emerald", "EMERALD_KEY"))
        result["reward"] = tuple(entities)
    elif screen is ScreenType.CARD_REWARD:
        is_grid = str(game.get("screen_type") or "").upper() == "GRID"
        result["reward"] = tuple(
            PublicEntity(
                f"select-card:{index}", normalize_card_id(card.get("id")),
                (
                    (("deck_index", index), ("upgrades", _integer(card.get("upgrades"))))
                    if is_grid else (("upgrades", _integer(card.get("upgrades"))),)
                ),
            )
            for index, card in enumerate(_mappings(state.get("cards")))
        )
    elif screen is ScreenType.SHOP:
        items = []
        for kind, key in (("CARD", "cards"), ("RELIC", "relics"), ("POTION", "potions")):
            for index, item in enumerate(_mappings(state.get(key))):
                content = normalize_card_id(item.get("id")) if kind == "CARD" else normalize_content_id(item.get("id"))
                items.append(ShopItem(
                    f"shop-{kind.lower()}:{index}", content, kind,
                    _integer(item.get("price")), bool(item.get("sold", False)),
                ))
        result["shop"] = tuple(items)
    return result


def _choice_id(value: Any) -> Any:
    if isinstance(value, Mapping):
        return value.get("id", value.get("text", value.get("name", "OPTION")))
    return value


def _optional_content_id(value: Any) -> str | None:
    normalized = normalize_content_id(value)
    return None if normalized in {"", "INVALID", "NONE", "UNKNOWN"} else normalized


def _integer(value: Any, default: int = 0) -> int:
    try:
        return int(value if value is not None else default)
    except (TypeError, ValueError):
        return default


def _mapping(value: Any) -> Mapping[str, Any]:
    return value if isinstance(value, Mapping) else {}


def _sequence(value: Any) -> Sequence[Any]:
    return value if isinstance(value, Sequence) and not isinstance(value, (str, bytes)) else ()


def _mappings(value: Any) -> tuple[Mapping[str, Any], ...]:
    return tuple(item for item in _sequence(value) if isinstance(item, Mapping))
