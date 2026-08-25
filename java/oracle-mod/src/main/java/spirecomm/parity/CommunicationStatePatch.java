package spirecomm.parity;

import com.autoplay.gson.Gson;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.Soul;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import com.megacrit.cardcrawl.events.shrines.Designer;
import com.megacrit.cardcrawl.events.city.Vampires;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.EnemyMoveInfo;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import basemod.ReflectionHacks;
import communicationmod.GameStateConverter;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public final class CommunicationStatePatch {
    public static final String INSTRUMENTATION_SCHEMA = "spirecomm-parity-v10";
    private static final Method CALCULATE_DAMAGE = privateCalculateDamage();
    private static AbstractEvent matchEvent;
    private static final ArrayList<String> matchOrder = new ArrayList<String>();
    private static final Map<String, String> knownMatchCards =
        new HashMap<String, String>();

    private static ArrayList<Map<String, Object>> matchSlots(AbstractEvent event) {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (!(event instanceof GremlinMatchGame)
                || !"PLAY".equals(matchPhase((GremlinMatchGame) event))) {
            if (!(event instanceof GremlinMatchGame)) {
                matchEvent = null;
                matchOrder.clear();
                knownMatchCards.clear();
            }
            return result;
        }
        CardGroup cards = ReflectionHacks.getPrivate(
            event, GremlinMatchGame.class, "cards"
        );
        if (matchEvent != event) {
            matchEvent = event;
            matchOrder.clear();
            knownMatchCards.clear();
            for (AbstractCard card : cards.group) {
                matchOrder.add(card.uuid.toString());
            }
        }
        Map<String, AbstractCard> current = new HashMap<String, AbstractCard>();
        for (AbstractCard card : cards.group) {
            String uuid = card.uuid.toString();
            current.put(uuid, card);
            if (!card.isFlipped) {
                knownMatchCards.put(uuid, card.cardID);
            }
        }
        for (int index = 0; index < matchOrder.size(); ++index) {
            String uuid = matchOrder.get(index);
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("slot", index);
            value.put("content_id", knownMatchCards.get(uuid));
            value.put("known", knownMatchCards.containsKey(uuid));
            value.put("removed", !current.containsKey(uuid));
            AbstractCard currentCard = current.get(uuid);
            if (currentCard != null) {
                value.put("click_x", Math.round(currentCard.hb.cX / Settings.xScale));
                value.put(
                    "click_y",
                    Math.round((Settings.HEIGHT - currentCard.hb.cY) / Settings.yScale)
                );
            }
            result.add(value);
        }
        return result;
    }

    static String matchPhase(GremlinMatchGame event) {
        Object phase = ReflectionHacks.getPrivate(
            event, GremlinMatchGame.class, "screen"
        );
        return String.valueOf(phase);
    }

    static AbstractCard matchCardForSlot(GremlinMatchGame event, int slot) {
        if (event != matchEvent || slot < 0 || slot >= matchOrder.size()) {
            return null;
        }
        String uuid = matchOrder.get(slot);
        CardGroup cards = ReflectionHacks.getPrivate(
            event, GremlinMatchGame.class, "cards"
        );
        for (AbstractCard card : cards.group) {
            if (uuid.equals(card.uuid.toString())) {
                return card;
            }
        }
        return null;
    }

    private static String eventId(AbstractEvent event) {
        if (event == null) {
            return null;
        }
        try {
            Field field = event.getClass().getField("ID");
            Object value = field.get(null);
            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception ignored) {
            // A modded event without a public stock-style ID remains
            // diagnosable by class name instead of breaking capture.
        }
        return event.getClass().getName();
    }

    private static Method privateCalculateDamage() {
        try {
            Method method = AbstractMonster.class.getDeclaredMethod("calculateDamage", int.class);
            method.setAccessible(true);
            return method;
        } catch (Exception error) {
            throw new RuntimeException("cannot access stock monster damage calculation", error);
        }
    }

    private static int adjustedIntentDamage(AbstractMonster monster, EnemyMoveInfo move) {
        if (move == null || move.baseDamage < 0) {
            return -1;
        }
        int previous = monster.getIntentDmg();
        if (previous >= 0) {
            return previous;
        }
        try {
            CALCULATE_DAMAGE.invoke(monster, move.baseDamage);
            return monster.getIntentDmg();
        } catch (Exception error) {
            throw new RuntimeException("stock monster damage calculation failed", error);
        } finally {
            ReflectionHacks.setPrivate(monster, AbstractMonster.class, "intentDmg", previous);
        }
    }

    private static Object eventPhase(AbstractEvent event) {
        if (event == null) {
            return null;
        }
        for (String name : new String[] {"screenNum", "screen", "curScreen"}) {
            for (Class<?> type = event.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    Object value = field.get(event);
                    if (value instanceof Number || value instanceof Enum<?>) {
                        return value.toString();
                    }
                } catch (NoSuchFieldException ignored) {
                    // Continue through the registered stock event hierarchy.
                } catch (Exception error) {
                    throw new RuntimeException("cannot inspect stock event phase", error);
                }
            }
        }
        return "UNKNOWN";
    }

    private static boolean pendingBottleSelection() {
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.GRID
                || AbstractDungeon.player == null) {
            return false;
        }
        for (AbstractRelic relic : AbstractDungeon.player.relics) {
            if (!("Bottled Flame".equals(relic.relicId)
                    || "Bottled Lightning".equals(relic.relicId)
                    || "Bottled Tornado".equals(relic.relicId))) {
                continue;
            }
            try {
                Field selected = relic.getClass().getDeclaredField("cardSelected");
                selected.setAccessible(true);
                if (!selected.getBoolean(relic)) {
                    return true;
                }
            } catch (Exception error) {
                throw new RuntimeException(
                    "cannot inspect registered bottle relic selection state", error
                );
            }
        }
        return false;
    }

    public static String inject(String json) {
        if (json == null || json.length() < 2 || json.charAt(json.length() - 1) != '}') {
            return json;
        }
        // CommunicationMod also serializes the main menu. Before a run starts
        // there is no game seed or dungeon RNG state to expose.
        if (Settings.seed == null) {
            return json;
        }
        Map<String, Object> rng = new LinkedHashMap<String, Object>();
        rng.put("ai", ParityRng.state(AbstractDungeon.aiRng));
        rng.put("card_random", ParityRng.state(AbstractDungeon.cardRandomRng));
        rng.put("card", ParityRng.state(AbstractDungeon.cardRng));
        rng.put("event", ParityRng.state(AbstractDungeon.eventRng));
        rng.put("math_util", ParityRng.state(ParityRng.requireMathRng()));
        rng.put("merchant", ParityRng.state(AbstractDungeon.merchantRng));
        rng.put("misc", ParityRng.state(AbstractDungeon.miscRng));
        rng.put("monster_hp", ParityRng.state(AbstractDungeon.monsterHpRng));
        rng.put("monster", ParityRng.state(AbstractDungeon.monsterRng));
        rng.put("neow", ParityRng.state(NeowEvent.rng));
        rng.put("potion", ParityRng.state(AbstractDungeon.potionRng));
        rng.put("relic", ParityRng.state(AbstractDungeon.relicRng));
        rng.put("shuffle", ParityRng.state(AbstractDungeon.shuffleRng));
        rng.put("treasure", ParityRng.state(AbstractDungeon.treasureRng));
        Map<String, Object> run = new LinkedHashMap<String, Object>();
        run.put("ruby_key", Settings.hasRubyKey);
        run.put("emerald_key", Settings.hasEmeraldKey);
        run.put("sapphire_key", Settings.hasSapphireKey);
        run.put("burning_elite_x", null);
        run.put("burning_elite_y", null);
        if (AbstractDungeon.getCurrMapNode() != null) {
            run.put("current_map_x", AbstractDungeon.getCurrMapNode().x);
            run.put("current_map_y", AbstractDungeon.getCurrMapNode().y);
        }
        if (AbstractDungeon.map != null) {
            for (ArrayList<MapRoomNode> row : AbstractDungeon.map) {
                for (MapRoomNode node : row) {
                    if (node.hasEmeraldKey) {
                        run.put("burning_elite_x", node.x);
                        run.put("burning_elite_y", node.y);
                    }
                }
            }
        }
        Map<String, Object> continuation = new LinkedHashMap<String, Object>();
        continuation.put("room_class", AbstractDungeon.getCurrRoom() == null ? null
            : AbstractDungeon.getCurrRoom().getClass().getName());
        continuation.put("screen", AbstractDungeon.screen == null ? null : AbstractDungeon.screen.name());
        continuation.put("event_id", AbstractDungeon.getCurrRoom() == null
            || AbstractDungeon.getCurrRoom().event == null ? null
            : eventId(AbstractDungeon.getCurrRoom().event));
        continuation.put("event_phase", AbstractDungeon.getCurrRoom() == null
            ? null : eventPhase(AbstractDungeon.getCurrRoom().event));
        continuation.put("action_phase", AbstractDungeon.actionManager == null ? null
            : AbstractDungeon.actionManager.phase.name());
        continuation.put("combat_turn", com.megacrit.cardcrawl.actions.GameActionManager.turn);
        continuation.put("card_selection_source", null);
        continuation.put("card_selection_task", null);
        continuation.put("card_selection_count", 0);
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD
                && AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            continuation.put("card_selection_source", "GENERATED");
            continuation.put("card_selection_task", "DISCOVERY");
            continuation.put("card_selection_count", 1);
        }
        if (pendingBottleSelection()) {
            continuation.put("card_selection_source", "MASTER_DECK");
            continuation.put("card_selection_task", "BOTTLE");
        }
        continuation.put("post_combat", AbstractDungeon.getCurrRoom() != null
            && AbstractDungeon.getCurrRoom().isBattleOver);
        continuation.put("loading_post_combat", AbstractDungeon.loading_post_combat);
        continuation.put("ui_boundary_folded", false);
        continuation.put("continuation_kind", AbstractDungeon.screen == null
            ? "NONE" : AbstractDungeon.screen.name());
        ArrayList<String> actionTypes = new ArrayList<String>();
        ArrayList<String> cardQueueTypes = new ArrayList<String>();
        if (AbstractDungeon.actionManager != null) {
            for (AbstractGameAction action : AbstractDungeon.actionManager.actions) {
                actionTypes.add(action.getClass().getName());
            }
            for (CardQueueItem item : AbstractDungeon.actionManager.cardQueue) {
                cardQueueTypes.add(item.card == null ? "null" : item.card.getClass().getName());
            }
        }
        continuation.put("action_queue_types", actionTypes);
        continuation.put("card_queue_types", cardQueueTypes);
        ArrayList<Map<String, Object>> activeCardSouls =
            new ArrayList<Map<String, Object>>();
        if (AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().souls != null) {
            ArrayList<Soul> souls = ReflectionHacks.getPrivate(
                AbstractDungeon.getCurrRoom().souls, SoulGroup.class, "souls"
            );
            for (Soul soul : souls) {
                if (soul.isReadyForReuse || soul.card == null) {
                    continue;
                }
                Map<String, Object> value = new LinkedHashMap<String, Object>();
                value.put("card_uuid", soul.card.uuid == null
                    ? null : soul.card.uuid.toString());
                value.put("card_id", soul.card.cardID);
                value.put("destination", soul.group == null ? null : soul.group.type.name());
                value.put("cost_for_turn", soul.card.costForTurn);
                value.put("done", soul.isDone);
                activeCardSouls.add(value);
            }
        }
        continuation.put("active_card_souls", activeCardSouls);
        ArrayList<Map<String, Object>> bottledCards = new ArrayList<Map<String, Object>>();
        if (AbstractDungeon.player != null && AbstractDungeon.player.masterDeck != null) {
            for (int index = 0; index < AbstractDungeon.player.masterDeck.group.size(); ++index) {
                AbstractCard card = AbstractDungeon.player.masterDeck.group.get(index);
                String bottleType = card.inBottleFlame ? "ATTACK"
                    : card.inBottleLightning ? "SKILL"
                    : card.inBottleTornado ? "POWER" : null;
                if (bottleType == null) {
                    continue;
                }
                Map<String, Object> value = new LinkedHashMap<String, Object>();
                value.put("type", bottleType);
                value.put("deck_index", index);
                value.put("uuid", card.uuid == null ? null : card.uuid.toString());
                value.put("id", card.cardID);
                value.put("upgrades", card.timesUpgraded);
                value.put("misc", card.misc);
                bottledCards.add(value);
            }
        }
        continuation.put("bottled_cards", bottledCards);
        Map<String, Object> timingEvidence = new LinkedHashMap<String, Object>();
        timingEvidence.put("fps_limit", Settings.MAX_FPS);
        timingEvidence.put("discovery_completion_serial", DiscoveryTimingPatch.completionSerial);
        timingEvidence.put("discovery_retrieval_updates", DiscoveryTimingPatch.lastRetrievalUpdates);
        ArrayList<Map<String, Object>> monsterIntents = new ArrayList<Map<String, Object>>();
        if (AbstractDungeon.getMonsters() != null) {
            for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                Map<String, Object> intent = new LinkedHashMap<String, Object>();
                EnemyMoveInfo move = ReflectionHacks.getPrivate(
                    monster, AbstractMonster.class, "move"
                );
                intent.put("intent", move == null || move.intent == null
                    ? "UNKNOWN" : move.intent.name());
                intent.put("next_move", move == null ? monster.nextMove : move.nextMove);
                intent.put("base_damage", move == null ? -1 : move.baseDamage);
                intent.put("damage", adjustedIntentDamage(monster, move));
                intent.put("hits", move != null && move.isMultiDamage ? move.multiplier : 1);
                intent.put("multiplier", move == null ? 0 : move.multiplier);
                intent.put("multi_damage", move != null && move.isMultiDamage);
                monsterIntents.add(intent);
            }
        }
        ArrayList<ArrayList<Map<String, Object>>> combatRewardCards =
            new ArrayList<ArrayList<Map<String, Object>>>();
        if (AbstractDungeon.combatRewardScreen != null
                && AbstractDungeon.combatRewardScreen.rewards != null) {
            for (RewardItem reward : AbstractDungeon.combatRewardScreen.rewards) {
                if (reward.type == RewardItem.RewardType.CARD && reward.cards != null) {
                    ArrayList<Map<String, Object>> cards = new ArrayList<Map<String, Object>>();
                    for (AbstractCard card : reward.cards) {
                        Map<String, Object> value = new LinkedHashMap<String, Object>();
                        value.put("id", card.cardID);
                        value.put("upgrades", card.timesUpgraded);
                        cards.add(value);
                    }
                    combatRewardCards.add(cards);
                }
            }
        }
        AbstractEvent currentEvent = AbstractDungeon.getCurrRoom() == null
            ? null : AbstractDungeon.getCurrRoom().event;
        ArrayList<Map<String, Object>> eventOptionRows =
            new ArrayList<Map<String, Object>>();
        if (currentEvent != null) {
            Iterable<LargeDialogOptionButton> buttons = currentEvent.hasDialog
                ? RoomEventDialog.optionList : currentEvent.imageEventText.optionList;
            int semanticRow = 0;
            int[] semanticSlots = null;
            if (currentEvent instanceof Designer) {
                boolean upgradeOne = ReflectionHacks.getPrivate(
                    currentEvent, Designer.class, "adjustmentUpgradesOne"
                );
                boolean cleanUpRemoves = ReflectionHacks.getPrivate(
                    currentEvent, Designer.class, "cleanUpRemovesCards"
                );
                semanticSlots = new int[] {
                    upgradeOne ? 0 : 1, cleanUpRemoves ? 2 : 3, 4, 5
                };
            } else if (currentEvent instanceof Vampires) {
                semanticSlots = AbstractDungeon.player.hasRelic("Blood Vial")
                    ? new int[] {1, 0, 2} : new int[] {1, 2};
            }
            for (LargeDialogOptionButton button : buttons) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("choice_index", semanticSlots != null && semanticRow < semanticSlots.length
                    ? semanticSlots[semanticRow] : button.slot);
                row.put("disabled", button.isDisabled);
                row.put("text", button.msg);
                eventOptionRows.add(row);
                ++semanticRow;
            }
        }
        ArrayList<Map<String, Object>> matchSlots = matchSlots(currentEvent);
        Gson gson = new Gson();
        if (OracleScenarioPatch.activeScenario != null
                && String.valueOf(OracleScenarioPatch.activeScenario.get("scenario_id"))
                    .startsWith("event_probe:")
                && currentEvent != null && currentEvent.hasDialog
                && !RoomEventDialog.optionList.isEmpty()) {
            ArrayList<Map<String, Object>> roomOptions =
                new ArrayList<Map<String, Object>>();
            for (LargeDialogOptionButton button : RoomEventDialog.optionList) {
                Map<String, Object> option = new LinkedHashMap<String, Object>();
                option.put("choice_index", button.slot);
                option.put("disabled", button.isDisabled);
                option.put("text", button.msg);
                option.put("label", "");
                roomOptions.add(option);
            }
            String needle = "\"options\":[]";
            int optionsAt = json.indexOf(needle);
            if (optionsAt >= 0) {
                json = json.substring(0, optionsAt)
                    + "\"options\":" + gson.toJson(roomOptions)
                    + json.substring(optionsAt + needle.length());
            }
        }
        return json.substring(0, json.length() - 1)
            + ",\"_parity_schema\":" + gson.toJson(INSTRUMENTATION_SCHEMA)
            + ",\"_rng\":" + gson.toJson(rng)
            + ",\"_parity_run\":" + gson.toJson(run)
            + ",\"_continuation\":" + gson.toJson(continuation)
            + ",\"_timing_evidence\":" + gson.toJson(timingEvidence)
            + ",\"_monster_intents\":" + gson.toJson(monsterIntents)
            + ",\"_combat_reward_cards\":" + gson.toJson(combatRewardCards)
            + ",\"_event_option_rows\":" + gson.toJson(eventOptionRows)
            + ",\"_match_slots\":" + gson.toJson(matchSlots)
            + ",\"math_seed\":" + Long.toUnsignedString(ParityRng.mathSeed)
            + (OracleScenarioPatch.activeScenario == null ? ""
                : ",\"_parity_scenario\":"
                    + gson.toJson(OracleScenarioPatch.activeScenario))
            + "}";
    }

    @SpirePatch(clz = GameStateConverter.class, method = "getCommunicationState")
    public static class AddRngState {
        @SpireRawPatch
        public static void Raw(CtBehavior method) throws CannotCompileException {
            method.insertAfter("$_ = spirecomm.parity.CommunicationStatePatch.inject($_);");
        }
    }
}
