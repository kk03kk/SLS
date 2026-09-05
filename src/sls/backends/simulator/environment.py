"""Canonical FullRun backend backed by the native simulator."""

from __future__ import annotations

import json
import math
from typing import Any, Mapping

from sls.content.card_features import (
    public_card_option_properties,
    public_card_properties,
)
from sls.content.energy import canonical_max_energy
from sls.content.normalize import (
    normalize_content_id,
    normalize_power_amount,
    normalize_power_id,
    normalize_relic_counter,
)
from sls.content.scope import (
    filter_policy_key_acquisitions,
    filter_policy_offers,
    filter_policy_shop,
)
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
from sls.contracts.continuation import continuation_simulator
from sls.curriculum import (
    IRONCLAD_A0_HEART,
    CurriculumProfile,
    EpisodeHorizon,
    TerminalOutcome,
    completed_act_between,
    evaluate_horizon,
)


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
        self._last_observation: Observation | None = None
        self._multi_selected: list[int] = []
        self._validation_action_queue_types: list[str] = []
        self._validation_choice_origin: str | None = None

    @property
    def raw_state(self) -> dict[str, Any]:
        if self._last_raw is None:
            raise RuntimeError("backend has not been reset")
        return self._last_raw

    def reset(self, seed: int) -> Decision:
        self._multi_selected.clear()
        self._validation_action_queue_types.clear()
        self._validation_choice_origin = None
        numeric_seed = int(seed)
        if not -(1 << 63) <= numeric_seed < (1 << 64):
            raise ValueError("seed must fit a signed or unsigned 64-bit integer")
        # Official run history renders the same 64 bits as a signed Java long,
        # while pybind accepts the native engine's unsigned seed domain.
        self._native.reset(numeric_seed & ((1 << 64) - 1), self.profile.ascension)
        return self._adapt(self._fold_non_heart_key_only_boundary(self._native.snapshot()))

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
        if self._last_raw is None or self._last_observation is None:
            raise RuntimeError("reset must be called before step")
        previous_observation = self._last_observation
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            bits = self._candidate_bits[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current decision boundary") from error
        previous_choice = str(
            ((self._last_raw or {}).get("public_combat") or {}).get("choice", {}).get("task") or ""
        )
        semantic_action = (
            Action.from_dict(json.loads(action)) if isinstance(action, str) else action
        )
        queued_type = _queued_potion_action_type(self._last_raw or {}, semantic_action)
        pending_queue = tuple(self._validation_action_queue_types)
        if bits < 0:
            self._multi_selected.append(-bits - 1)
            raw = self._native.snapshot()
        else:
            raw = self._native.step(bits)
        current_choice = str(
            (raw.get("public_combat") or {}).get("choice", {}).get("task") or ""
        )
        if bits >= 0 and not (
            previous_choice and current_choice == previous_choice
            and semantic_action.kind in {ActionKind.USE_POTION, ActionKind.DISCARD_POTION}
        ):
            self._multi_selected.clear()
        if previous_choice and current_choice == previous_choice and queued_type:
            self._validation_action_queue_types.append(queued_type)
        elif current_choice != previous_choice:
            self._validation_action_queue_types.clear()
        if (
            current_choice == "EXHAUST_MANY"
            and current_choice != previous_choice
            and "com.megacrit.cardcrawl.actions.common.ExhaustAction" in pending_queue
        ):
            self._validation_choice_origin = "ELIXIR_POTION"
        elif current_choice != previous_choice:
            self._validation_choice_origin = None
        if _is_completed_neow_reward(raw):
            # Stock closes the terminal Neow CardRewardScreen before exposing
            # the map.  The native engine represents that UI-only close as its
            # generic reward skip action, so fold it inside the same semantic
            # transition instead of leaking a spurious SKIP_REWARD boundary.
            fold = [
                candidate for candidate in raw.get("legal_actions", ())
                if candidate.get("domain") != "COMBAT"
                and int(candidate.get("reward_type", -1)) == 6
            ]
            if len(fold) != 1:
                raise RuntimeError(
                    "completed Neow reward must expose exactly one native UI fold"
                )
            raw = self._native.step(int(fold[0]["bits"]))
        if validation_evidence and "card_soul_cost_reset_count" in validation_evidence:
            self._native._reset_last_hand_card_costs_for_validation(
                int(validation_evidence["card_soul_cost_reset_count"]),
            )
            raw = self._native.snapshot()
        raw = self._fold_non_heart_key_only_boundary(raw)
        return self._transition_from_raw(previous_observation, raw)

    def _fold_non_heart_key_only_boundary(
        self, raw: Mapping[str, Any],
    ) -> Mapping[str, Any]:
        """Resolve a forced key-only UI boundary outside the Heart profile.

        Stock can leave Recall as the campfire's sole enabled option (for
        example, Coffee Dripper plus Fusion Hammer with no other campfire
        relic).  Non-Heart policies intentionally cannot choose keys.  The
        boundary is therefore presentation-only for that policy profile, but
        it still must execute the stock native action so key state, callbacks,
        and RNG remain canonical.
        """

        if self.profile.horizon is EpisodeHorizon.HEART:
            return raw
        actions = tuple(raw.get("legal_actions") or ())
        if not _is_forced_recall_boundary(raw, actions):
            return raw
        return self._native.step(int(actions[0]["bits"]))

    def _transition_from_raw(
        self, previous_observation: Observation, raw: Mapping[str, Any],
    ) -> Transition:
        """Build one policy transition while retaining native terminal truth."""

        next_decision = self._adapt(raw)
        terminal_outcome = _native_terminal_outcome(raw)
        reported_completed_act = int(raw["public_run"].get("completed_act", 0))
        completed_act = (
            reported_completed_act if reported_completed_act > 0
            else completed_act_between(previous_observation, next_decision.observation)
        )
        decision = evaluate_horizon(
            self.profile,
            next_decision.observation,
            act_completed=completed_act,
            terminal_outcome=terminal_outcome,
        )
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
            info={
                "reason": decision.reason,
                "success": decision.success,
                "terminal_outcome": (
                    terminal_outcome.value if terminal_outcome is not None else None
                ),
            },
        )

    def checkpoint(self) -> dict[str, Any]:
        """Return the exact native checkpoint; this object is not policy input."""

        if self._last_raw is None:
            raise RuntimeError("reset must be called before checkpoint")
        checkpoint = self._native.snapshot()
        if self._multi_selected:
            checkpoint["_policy_multi_selection"] = list(self._multi_selected)
        if self._validation_action_queue_types:
            checkpoint["_validation_action_queue_types"] = list(
                self._validation_action_queue_types
            )
        if self._validation_choice_origin:
            checkpoint["_validation_choice_origin"] = self._validation_choice_origin
        return checkpoint

    def load_checkpoint(self, state: Mapping[str, Any]) -> Decision:
        checkpoint = dict(state)
        self._multi_selected = [
            int(value) for value in checkpoint.pop("_policy_multi_selection", ())
        ]
        if len(self._multi_selected) != len(set(self._multi_selected)):
            raise ValueError("checkpoint contains duplicate pending selections")
        self._validation_action_queue_types = list(
            checkpoint.pop("_validation_action_queue_types", ())
        )
        self._validation_choice_origin = checkpoint.pop(
            "_validation_choice_origin", None
        )
        self._native.load_state(checkpoint)
        raw = self._fold_non_heart_key_only_boundary(self._native.snapshot())
        return self._adapt(raw)

    def _adapt(self, raw: Mapping[str, Any]) -> Decision:
        raw = dict(raw)
        raw["legal_actions"] = _effective_combat_actions(raw, self._multi_selected)
        if self._multi_selected:
            raw["_policy_multi_selection"] = list(self._multi_selected)
        if self._validation_action_queue_types:
            raw["_validation_action_queue_types"] = list(
                self._validation_action_queue_types
            )
        if self._validation_choice_origin:
            raw["_validation_choice_origin"] = self._validation_choice_origin
        self._last_raw = raw
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
                properties=public_card_properties(str(card["content_id"]), card),
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
        hand_choice_sources = _hand_choice_sources(raw)
        if combat and (hand_choice_sources or _is_incremental_hand_selection(raw)):
            card_zones["HAND"] = _combat_cards(
                [combat.get("hand", ())[index] for index in hand_choice_sources], "HAND",
            )
        enemies = tuple(
            Enemy(
                f"MONSTER:{index}", str(monster["content_id"]),
                int(monster["current_hp"]), int(monster["max_hp"]), int(monster["block"]),
                "UNKNOWN" if bool(monster["is_gone"]) else str(monster["intent"]),
                0 if bool(monster["is_gone"]) else int(monster["intent_damage"]),
                0 if bool(monster["is_gone"]) else int(monster["intent_hits"]),
                (("is_gone", bool(monster["is_gone"])),),
            )
            for index, monster in enumerate(combat.get("monsters", ()) if combat else ())
        )
        powers = _powers(combat["player"].get("powers", ()) if combat else (), "PLAYER_POWER")
        for index, monster in enumerate(combat.get("monsters", ()) if combat else ()):
            if not bool(monster.get("is_gone", False)) and int(monster.get("current_hp", 0)) > 0:
                powers += _powers(monster.get("powers", ()), f"MONSTER:{index}:POWER")

        actions, candidate_bits = _semantic_actions(raw, card_zones["HAND"])
        options = _screen_entities(raw)
        if screen is ScreenType.SHOP:
            shop, actions, candidate_bits = filter_policy_shop(
                options["shop"], actions, candidate_bits,
            )
            options["shop"] = shop
        elif screen is ScreenType.COMBAT_REWARD:
            rewards, actions, candidate_bits = filter_policy_offers(
                options["reward"], actions, candidate_bits,
            )
            options["reward"] = rewards
        reward_items = options["reward"] if screen is ScreenType.COMBAT_REWARD else ()
        reward_items, actions, candidate_bits = filter_policy_key_acquisitions(
            reward_items,
            actions,
            candidate_bits,
            allow_keys=self.profile.horizon is EpisodeHorizon.HEART,
        )
        if screen is ScreenType.COMBAT_REWARD:
            options["reward"] = reward_items
        self._candidate_bits = candidate_bits
        map_nodes = tuple(
            MapNode(
                str(node["node_id"]), int(node["x"]), int(node["y"]),
                (
                    "BURNING_ELITE"
                    if node["burning"] and not bool(player_state["green_key"])
                    else str(node["room_type"])
                ),
                bool(node["reachable"]), tuple(
                    "map:boss" if str(item).rsplit(":", 1)[-1] == "15" else str(item)
                    for item in node["outgoing_node_ids"]
                ),
            )
            for node in raw["public_map"]
        )
        if any(action.node_id == "map:boss" for action in actions):
            map_nodes += (MapNode("map:boss", 0, 15, "BOSS", True),)
        if screen is ScreenType.GAME_OVER:
            card_zones = {zone: () for zone in card_zones}
            enemies = ()
            powers = ()
        observation = Observation(
            player=Player(
                "IRONCLAD", int(visible_player["current_hp"]), int(visible_player["max_hp"]),
                int(visible_player.get("block", 0)), int(visible_player.get("energy", 0)),
                (
                    canonical_max_energy(
                        (str(relic["content_id"]) for relic in inventory["relics"]),
                        combat_value=(
                            int(visible_player["max_energy"])
                            if visible_player.get("max_energy") is not None
                            else None
                        ),
                        in_combat=True,
                    )
                    if combat else 3
                ),
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
                    counter=normalize_relic_counter(value["counter"]),
                )
                for index, value in enumerate(inventory["relics"])
            ),
            potions=tuple(
                _entity(f"POTION:{int(value['slot'])}", value["content_id"], slot=int(value["slot"]))
                for value in inventory["potions"]
                if value["content_id"] not in {"INVALID", "EMPTY_POTION_SLOT"}
            ),
            map_nodes=map_nodes,
            choice_options=options["choice"],
            selected_cards=tuple(
                _entity(
                    f"SELECTED:{order}", option["content_id"],
                    **dict(public_card_option_properties(option["content_id"], option)),
                    source=str(combat["choice"]["source"]), selected=True, selected_order=order,
                )
                for order, index in enumerate(self._multi_selected)
                for option in (combat["choice"]["options"][index],)
            ),
            reward_options=options["reward"],
            shop_items=options["shop"],
            event_options=options["event"],
            rest_options=options["rest"],
            boss_relic_options=options["boss"],
            public_context=(
                (("turn", int(combat["turn"])),)
                if combat and screen is not ScreenType.GAME_OVER else (
                    (("attempts_remaining", int(raw["public_screen"]["attempts_remaining"])),)
                    if screen is ScreenType.EVENT and raw["public_screen"].get("match_slots") else ()
                )
            ),
        )
        decision = Decision(
            observation=observation,
            actions=actions,
            terminal=screen is ScreenType.GAME_OVER,
        )
        self._last_observation = observation
        return decision

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
                public_card_properties(str(value["content_id"]), value),
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
            tuple(sorted(public_card_properties(str(card["content_id"]), card)
                         + ((("order_is_visible", True),)
                            if zone == "DRAW" and draw_order_visible else ()))),
        )
        for index, card in enumerate(ordered)
    )


