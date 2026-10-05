package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ModHelper;
import communicationmod.CommandExecutor;
import java.util.ArrayList;

/** Trigger the stock main-menu Resume path without constructing run state. */
public final class ContinuePatch {
    public static final String COMMAND = "parity_continue";

    private ContinuePatch() {}

    private static AbstractPlayer resumableCharacter() {
        if (CommandExecutor.isInDungeon()
            || CardCrawlGame.characterManager == null
            || !CardCrawlGame.characterManager.anySaveFileExists()) {
            return null;
        }
        AbstractPlayer player = CardCrawlGame.characterManager.loadChosenCharacter();
        if (player == null || player.chosenClass != AbstractPlayer.PlayerClass.IRONCLAD) {
            return null;
        }
        return player;
    }

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            if (resumableCharacter() != null && !commands.contains(COMMAND)) {
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
            AbstractPlayer player = resumableCharacter();
            if (player == null) {
                throw new IllegalStateException(
                    "parity_continue requires the main menu and a valid Ironclad autosave"
                );
            }
            CardCrawlGame.loadingSave = true;
            CardCrawlGame.chosenCharacter = player.chosenClass;
            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
            Settings.isDailyRun = false;
            Settings.isTrial = false;
            ModHelper.setModsFalse();
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
