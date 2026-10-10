package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Controlled initial room only; all subsequent options/effects are stock. */
public final class OracleKeyRoom {
    public static final String COMMAND = "parity_key_room";

    private static boolean rootReachable(MapRoomNode node) {
        if (node.y == 0) return node.hasEdges();
        for (MapRoomNode parent : node.getParents()) {
            if (parent.y >= node.y) throw new IllegalStateException("non-acyclic stock map");
            if (rootReachable(parent)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void prepare(String identifier, String corpus) {
        if (!("fullrun-key-acquisition-r1".equals(corpus)
                || "fullrun-key-acquisition-r2".equals(corpus)
                || "fullrun-green-key-r1".equals(corpus)
                || "fullrun-green-key-r2".equals(corpus))) {
            throw new IllegalArgumentException("undeclared key room corpus");
        }
        boolean stockCombatFixture = "fullrun-green-key-r2".equals(corpus);
        boolean greenFixture = "fullrun-green-key-r1".equals(corpus) || stockCombatFixture;
        boolean reachableFixture = "fullrun-key-acquisition-r2".equals(corpus) || greenFixture;
        InputStream resource = OracleKeyRoom.class.getResourceAsStream(
            "/spirecomm/parity/" + corpus + ".json");
        if (resource == null) throw new IllegalStateException("missing key room resource");
        Map<String, Object> manifest;
        try (InputStreamReader reader = new InputStreamReader(resource, "UTF-8")) {
            manifest = new Gson().fromJson(reader, Map.class);
        } catch (Exception error) {
            throw new IllegalStateException("cannot read frozen key room resource", error);
        }
        Map<String, Object> scene = null;
        String schema = stockCombatFixture ? "sls-green-key-scenes-v2" : greenFixture ? "sls-green-key-scenes-v1"
            : reachableFixture ? "sls-key-room-scenes-v2" : "sls-key-room-scenes-v1";
        if (!schema.equals(manifest.get("schema")) || (reachableFixture
                && (!(greenFixture ? "ROOT_REACHABLE_STOCK_BURNING_ELITE" : "FIRST_ROOT_REACHABLE_ROOM").equals(manifest.get("map_node_policy"))
                || !"STOCK_SEED_PLUS_DERIVED_FLOOR_FIVE_STREAMS".equals(manifest.get("room_rng_policy"))))) {
            throw new IllegalArgumentException("unsupported key room schema");
        }
        for (Object value : (List<?>) manifest.get("scenes")) {
            Map<String, Object> candidate = (Map<String, Object>) value;
            if (identifier.equals(candidate.get("id"))) scene = candidate;
        }
        if (scene == null || AbstractDungeon.player == null
                || ((Number) scene.get("act")).intValue() != 2) {
            throw new IllegalArgumentException("unknown or unsupported controlled key room");
        }
        boolean seedAllowed = false;
        for (Object value : (List<?>) scene.get("seeds")) {
            if (((Number) value).longValue() == Settings.seed.longValue()) seedAllowed = true;
        }
        if (!seedAllowed) throw new IllegalArgumentException("undeclared key room seed");
        String kind = (String) scene.get("room");
        if (!("REST".equals(kind) || "TREASURE".equals(kind) || (greenFixture && "ELITE".equals(kind)))) {
            throw new IllegalArgumentException("unsupported controlled key room type");
        }
        Map<String, Object> initial = (Map<String, Object>) scene.get("initial");
        if (reachableFixture && !"ACT2_MAP_Y_PLUS_18".equals(scene.get("floor_policy"))) {
            throw new IllegalArgumentException("undeclared floor derivation");
        }
        int hp = ((Number) initial.get("hp")).intValue();
        int maxHp = ((Number) initial.get("max_hp")).intValue();
        if (hp <= 0 || hp > maxHp || maxHp > 1000) throw new IllegalArgumentException("invalid HP");
        for (Object card : (List<?>) initial.get("deck")) {
            if (CardLibrary.getCard((String) card) == null) {
                throw new IllegalArgumentException("unknown initial card: " + card);
            }
        }
        for (Object relic : (List<?>) initial.get("relics")) {
            if (RelicLibrary.getRelic((String) relic) == null) {
                throw new IllegalArgumentException("unknown initial relic: " + relic);
            }
        }

        AbstractDungeon.effectList.clear();
        AbstractDungeon.effectsQueue.clear();
        AbstractDungeon.topLevelEffects.clear();
        AbstractDungeon.topLevelEffectsQueue.clear();
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        Settings.isFinalActAvailable = Boolean.TRUE.equals(initial.get("final_act_available"));
        Settings.hasRubyKey = Boolean.TRUE.equals(initial.get("ruby_key"));
        Settings.hasEmeraldKey = false;
        Settings.hasSapphireKey = Boolean.TRUE.equals(initial.get("sapphire_key"));
        AbstractDungeon.actNum = 1;
        new TheCity(AbstractDungeon.player, new ArrayList<String>());
        if (AbstractDungeon.actNum != 2 || !"TheCity".equals(AbstractDungeon.id)
                || !CardCrawlGame.dungeon.getClass().getSimpleName().equals("TheCity")) {
            throw new IllegalStateException("actual stock TheCity not established");
        }
        MapRoomNode selected = null;
        for (ArrayList<MapRoomNode> row : AbstractDungeon.map) {
            for (MapRoomNode node : row) {
                if (selected == null && ("REST".equals(kind) ? node.room instanceof RestRoom
                        : "TREASURE".equals(kind) ? node.room instanceof TreasureRoom
                        : node.room instanceof com.megacrit.cardcrawl.rooms.MonsterRoomElite && node.hasEmeraldKey)
                        && (!reachableFixture || rootReachable(node))) selected = node;
            }
        }
        if (selected == null) throw new IllegalStateException("stock map lacks requested room");
        AbstractDungeon.currMapNode = selected;
        AbstractDungeon.floorNum = reachableFixture ? selected.y + 18
            : ((Number) scene.get("floor")).intValue();
        if (reachableFixture) {
            long roomSeed = Settings.seed.longValue() + AbstractDungeon.floorNum;
            AbstractDungeon.aiRng = new com.megacrit.cardcrawl.random.Random(Long.valueOf(roomSeed));
            AbstractDungeon.shuffleRng = new com.megacrit.cardcrawl.random.Random(Long.valueOf(roomSeed));
            AbstractDungeon.cardRandomRng = new com.megacrit.cardcrawl.random.Random(Long.valueOf(roomSeed));
            AbstractDungeon.miscRng = new com.megacrit.cardcrawl.random.Random(Long.valueOf(roomSeed));
            AbstractDungeon.monsterHpRng = new com.megacrit.cardcrawl.random.Random(Long.valueOf(roomSeed));
        }
        AbstractDungeon.firstRoomChosen = true;
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.player.currentHealth = hp;
        AbstractDungeon.player.maxHealth = maxHp;
        AbstractDungeon.player.gold = ((Number) initial.get("gold")).intValue();
        AbstractDungeon.player.hand.clear();
        AbstractDungeon.player.drawPile.clear();
        AbstractDungeon.player.discardPile.clear();
        AbstractDungeon.player.exhaustPile.clear();
        AbstractDungeon.player.masterDeck.clear();
        for (Object card : (List<?>) initial.get("deck")) {
            AbstractDungeon.player.masterDeck.addToBottom(CardLibrary.getCard((String) card).makeCopy());
        }
        AbstractDungeon.player.relics.clear();
        for (Object relic : (List<?>) initial.get("relics")) {
            AbstractDungeon.player.relics.add(RelicLibrary.getRelic((String) relic).makeCopy());
        }
        AbstractDungeon.player.potions.clear();
        AbstractDungeon.player.potions.add(new PotionSlot(0));
        AbstractDungeon.player.potions.add(new PotionSlot(1));
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("scenario_id", "key-room:" + identifier);
        evidence.put("source", "controlled-stock-room-entry");
        evidence.put("manifest_schema", manifest.get("schema"));
        evidence.put("corpus", corpus);
        evidence.put("map_node_policy", greenFixture ? "ROOT_REACHABLE_STOCK_BURNING_ELITE"
            : reachableFixture ? "FIRST_ROOT_REACHABLE_ROOM" : "LEGACY_FIRST_ROOM");
        if (greenFixture) evidence.put("map_rng_before_entry", ParityRng.state(AbstractDungeon.mapRng));
        OracleScenarioPatch.activeScenario = evidence;
        // This is the actual stock entry method, including campfire options,
        // onEnterRestRoom callbacks or stock getRandomChest generation.
        AbstractDungeon.getCurrRoom().onPlayerEntry();
        // AbstractDungeon.nextRoomTransition invokes these stock routines
        // in this order. Only R2 includes the real player battle preparation.
        if (stockCombatFixture) AbstractDungeon.player.preBattlePrep();
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
        public static SpireReturn<Boolean> Prefix(String command) {
            if (!OracleMode.validation() || !command.trim().startsWith(COMMAND + " ")) {
                return SpireReturn.Continue();
            }
            String[] words = command.trim().split("\\s+");
            if (words.length != 2 && words.length != 3) {
                throw new IllegalArgumentException("parity_key_room SCENE [CORPUS]");
            }
            prepare(words[1], words.length == 3 ? words[2] : "fullrun-key-acquisition-r1");
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