def _powers(values: Any, prefix: str) -> tuple[PublicEntity, ...]:
    visible = sorted([
        value for value in values
        if normalize_power_id(value["id"]) not in {"ASLEEP", "MINION_LEADER"}
    ], key=lambda value: (
        normalize_power_id(value["id"]), int(value["amount"]),
    ))
    return tuple(
        PublicEntity(
            f"{prefix}:{index}", normalize_power_id(value["id"]),
            (("amount", normalize_power_amount(value["id"], value["amount"])),),
            owner_id="player" if prefix == "PLAYER_POWER" else prefix.removesuffix(":POWER"),
        )
        for index, value in enumerate(visible)
    )


def _is_neow_card_reward(raw: Mapping[str, Any]) -> bool:
    public = raw.get("public_run") or {}
    screen = raw.get("public_screen") or {}
    return bool(
        int(public.get("screen_state", 0) or 0) == 2
        and str(public.get("current_event_id") or "").upper() == "NEOW"
        and len(screen.get("card_rewards") or ()) == 1
        and not any(screen.get(key) for key in ("gold", "relics", "potions"))
    )


def _is_completed_neow_reward(raw: Mapping[str, Any]) -> bool:
    public = raw.get("public_run") or {}
    screen = raw.get("public_screen") or {}
    info = raw.get("screen_info") or {}
    return bool(
        int(public.get("screen_state", 0) or 0) == 2
        and str(public.get("current_event_id") or "").upper() == "NEOW"
        and str(info.get("continuation") or "").lower() == "map"
        and not any(screen.get(key) for key in (
            "card_rewards", "gold", "relics", "potions", "emerald_key", "sapphire_key",
        ))
    )


