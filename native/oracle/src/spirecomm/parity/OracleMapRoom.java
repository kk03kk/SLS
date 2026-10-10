package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.RestRoom;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateListener;
import java.io.InputStreamReader;
import java.util.*;

/** Validation-only initial flags; unmodified stock constructors generate maps. */
public final class OracleMapRoom {
    public static final String COMMAND = "parity_map_room";
    private static boolean reachable(MapRoomNode node) {
        if (node.y == 0) return node.hasEdges();
        for (MapRoomNode parent : node.getParents()) {
            if (parent.y >= node.y) throw new IllegalStateException("non-acyclic stock map");
            if (reachable(parent)) return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private static void prepare(String identifier) throws Exception {
        Map<String, Object> manifest;
        try (InputStreamReader reader = new InputStreamReader(OracleMapRoom.class.getResourceAsStream(
                "/spirecomm/parity/fullrun-held-key-map-r1.json"), "UTF-8")) {
            manifest = new Gson().fromJson(reader, Map.class);
        }
        if (!"sls-held-key-map-scenes-v1".equals(manifest.get("schema")))
            throw new IllegalArgumentException("unsupported held-key map corpus");
        Map<String, Object> scene = null;
        for (Object value : (List<?>) manifest.get("scenes")) {
            Map<String, Object> candidate = (Map<String, Object>) value;
            if (identifier.equals(candidate.get("id"))) scene = candidate;
        }
        if (scene == null || Settings.seed.longValue() != 131200410L
                || AbstractDungeon.ascensionLevel != 20 || AbstractDungeon.player == null)
            throw new IllegalArgumentException("undeclared held-key map input");
        int act = ((Number) scene.get("act")).intValue();
        if (act != 2 && act != 3) throw new IllegalArgumentException("unsupported map act");
        AbstractDungeon.effectList.clear();
        AbstractDungeon.effectsQueue.clear();
        AbstractDungeon.topLevelEffects.clear();
        AbstractDungeon.topLevelEffectsQueue.clear();
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        Settings.isFinalActAvailable = Boolean.TRUE.equals(scene.get("final_act_available"));
        Settings.hasEmeraldKey = Boolean.TRUE.equals(scene.get("emerald_key"));
        Settings.hasRubyKey = false;
        Settings.hasSapphireKey = false;
        AbstractDungeon.actNum = act - 1;
        if (act == 2) new TheCity(AbstractDungeon.player, new ArrayList<String>());
        else new TheBeyond(AbstractDungeon.player, new ArrayList<String>());
        String expected = act == 2 ? "TheCity" : "TheBeyond";
        if (AbstractDungeon.actNum != act || !expected.equals(AbstractDungeon.id)
                || !expected.equals(CardCrawlGame.dungeon.getClass().getSimpleName()))
            throw new IllegalStateException("stock dungeon constructor not established");
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("scenario_id", identifier);
        evidence.put("corpus", "fullrun-held-key-map-r1");
        evidence.put("source", "CONTROLLED_FLAGS_STOCK_DUNGEON_CONSTRUCTOR");
        evidence.put("act", act);
        evidence.put("emerald_key", Settings.hasEmeraldKey);
        evidence.put("final_act_available", Settings.isFinalActAvailable);
        evidence.put("map_rng_after_constructor", ParityRng.state(AbstractDungeon.mapRng));
        MapRoomNode selected = null;
        for (ArrayList<MapRoomNode> row : AbstractDungeon.map) for (MapRoomNode node : row)
            if (selected == null && node.room instanceof RestRoom && reachable(node)) selected = node;
        if (selected == null) throw new IllegalStateException("no reachable stock rest room");
        AbstractDungeon.currMapNode = selected;
        AbstractDungeon.floorNum = selected.y + (act == 2 ? 18 : 35);
        AbstractDungeon.firstRoomChosen = true;
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        OracleScenarioPatch.activeScenario = evidence;
        // Expose generated map through a stable stock room boundary. No map mutation.
        selected.room.onPlayerEntry();
        CommunicationMod.mustSendGameState = true;
        GameStateListener.registerStateChange();
    }
    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            if (OracleMode.validation() && CommandExecutor.isEndCommandAvailable()
                    && !commands.contains(COMMAND)) commands.add(COMMAND);
            return commands;
        }
    }
    @SpirePatch(clz = CommandExecutor.class, method = "executeCommand")
    public static class Execute {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> Prefix(String command) throws Exception {
            if (!OracleMode.validation() || !command.trim().startsWith(COMMAND + " "))
                return SpireReturn.Continue();
            String[] words = command.trim().split("\\s+");
            if (words.length != 2) throw new IllegalArgumentException("parity_map_room SCENE");
            prepare(words[1]);
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
