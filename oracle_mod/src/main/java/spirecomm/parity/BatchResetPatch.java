package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.RestRoom;
import communicationmod.CommandExecutor;
import java.util.ArrayList;

/** Adds a test-only command that returns a run to the main menu in-process. */
public final class BatchResetPatch {
    public static final String COMMAND = "reset_run";

    private BatchResetPatch() {}

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            if (CommandExecutor.isInDungeon() && !commands.contains(COMMAND)) {
                commands.add(COMMAND);
            }
            return commands;
        }
    }

    @SpirePatch(clz = CommandExecutor.class, method = "executeCommand")
    public static class Execute {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> Prefix(String command) {
            if (!COMMAND.equalsIgnoreCase(command.trim())) {
                return SpireReturn.Continue();
            }
            CardCrawlGame.music.fadeAll();
            AbstractDungeon.getCurrRoom().clearEvent();
            AbstractDungeon.closeCurrentScreen();
            CardCrawlGame.startOver();
            if (RestRoom.lastFireSoundId != 0L) {
                CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
            }
            if (AbstractDungeon.player != null && AbstractDungeon.player.stance != null
                && !"Neutral".equals(AbstractDungeon.player.stance.ID)) {
                AbstractDungeon.player.stance.stopIdleSfx();
            }
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