def _screen_type(raw: Mapping[str, Any]) -> ScreenType:
    public = raw["public_run"]
    if int(public["outcome"]) != 1:
        return ScreenType.GAME_OVER
    screen = int(public["screen_state"])
    if _is_neow_card_reward(raw):
        return ScreenType.CARD_REWARD
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


def _native_terminal_outcome(raw: Mapping[str, Any]) -> TerminalOutcome | None:
    outcome = int(raw["public_run"]["outcome"])
    if outcome == 0:
        return TerminalOutcome.PLAYER_LOSS
    if outcome == 1:
        return None
    if outcome == 2:
        return TerminalOutcome.PLAYER_VICTORY
    raise ValueError(f"unsupported native game outcome: {outcome}")


def _is_forced_recall_boundary(
    raw: Mapping[str, Any], actions: tuple[Mapping[str, Any], ...],
) -> bool:
    """Return whether stock exposes Recall as the only possible campfire action."""

    return bool(
        int((raw.get("public_run") or {}).get("outcome", 1)) == 1
        and int((raw.get("public_run") or {}).get("screen_state", 0)) == 7
        and len(actions) == 1
        and actions[0].get("domain") != "COMBAT"
        and not bool(actions[0].get("potion"))
        and int(actions[0].get("idx1", -1)) == 2
    )


