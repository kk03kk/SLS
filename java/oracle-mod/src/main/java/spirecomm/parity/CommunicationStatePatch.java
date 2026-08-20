package spirecomm.parity;

import com.autoplay.gson.Gson;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import communicationmod.GameStateConverter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public final class CommunicationStatePatch {
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
            : AbstractDungeon.getCurrRoom().event.getClass().getName());
        continuation.put("event_phase", null);
        continuation.put("action_phase", AbstractDungeon.actionManager == null ? null
            : AbstractDungeon.actionManager.phase.name());
        continuation.put("combat_turn", com.megacrit.cardcrawl.actions.GameActionManager.turn);
        continuation.put("card_selection_source", null);
        continuation.put("card_selection_task", null);
        continuation.put("card_selection_count", 0);
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
        ArrayList<Map<String, Object>> monsterIntents = new ArrayList<Map<String, Object>>();
        if (AbstractDungeon.getMonsters() != null) {
            for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                Map<String, Object> intent = new LinkedHashMap<String, Object>();
                intent.put("intent", monster.intent == null ? "UNKNOWN" : monster.intent.name());
                monsterIntents.add(intent);
            }
        }
        Gson gson = new Gson();
        return json.substring(0, json.length() - 1)
            + ",\"_rng\":" + gson.toJson(rng)
            + ",\"_parity_run\":" + gson.toJson(run)
            + ",\"_continuation\":" + gson.toJson(continuation)
            + ",\"_monster_intents\":" + gson.toJson(monsterIntents)
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
