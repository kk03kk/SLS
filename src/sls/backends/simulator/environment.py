"""Canonical FullRun backend backed by the native simulator."""

from __future__ import annotations

from typing import Any, Mapping

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
    Transition,
    ValidationSnapshot,
)
from sls.curriculum import CurriculumProfile, IRONCLAD_A0_HEART, evaluate_horizon
from sls.contracts.continuation import continuation_simulator
from sls.content.normalize import normalize_content_id


class SimulatorBackend:
    """Policy-safe wrapper around the native full-run state machine.

    Native command bits exist only in ``_candidate_bits``.  They are never
    placed in :class:`Observation`, action metadata, logs, or policy input.
    """

    def __init__(self, profile: CurriculumProfile = IRONCLAD_A0_HEART) -> None:
        if profile.character_id != "IRONCLAD":
            raise ValueError("the native FullRun backend currently supports IRONCLAD only")
        from sls.backends.simulator.native import LightspeedRunState

        self.profile = profile
        self._native = LightspeedRunState()
        self._candidate_bits: dict[str, int] = {}
        self._last_raw: dict[str, Any] | None = None

    @property
    def raw_state(self) -> dict[str, Any]:
        if self._last_raw is None:
            raise RuntimeError("backend has not been reset")
        return self._last_raw

    def reset(self, seed: int) -> Decision:
        self._native.reset(int(seed), self.profile.ascension)
        return self._adapt(self._native.snapshot())

    def step(
        self, action: Action | str, *, validation_evidence: Mapping[str, Any] | None = None,
    ) -> Transition:
        if validation_evidence:
            unknown = set(validation_evidence) - {
                "discovery_retrieval_updates", "card_soul_cost_reset_count",
            }
            if unknown:
                raise ValueError(f"unknown validation evidence: {sorted(unknown)}")
            if "discovery_retrieval_updates" in validation_evidence:
                self._native._set_discovery_retrieval_updates_for_validation(
                    int(validation_evidence["discovery_retrieval_updates"]),
                )
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            bits = self._candidate_bits[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current decision boundary") from error
        raw = self._native.step(bits)
        if validation_evidence and "card_soul_cost_reset_count" in validation_evidence:
            self._native._reset_last_hand_card_costs_for_validation(
                int(validation_evidence["card_soul_cost_reset_count"]),
            )
            raw = self._native.snapshot()
        next_decision = self._adapt(raw)
        decision = evaluate_horizon(self.profile, next_decision.observation)
        if decision.terminated != next_decision.terminal:
            next_decision = Decision(
                next_decision.observation,
                () if decision.terminated else next_decision.actions,
                decision.terminated,
            )
        return Transition(
            decision=next_decision,
            reward=1.0 if decision.success else -1.0 if decision.reason == "DEATH" else 0.0,
            terminated=decision.terminated,
            truncated=False,
            info={"reason": decision.reason, "success": decision.success},
        )

    def checkpoint(self) -> dict[str, Any]:
        """Return the exact native checkpoint; this object is not policy input."""

        if self._last_raw is None:
            raise RuntimeError("reset must be called before checkpoint")
        return self._native.snapshot()

    def load_checkpoint(self, state: Mapping[str, Any]) -> Decision:
        self._native.load_state(dict(state))
        return self._adapt(self._native.snapshot())

    def _adapt(self, raw: Mapping[str, Any]) -> Decision:
        self._last_raw = dict(raw)
        public_run = raw["public_run"]
        player_state = raw["player_state"]
        inventory = raw["public_inventory"]
        combat = raw.get("public_combat")
        visible_player = combat["player"] if combat else player_state
        screen = _screen_type(raw)

        deck = tuple(
            Card(
                f"DECK:{index}", str(card["content_id"]), "DECK",
                int(card["upgrades"]), int(card["base_cost"]), int(card["current_cost"]),
            )
            for index, card in enumerate(inventory["deck"])
        )
        has_frozen_eye = any(
            str(relic["content_id"]) == "FROZEN_EYE"
            for relic in inventory["relics"]
        )
        card_zones = {
            zone: _combat_cards(
                combat.get(key, ()) if combat else (), zone,
                draw_order_visible=has_frozen_eye,
            )
            for zone, key in (
                ("HAND", "hand"), ("DRAW", "draw_pile"),
                ("DISCARD", "discard_pile"), ("EXHAUST", "exhaust_pile"),
            )
        }
        enemies = tuple(
            Enemy(
                f"MONSTER:{index}", str(monster["content_id"]),
                int(monster["current_hp"]), int(monster["max_hp"]), int(monster["block"]),
                str(monster["intent"]), int(monster["intent_damage"]),
                int(monster["intent_hits"]), (("is_gone", bool(monster["is_gone"])),),
            )
            for index, monster in enumerate(combat.get("monsters", ()) if combat else ())
        )
        powers = _powers(combat["player"].get("powers", ()) if combat else (), "PLAYER_POWER")
        for index, monster in enumerate(combat.get("monsters", ()) if combat else ()):
            if not bool(monster.get("is_gone", False)) and int(monster.get("current_hp", 0)) > 0:
                powers += _powers(monster.get("powers", ()), f"MONSTER:{index}:POWER")

        actions, candidate_bits = _semantic_actions(raw, card_zones["HAND"])
        self._candidate_bits = candidate_bits
        options = _screen_entities(raw)
        if screen is ScreenType.GAME_OVER:
            card_zones = {zone: () for zone in card_zones}
            enemies = ()
            powers = ()
        observation = Observation(
            player=Player(
                "IRONCLAD", int(visible_player["current_hp"]), int(visible_player["max_hp"]),
                int(visible_player.get("block", 0)), int(visible_player.get("energy", 0)),
                int(visible_player.get("max_energy", 3)),
            ),
            run=RunContext(
                int(public_run["ascension"]), int(public_run["act"]), int(public_run["floor"]),
                int(public_run["gold"]), bool(player_state["red_key"]),
                bool(player_state["green_key"]), bool(player_state["blue_key"]),
                None if public_run["visible_boss_id"] == "INVALID" else str(public_run["visible_boss_id"]),
            ),
            screen=screen,
            deck=deck,
            hand=card_zones["HAND"],
            draw_pile=card_zones["DRAW"],
            discard_pile=card_zones["DISCARD"],
            exhaust_pile=card_zones["EXHAUST"],
            enemies=enemies,
            powers=powers,
            relics=tuple(
                _entity(
                    f"RELIC:{index}", value["content_id"],
                    counter=max(0, int(value["counter"])),
                )
                for index, value in enumerate(inventory["relics"])
            ),
            potions=tuple(
                _entity(f"POTION:{int(value['slot'])}", value["content_id"], slot=int(value["slot"]))
                for value in inventory["potions"]
                if value["content_id"] not in {"INVALID", "EMPTY_POTION_SLOT"}
            ),
            map_nodes=tuple(
                MapNode(
                    str(node["node_id"]), int(node["x"]), int(node["y"]),
                    (
                        "BURNING_ELITE"
                        if node["burning"] and not bool(player_state["green_key"])
                        else str(node["room_type"])
                    ),
                    bool(node["reachable"]), tuple(str(item) for item in node["outgoing_node_ids"]),
                )
                for node in raw["public_map"]
            ),
            choice_options=options["choice"],
            reward_options=options["reward"],
            shop_items=options["shop"],
            event_options=options["event"],
            rest_options=options["rest"],
            boss_relic_options=options["boss"],
            public_context=(
                (("turn", int(combat["turn"])),)
                if combat and screen is not ScreenType.GAME_OVER else ()
            ),
        )
        return Decision(
            observation=observation,
            actions=actions,
            terminal=screen is ScreenType.GAME_OVER,
        )

    def validation_snapshot(self) -> ValidationSnapshot:
        if self._last_raw is None:
            raise RuntimeError("reset must be called before validation_snapshot")
        raw = self._last_raw
        rng = (
            (raw.get("combat_checkpoint") or {}).get("rng")
            if raw.get("public_combat")
            else raw.get("rng")
        ) or {}
        return ValidationSnapshot(
            public_state={
                "public_run": raw.get("public_run"),
                "player_state": raw.get("player_state"),
                "public_inventory": raw.get("public_inventory"),
                "public_map": raw.get("public_map"),
                "public_combat": raw.get("public_combat"),
                "public_screen": raw.get("public_screen"),
            },
            rng_streams=rng,
            continuation=continuation_simulator(raw),
        )


def _entity(instance_id: Any, content_id: Any, **properties: int | float | bool | str) -> PublicEntity:
    return PublicEntity(
        str(instance_id), str(content_id), tuple(sorted(properties.items())),
    )


def _combat_cards(
    cards: Any, zone: str, *, draw_order_visible: bool = False,
) -> tuple[Card, ...]:
    if zone == "HAND" or (zone == "DRAW" and draw_order_visible):
        ordered = list(cards)
    else:
        # Original STS exposes pile contents but not draw order.  Sort solely by
        # public card attributes; native unique ids must not become an ordering
        # side channel for policy input.
        ordered = sorted(
            cards,
            key=lambda value: (
                str(value["content_id"]), int(value["upgrades"]),
                int(value["base_cost"]), int(value["cost"]),
            ),
        )
    return tuple(
        Card(
            (
                f"{zone}:{index}"
                if zone != "DRAW" or draw_order_visible
                else f"DRAW:HIDDEN:{index}"
            ),
            str(card["content_id"]), zone,
            int(card["upgrades"]), int(card["base_cost"]), int(card["cost"]),
            bool(card["is_playable"]) if zone == "HAND" else False,
            index if zone == "DRAW" and draw_order_visible else None,
            (("order_is_visible", True),)
            if zone == "DRAW" and draw_order_visible else (),
        )
        for index, card in enumerate(ordered)
    )


def _powers(values: Any, prefix: str) -> tuple[PublicEntity, ...]:
    visible = [
        value for value in values
        if normalize_content_id(value["id"]) not in {"ASLEEP"}
    ]
    return tuple(
        _entity(
            f"{prefix}:{index}", normalize_content_id(value["id"]),
            amount=int(value["amount"]),
        )
        for index, value in enumerate(visible)
    )


def _screen_type(raw: Mapping[str, Any]) -> ScreenType:
    public = raw["public_run"]
    if int(public["outcome"]) != 1:
        return ScreenType.GAME_OVER
    screen = int(public["screen_state"])
    if screen == 1:
        return ScreenType.NEOW if public["current_event_id"] == "NEOW" else ScreenType.EVENT
    return {
        2: ScreenType.COMBAT_REWARD,
        3: ScreenType.BOSS_REWARD,
        4: ScreenType.CARD_REWARD,
        5: ScreenType.MAP,
        6: ScreenType.TREASURE,
        7: ScreenType.REST,
        8: ScreenType.SHOP,
        9: ScreenType.COMBAT,
    }.get(screen, ScreenType.ACT_TRANSITION)


def _semantic_actions(
    raw: Mapping[str, Any], hand: tuple[Card, ...]
) -> tuple[tuple[Action, ...], dict[str, int]]:
    screen = _screen_type(raw)
    progress = raw["progress_state"]
    semantic: list[Action] = []
    mapping: dict[str, int] = {}
    for ordinal, native in enumerate(raw["legal_actions"]):
        if native.get("domain") == "COMBAT":
            action_type = int(native["action_type"])
            source = int(native["source_index"])
            target = int(native["target_index"])
            if action_type == 0:
                action = Action(
                    ActionKind.PLAY_CARD,
                    subject_id=hand[source].instance_id,
                    target_id=f"MONSTER:{target}" if native.get("requires_target") else None,
                )
            elif action_type == 1:
                kind = ActionKind.DISCARD_POTION if target == 6 else ActionKind.USE_POTION
                action = Action(
                    kind, subject_id=f"POTION:{source}",
                    target_id=f"MONSTER:{target}" if native.get("requires_target") else None,
                )
            elif action_type == 2:
                options = raw["public_combat"]["choice"]["options"]
                action = Action(
                    ActionKind.SELECT_CARD,
                    subject_id=f"CHOICE:{source}",
                )
            elif action_type == 3:
                options = raw["public_combat"]["choice"]["options"]
                selected = ",".join(
                    f"CHOICE:{int(value)}"
                    for value in native.get("selected_indices", ())
                )
                action = Action(ActionKind.CONFIRM, option_id=f"combat-selection:{selected}")
            else:
                action = Action(ActionKind.END_TURN)
        else:
            action = _run_action(raw, native, ordinal, screen, progress)
        if action.candidate_id in mapping:
            raise RuntimeError(f"native legal actions collapse to one semantic identity: {action.candidate_id}")
        semantic.append(action)
        mapping[action.candidate_id] = int(native["bits"])
    return tuple(semantic), mapping


def _run_action(
    raw: Mapping[str, Any], native: Mapping[str, Any], ordinal: int,
    screen: ScreenType, progress: Mapping[str, Any],
) -> Action:
    idx1, idx2 = int(native["idx1"]), int(native["idx2"])
    reward_type = int(native["reward_type"])
    if screen in {ScreenType.NEOW, ScreenType.EVENT}:
        if raw["public_run"]["current_event_id"] == "Match and Keep":
            return Action(
                ActionKind.CHOOSE_EVENT_OPTION,
                option_id=f"match-pair:{idx1}:{idx2}",
            )
        return Action(
            ActionKind.CHOOSE_NEOW_OPTION if screen is ScreenType.NEOW else ActionKind.CHOOSE_EVENT_OPTION,
            option_id=f"event-option:{idx1}",
        )
    if screen is ScreenType.MAP:
        y = int(progress["current_map_y"])
        node_id = f"map:{idx1}:{y + 1}" if y < 14 else "map:boss"
        return Action(ActionKind.CHOOSE_MAP_NODE, node_id=node_id)
    if screen is ScreenType.COMBAT_REWARD:
        if reward_type == 0:
            if idx2 == 6:
                return Action(
                    ActionKind.SKIP_CARD_REWARD,
                    option_id=f"reward-card:{idx1}",
                )
            if idx2 == 5:
                return Action(
                    ActionKind.TAKE_SINGING_BOWL,
                    option_id=f"reward-card:{idx1}",
                )
            return Action(ActionKind.CHOOSE_CARD_REWARD, subject_id=f"reward-card:{idx1}:{idx2}")
        if reward_type == 1:
            return Action(ActionKind.TAKE_REWARD, reward_id=f"reward-gold:{idx1}")
        if reward_type == 2:
            if raw["public_screen"].get("sapphire_key"):
                return Action(ActionKind.TAKE_BLUE_KEY, reward_id="reward-key:sapphire")
            return Action(ActionKind.TAKE_REWARD, reward_id="reward-key:emerald")
        if reward_type == 3:
            return Action(ActionKind.TAKE_REWARD, reward_id=f"reward-potion:{idx1}")
        if reward_type == 4:
            return Action(ActionKind.TAKE_REWARD, reward_id=f"reward-relic:{idx1}")
        if reward_type == 6:
            return Action(ActionKind.SKIP_REWARD)
        raise RuntimeError(f"unsupported combat reward action type: {reward_type}")
    if screen is ScreenType.BOSS_REWARD:
        return Action(
            ActionKind.CHOOSE_BOSS_RELIC if idx1 < 3 else ActionKind.SKIP_REWARD,
            subject_id=f"boss-relic:{idx1}" if idx1 < 3 else None,
        )
    if screen is ScreenType.CARD_REWARD:
        select_type = raw["public_screen"].get("select_type")
        kind = {
            "UPGRADE": ActionKind.UPGRADE_CARD,
            "REMOVE": ActionKind.REMOVE_CARD,
        }.get(select_type, ActionKind.SELECT_CARD)
        return Action(kind, subject_id=f"select-card:{idx1}")
    if screen is ScreenType.TREASURE:
        return Action(ActionKind.OPEN_CHEST if idx1 == 0 else ActionKind.PROCEED)
    if screen is ScreenType.REST:
        kinds = {
            0: ActionKind.REST, 1: ActionKind.CONFIRM, 2: ActionKind.RECALL,
            3: ActionKind.LIFT, 4: ActionKind.CONFIRM, 5: ActionKind.DIG,
            6: ActionKind.PROCEED,
        }
        return Action(kinds[idx1], option_id=f"rest-option:{idx1}")
    if screen is ScreenType.SHOP:
        if reward_type == 0:
            return Action(ActionKind.BUY_CARD, subject_id=f"shop-card:{idx1}")
        if reward_type == 3:
            return Action(ActionKind.BUY_POTION, subject_id=f"shop-potion:{idx1}")
        if reward_type == 4:
            return Action(ActionKind.BUY_RELIC, subject_id=f"shop-relic:{idx1}")
        if reward_type == 5:
            return Action(ActionKind.CONFIRM, option_id="shop-remove")
        return Action(ActionKind.LEAVE_SHOP)
    return Action(ActionKind.PROCEED, option_id=f"native-option:{ordinal}")


def _screen_entities(raw: Mapping[str, Any]) -> dict[str, tuple[Any, ...]]:
    result: dict[str, tuple[Any, ...]] = {
        "choice": (), "reward": (), "shop": (), "event": (), "rest": (), "boss": (),
    }
    screen = _screen_type(raw)
    info = raw["screen_info"]
    public_screen = raw["public_screen"]
    public_combat = raw.get("public_combat", {})
    actions = raw["legal_actions"]
    if screen is ScreenType.COMBAT and "choice" in public_combat:
        result["choice"] = tuple(
            _entity(
                f"CHOICE:{index}", option["content_id"],
                upgrades=int(option["upgrades"]), source=str(public_combat["choice"]["source"]),
            )
            for index, option in enumerate(public_combat["choice"]["options"])
        )
    elif screen in {ScreenType.NEOW, ScreenType.EVENT}:
        event_id = normalize_content_id(raw["public_run"]["current_event_id"])
        if event_id == "MATCH_AND_KEEP":
            result["event"] = tuple(
                _entity(
                    slot["instance_id"], slot["content_id"],
                    known=bool(slot["known"]), removed=bool(slot["removed"]),
                )
                for slot in public_screen["match_slots"]
            )
        else:
            result["event"] = tuple(
                _entity(f"event-option:{int(action['idx1'])}", f"{event_id}:OPTION:{int(action['idx1'])}")
                for action in actions
            )
    elif screen is ScreenType.REST:
        names = {0: "REST", 1: "SMITH", 2: "RECALL", 3: "LIFT", 4: "TOKE", 5: "DIG", 6: "PROCEED"}
        result["rest"] = tuple(
            _entity(f"rest-option:{int(action['idx1'])}", names[int(action["idx1"])])
            for action in actions
        )
    elif screen is ScreenType.BOSS_REWARD:
        result["boss"] = tuple(
            _entity(f"boss-relic:{index}", content_id)
            for index, content_id in enumerate(public_screen.get("boss_relics", ()))
        )
    elif screen is ScreenType.COMBAT_REWARD:
        rewards = public_screen
        entities: list[PublicEntity] = []
        for group_index, group in enumerate(rewards["card_rewards"]):
            for card_index, card in enumerate(group):
                entities.append(_entity(
                    f"reward-card:{group_index}:{card_index}", card["content_id"],
                    upgrades=int(card["upgrades"]),
                ))
        for index, amount in enumerate(rewards["gold"]):
            entities.append(_entity(f"reward-gold:{index}", "GOLD", amount=int(amount)))
        for index, content_id in enumerate(rewards.get("relics", ())):
            entities.append(_entity(f"reward-relic:{index}", content_id))
        for index, content_id in enumerate(rewards.get("potions", ())):
            entities.append(_entity(f"reward-potion:{index}", content_id))
        if rewards["emerald_key"]:
            entities.append(_entity("reward-key:emerald", "EMERALD_KEY"))
        if rewards["sapphire_key"]:
            entities.append(_entity("reward-key:sapphire", "SAPPHIRE_KEY"))
        result["reward"] = tuple(sorted(entities, key=lambda item: item.instance_id))
    elif screen is ScreenType.CARD_REWARD:
        result["reward"] = tuple(
            _entity(
                option["instance_id"], option["content_id"],
                upgrades=int(option["upgrades"]), deck_index=int(option["deck_index"]),
            )
            for option in public_screen.get("card_options", ())
        )
    elif screen is ScreenType.SHOP:
        shop = public_screen
        items: list[ShopItem] = []
        for index, card in enumerate(shop["cards"]):
            items.append(ShopItem(
                f"shop-card:{index}", card["content_id"], "CARD", int(shop["prices"][index]),
                int(shop["prices"][index]) < 0,
            ))
        for index, content_id in enumerate(shop["relics"]):
            items.append(ShopItem(
                f"shop-relic:{index}", content_id, "RELIC", int(shop["prices"][7 + index]),
                int(shop["prices"][7 + index]) < 0,
            ))
        for index, content_id in enumerate(shop["potions"]):
            items.append(ShopItem(
                f"shop-potion:{index}", content_id, "POTION", int(shop["prices"][10 + index]),
                int(shop["prices"][10 + index]) < 0,
            ))
        result["shop"] = tuple(items)
    return result