def _semantic_actions(
    raw: Mapping[str, Any], hand: tuple[Card, ...]
) -> tuple[tuple[Action, ...], dict[str, int]]:
    screen = _screen_type(raw)
    progress = raw["progress_state"]
    semantic: list[Action] = []
    mapping: dict[str, int] = {}
    native_target_to_public: dict[int, int] = {}
    for public_index, monster in enumerate(
        (raw.get("public_combat") or {}).get("monsters") or ()
    ):
        instance_id = str(monster.get("instance_id") or "")
        if instance_id.startswith("monster:") and instance_id[8:].isdigit():
            native_target_to_public[int(instance_id[8:])] = public_index
    for ordinal, native in enumerate(raw["legal_actions"]):
        if _is_neow_card_reward(raw) and int(native["reward_type"]) == 6:
            # The native Rewards container has a generic skip-all action that
            # stock's Neow CardRewardScreen does not expose.
            continue
        if (
            screen is ScreenType.COMBAT_REWARD
            and native.get("domain") != "COMBAT"
            and int(native.get("reward_type", -1)) == 0
            and int(native.get("idx2", -1)) == 6
        ):
            # The public adapter flattens the stock parent/child reward UI.
            # Closing only the child card popup returns to an identical
            # policy boundary and is therefore not a semantic action.
            continue
        if native.get("domain") == "COMBAT":
            action_type = int(native["action_type"])
            source = int(native["source_index"])
            target = int(native["target_index"])
            public_target = native_target_to_public.get(target, target)
            if action_type == 0:
                action = Action(
                    ActionKind.PLAY_CARD,
                    subject_id=hand[source].instance_id,
                    target_id=(
                        f"MONSTER:{public_target}" if native.get("requires_target") else None
                    ),
                )
            elif action_type == 1:
                kind = ActionKind.DISCARD_POTION if target == 6 else ActionKind.USE_POTION
                action = Action(
                    kind, subject_id=f"POTION:{source}",
                    target_id=(
                        f"MONSTER:{public_target}"
                        if kind is ActionKind.USE_POTION and native.get("requires_target")
                        else None
                    ),
                )
            elif action_type == 2:
                action = Action(
                    ActionKind.SELECT_CARD,
                    subject_id=f"CHOICE:{source}",
                )
            elif action_type == 3:
                action = Action(ActionKind.CONFIRM, option_id="combat-selection")
            else:
                action = Action(ActionKind.END_TURN)
        else:
            action = _run_action(raw, native, ordinal, screen, progress)
        if action.candidate_id in mapping:
            raise RuntimeError(f"native legal actions collapse to one semantic identity: {action.candidate_id}")
        semantic.append(action)
        mapping[action.candidate_id] = int(native["bits"])
    if screen is ScreenType.COMBAT_REWARD:
        def reward_order(action: Action) -> tuple[int, str]:
            identity = action.reward_id or action.subject_id or action.option_id or ""
            if action.kind is ActionKind.TAKE_REWARD:
                prefix_order = {
                    "reward-gold": 0, "reward-relic": 1,
                    "reward-key": 2, "reward-potion": 3,
                }
                return next(
                    ((value, identity) for prefix, value in prefix_order.items()
                     if identity.startswith(prefix)),
                    (3, identity),
                )
            return ({
                ActionKind.TAKE_BLUE_KEY: 2,
                ActionKind.CHOOSE_CARD_REWARD: 4,
                ActionKind.TAKE_SINGING_BOWL: 5,
                ActionKind.SKIP_CARD_REWARD: 6,
                ActionKind.SKIP_REWARD: 7,
            }.get(action.kind, 6), identity)
        semantic.sort(key=reward_order)
    return tuple(semantic), mapping


