package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.random.Random;
import communicationmod.CommandExecutor;
import communicationmod.CommunicationMod;
import communicationmod.GameStateListener;
import java.util.ArrayList;

/** Explicit initial setup only. No combat callbacks or RNG are patched. */
public final class OracleDungeonContext {
    public static final String COMMAND = "parity_dungeon";

    private static void prepare(int act, int floor, String kind) {
        if ((act < 2 || act > 4) || floor < 1 || AbstractDungeon.player == null
                || !("MONSTER".equals(kind) || "ELITE".equals(kind) || "BOSS".equals(kind))) {
            throw new IllegalArgumentException("unsupported controlled dungeon context");
        }
        AbstractDungeon.effectList.clear();
        AbstractDungeon.effectsQueue.clear();
        AbstractDungeon.topLevelEffects.clear();
        AbstractDungeon.topLevelEffectsQueue.clear();
        if (AbstractDungeon.isScreenUp) AbstractDungeon.closeCurrentScreen();
        // The stock superclass transition increments actNum and initializes
        // pools, action manager and boss context. Concrete constructors create
        // actual scenes/maps; simply assigning actNum would omit this.
        AbstractDungeon.actNum = act - 1;
        if (act == 2) new TheCity(AbstractDungeon.player, new ArrayList<String>());
        else if (act == 3) new TheBeyond(AbstractDungeon.player, new ArrayList<String>());
        else new TheEnding(AbstractDungeon.player, new ArrayList<String>());
        String expected = act == 2 ? "TheCity" : act == 3 ? "TheBeyond" : "TheEnding";
        if (AbstractDungeon.actNum != act || !expected.equals(AbstractDungeon.id)
                || !CardCrawlGame.dungeon.getClass().getSimpleName().equals(expected)) {
            throw new IllegalStateException("stock dungeon constructor did not establish context");
        }
        MapRoomNode selected = null;
        for (ArrayList<MapRoomNode> row : AbstractDungeon.map) {
            for (MapRoomNode node : row) {
                boolean match = "BOSS".equals(kind) ? node.room instanceof MonsterRoomBoss
                    : "ELITE".equals(kind) ? node.room instanceof MonsterRoomElite
                    : node.room != null && node.room.getClass() == MonsterRoom.class;
                if (selected == null && match) selected = node;
            }
        }
        // Act3's boss room is outside the generated ordinary map rows.
        if (selected == null && act <= 3 && "BOSS".equals(kind)) {
            selected = new MapRoomNode(3, 15);
            selected.room = new MonsterRoomBoss();
        }
        if (selected == null) throw new IllegalStateException("no stock room for controlled context");
        AbstractDungeon.currMapNode = selected;
        AbstractDungeon.floorNum = floor;
        AbstractDungeon.firstRoomChosen = true;
        AbstractDungeon.isScreenUp = false;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
        long roomSeed = Settings.seed.longValue() + floor;
        AbstractDungeon.aiRng = new Random(Long.valueOf(roomSeed));
        AbstractDungeon.monsterHpRng = new Random(Long.valueOf(roomSeed));
        AbstractDungeon.shuffleRng = new Random(Long.valueOf(roomSeed));
        AbstractDungeon.cardRandomRng = new Random(Long.valueOf(roomSeed));
        // Bootstrap a stock monster to expose a live preparation boundary.
        // Save its exact streams before constructing the requested encounter.
        // This is a controlled fixture, never a normal-start trajectory.
        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter("Cultist");
        AbstractDungeon.getMonsters().init();
        AbstractDungeon.getMonsters().usePreBattleAction();
        AbstractDungeon.getMonsters().showIntent();
        AbstractDungeon.actionManager.phase = com.megacrit.cardcrawl.actions.GameActionManager.Phase.WAITING_ON_USER;
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
            if (!OracleMode.validation() || !command.trim().startsWith(COMMAND + " "))
                return SpireReturn.Continue();
            String[] words = command.trim().split("\\s+");
            if (words.length != 4) throw new IllegalArgumentException("parity_dungeon ACT FLOOR ROOM");
            prepare(Integer.parseInt(words[1]), Integer.parseInt(words[2]), words[3]);
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