def _run_action(
    raw: Mapping[str, Any], native: Mapping[str, Any], ordinal: int,
    screen: ScreenType, progress: Mapping[str, Any],
) -> Action:
    idx1, idx2 = int(native["idx1"]), int(native["idx2"])
    reward_type = int(native["reward_type"])
    if native.get("potion"):
        return Action(
            ActionKind.DISCARD_POTION
            if native.get("potion_discard") else ActionKind.USE_POTION,
            subject_id=f"POTION:{idx1}",
        )
    if screen in {ScreenType.NEOW, ScreenType.EVENT}:
        if raw["public_run"]["current_event_id"] == "Match and Keep":
            return Action(
                ActionKind.CHOOSE_EVENT_OPTION,
                option_id=f"match-pair:{idx1}:{idx2}",
                subject_id=f"match-slot:{idx1}",
                target_id=f"match-slot:{idx2}",
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
        if _is_neow_card_reward(raw):
            if reward_type != 0:
                raise RuntimeError(f"unsupported Neow card reward action type: {reward_type}")
            if idx2 == 6:
                return Action(ActionKind.SKIP_CARD_REWARD, option_id="reward-card:0")
            return Action(ActionKind.SELECT_CARD, subject_id=f"select-card:{idx2}")
        select_type = raw["public_screen"].get("select_type")
        kind = {
            "UPGRADE": ActionKind.UPGRADE_CARD,
            "REMOVE": ActionKind.REMOVE_CARD,
            # Stock Bonfire's grid selection immediately purges the offered
            # card; expose the semantic operation, not the generic native
            # card-selection container.
            "BONFIRE_SPIRITS": ActionKind.REMOVE_CARD,
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
        prices = raw["public_screen"]["prices"]
        if reward_type == 0:
            visible = sum(int(price) >= 0 for price in prices[:idx1])
            return Action(ActionKind.BUY_CARD, subject_id=f"shop-card:{visible}")
        if reward_type == 3:
            visible = sum(int(price) >= 0 for price in prices[10:10 + idx1])
            return Action(ActionKind.BUY_POTION, subject_id=f"shop-potion:{visible}")
        if reward_type == 4:
            visible = sum(int(price) >= 0 for price in prices[7:7 + idx1])
            return Action(ActionKind.BUY_RELIC, subject_id=f"shop-relic:{visible}")
        if reward_type == 5:
            return Action(ActionKind.CONFIRM, option_id="shop-remove")
        return Action(ActionKind.LEAVE_SHOP)
    return Action(ActionKind.PROCEED, option_id=f"native-option:{ordinal}")


def _screen_entities(raw: Mapping[str, Any]) -> dict[str, tuple[Any, ...]]:
    result: dict[str, tuple[Any, ...]] = {
        "choice": (), "reward": (), "shop": (), "event": (), "rest": (), "boss": (),
    }
    screen = _screen_type(raw)
    public_screen = raw["public_screen"]
    public_combat = raw.get("public_combat", {})
    actions = raw["legal_actions"]
    if screen is ScreenType.COMBAT and "choice" in public_combat:
        choice = public_combat["choice"]
        task = str(choice.get("task") or "UNKNOWN").upper()
        indexed_options = [
            (int(option.get("choice_index", index)), option)
            for index, option in enumerate(choice["options"])
        ]
        option_indices = [index for index, _ in indexed_options]
        if any(index < 0 for index in option_indices) or len(option_indices) != len(set(option_indices)):
            raise ValueError(f"native {task} choice exposes invalid option indices")
        hand_choice_sources = _hand_choice_sources(raw)
        if hand_choice_sources or _is_incremental_hand_selection(raw):
            wanted = set(hand_choice_sources)
            indexed_options = [
                (index, option) for index, option in indexed_options if index in wanted
            ]
        entities = [
            _entity(
                f"CHOICE:{index}", option["content_id"],
                **dict(public_card_option_properties(option["content_id"], option)),
                source=str(choice["source"]),
            )
            for index, option in indexed_options
        ]
        for control in choice.get("controls", ()):
            control_index = int(control["choice_index"])
            if task != "CODEX" or str(control.get("kind") or "").upper() != "SKIP":
                raise ValueError(f"native {task} choice exposes an unsupported control")
            entities.append(_entity(
                f"CHOICE:{control_index}", "OPTION",
                option_ordinal=control_index, source=str(choice["source"]),
            ))
        entity_ids = [entity.instance_id for entity in entities]
        if len(entity_ids) != len(set(entity_ids)):
            raise ValueError(f"native {task} choice exposes duplicate policy identities")
        legal_choice_ids = {
            f"CHOICE:{int(action['source_index'])}"
            for action in actions
            if action.get("domain") == "COMBAT"
            and int(action.get("action_type", -1)) == 2
        }
        missing = legal_choice_ids - set(entity_ids)
        if missing:
            raise ValueError(
                f"native {task} choice actions lack public entities: {sorted(missing)}"
            )
        result["choice"] = tuple(entities)
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
                for action in actions if not action.get("potion")
            )
    elif screen is ScreenType.REST:
        names = {0: "REST", 1: "SMITH", 2: "RECALL", 3: "LIFT", 4: "TOKE", 5: "DIG", 6: "PROCEED"}
        result["rest"] = tuple(
            _entity(f"rest-option:{int(action['idx1'])}", names[int(action["idx1"])])
            for action in actions if not action.get("potion")
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
                    **dict(public_card_option_properties(card["content_id"], card)),
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
        if _is_neow_card_reward(raw):
            result["reward"] = tuple(
                _entity(
                    f"select-card:{index}", option["content_id"],
                    **dict(public_card_option_properties(option["content_id"], option)),
                )
                for index, option in enumerate(public_screen["card_rewards"][0])
            )
        else:
            result["reward"] = tuple(
                _entity(
                    option["instance_id"], option["content_id"],
                    **dict(public_card_option_properties(option["content_id"], option)),
                    deck_index=int(option["deck_index"]),
                )
                for option in public_screen.get("card_options", ())
            )
    elif screen is ScreenType.SHOP:
        shop = public_screen
        items: list[ShopItem] = []
        visible_index = 0
        for index, card in enumerate(shop["cards"]):
            if int(shop["prices"][index]) < 0:
                continue
            items.append(ShopItem(
                f"shop-card:{visible_index}", card["content_id"], "CARD",
                int(shop["prices"][index]), False,
                public_card_option_properties(card["content_id"], card),
            ))
            visible_index += 1
        visible_index = 0
        for index, content_id in enumerate(shop["relics"]):
            if int(shop["prices"][7 + index]) < 0:
                continue
            items.append(ShopItem(
                f"shop-relic:{visible_index}", content_id, "RELIC",
                int(shop["prices"][7 + index]), False,
            ))
            visible_index += 1
        visible_index = 0
        for index, content_id in enumerate(shop["potions"]):
            if int(shop["prices"][10 + index]) < 0:
                continue
            items.append(ShopItem(
                f"shop-potion:{visible_index}", content_id, "POTION",
                int(shop["prices"][10 + index]), False,
            ))
            visible_index += 1
        result["shop"] = tuple(items)
    return result


def _hand_choice_sources(raw: Mapping[str, Any]) -> tuple[int, ...]:
    combat = raw.get("public_combat") or {}
    choice = combat.get("choice") or {}
    if str(choice.get("source") or "").upper() != "HAND":
        return ()
    return tuple(sorted({
        int(action["source_index"])
        for action in raw.get("legal_actions") or ()
        if action.get("domain") == "COMBAT"
        and int(action.get("action_type", -1)) == 2
    }))


def _is_incremental_hand_selection(raw: Mapping[str, Any]) -> bool:
    choice = (raw.get("public_combat") or {}).get("choice") or {}
    return str(choice.get("source") or "").upper() == "HAND" and _is_incremental_selection(raw)


def _is_incremental_selection(raw: Mapping[str, Any]) -> bool:
    choice = (raw.get("public_combat") or {}).get("choice") or {}
    task = str(choice.get("task") or "").upper()
    if task in {"EXHAUST_MANY", "GAMBLE", "RETAIN_CARDS"}:
        return True
    internal = _selection_contract(raw)
    return (
        task == "FORETHOUGHT" and bool(internal.get("can_pick_any_number"))
        or task == "LIQUID_MEMORIES_POTION" and int(internal.get("pick_count", 1)) > 1
    )


def _selection_contract(raw: Mapping[str, Any]) -> Mapping[str, Any]:
    checkpoint = raw.get("combat_checkpoint") or {}
    game = checkpoint.get("game_state") or {}
    return ((game.get("combat_state") or {}).get("_internal") or {}).get("choice") or {}


def _effective_combat_actions(
    raw: Mapping[str, Any], selected: list[int],
) -> list[dict[str, Any]]:
    """Expose stock-like incremental candidates for optional hand selection.

    The native search action commits the whole selection. Policy/Original boundaries
    select one card at a time and then confirm, so negative private candidate
    codes accumulate ordered indices in :class:`SimulatorBackend` without changing
    native combat state.  The final confirm executes the native atomic action.
    """

    legal = [dict(value) for value in raw.get("legal_actions") or ()]
    combat = raw.get("public_combat") or {}
    choice = combat.get("choice") or {}
    task = str(choice.get("task") or "").upper()
    if not _is_incremental_selection(raw):
        return legal
    internal = _selection_contract(raw)
    limit = int(internal.get("pick_count", len(choice.get("options") or ())) or 0)
    option_count = len(choice.get("options") or ())
    if any(index < 0 or index >= option_count for index in selected):
        raise ValueError("checkpoint contains an invalid pending multi-selection")
    if len(selected) > limit:
        raise ValueError("checkpoint exceeds the pending selection limit")
    base = 3 << 29
    result = []
    if len(selected) < limit:
        for index in range(option_count):
            if index in selected:
                continue
            result.append({
                "bits": -(index + 1), "action_type": 2,
                "source_index": index, "target_index": 0,
                "domain": "COMBAT", "requires_target": False,
            })
    can_confirm = (
        task in {"EXHAUST_MANY", "GAMBLE"}
        or bool(internal.get("can_pick_zero") or internal.get("can_pick_any_number"))
        or len(selected) >= limit
    )
    if can_confirm:
        if task == "LIQUID_MEMORIES_POTION":
            if len(selected) != 2:
                raise ValueError("Sacred Bark Liquid Memories requires exactly two cards")
            first, second = selected
            # Discard piles may exceed the ten-card hand bitset. Encode the
            # two pile indices in the native action's source/target slots.
            bits = base | first | (second << 16)
        else:
            # Partial permutation rank fits within 24 bits for ten hand slots.
            # Keep the native legacy bitset decoder for historical replay, but
            # mark new ordered selections with bit 28 and a four-bit count.
            remaining = list(range(10))
            rank = 0
            for position, index in enumerate(selected):
                digit = remaining.index(index)
                rank += digit * math.factorial(9 - position)
                remaining.pop(digit)
            bits = base | (1 << 28) | (len(selected) << 24) | rank
        result.append({
            "bits": bits, "action_type": 3,
            "source_index": 0, "target_index": 0,
            "domain": "COMBAT", "requires_target": False,
            "selected_indices": list(selected),
        })
    # Replacing native subset actions must not hide potion actions. Stock
    # consumes/discards the potion while keeping the current selection open.
    result.extend(value for value in legal if value.get("domain") == "COMBAT"
                  and int(value.get("action_type", -1)) == 1)
    return result


def _queued_potion_action_type(raw: Mapping[str, Any], action: Action) -> str | None:
    if action.kind is not ActionKind.USE_POTION:
        return None
    if not (raw.get("public_combat") or {}).get("choice"):
        return None
    try:
        slot = int(str(action.subject_id).split(":", 1)[1])
        potion = (raw.get("public_inventory") or {}).get("potions", ())[slot]
    except (IndexError, TypeError, ValueError):
        return None
    return {
        "ELIXIR_POTION": "com.megacrit.cardcrawl.actions.common.ExhaustAction",
    }.get(str(potion.get("content_id") or potion.get("id") or ""))
