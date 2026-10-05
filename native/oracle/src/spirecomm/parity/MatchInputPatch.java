package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import communicationmod.CommandExecutor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

/** Deliver one semantic Match-and-Keep pair through the stock input path. */
public final class MatchInputPatch {
    public static final String COMMAND = "parity_match";
    private static final Method UPDATE_MATCH_LOGIC = updateMatchLogic();

    private MatchInputPatch() {}

    private static Method updateMatchLogic() {
        try {
            Method method = GremlinMatchGame.class.getDeclaredMethod("updateMatchGameLogic");
            method.setAccessible(true);
            return method;
        } catch (Exception error) {
            throw new RuntimeException("cannot access stock Match input handler", error);
        }
    }

    private static void click(GremlinMatchGame event, AbstractCard card) {
        InputHelper.mX = Math.round(card.hb.cX);
        InputHelper.mY = Math.round(card.hb.cY);
        InputHelper.justClickedLeft = true;
        try {
            UPDATE_MATCH_LOGIC.invoke(event);
        } catch (Exception error) {
            throw new RuntimeException("stock Match input handler failed", error);
        }
    }

    private static GremlinMatchGame activeEvent() {
        if (!CommandExecutor.isInDungeon() || AbstractDungeon.getCurrRoom() == null) {
            return null;
        }
        AbstractEvent event = AbstractDungeon.getCurrRoom().event;
        if (!(event instanceof GremlinMatchGame)) {
            return null;
        }
        GremlinMatchGame match = (GremlinMatchGame) event;
        return "PLAY".equals(CommunicationStatePatch.matchPhase(match)) ? match : null;
    }

    @SpirePatch(clz = CommandExecutor.class, method = "getAvailableCommands")
    public static class Advertise {
        @SpirePostfixPatch
        public static ArrayList<String> Postfix(ArrayList<String> commands) {
            if (activeEvent() != null && !commands.contains(COMMAND)) {
                commands.add(COMMAND);
            }
            return commands;
        }
    }

    @SpirePatch(clz = CommandExecutor.class, method = "executeCommand")
    public static class Execute {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> Prefix(String command) {
            String normalized = command.trim().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith(COMMAND + " ")) {
                return SpireReturn.Continue();
            }
            String[] parts = normalized.split("\\s+");
            if (parts.length != 3) {
                throw new IllegalArgumentException("parity_match requires two slot indices");
            }
            GremlinMatchGame event = activeEvent();
            if (event == null) {
                throw new IllegalStateException("parity_match is unavailable outside a Match game");
            }
            int left = Integer.parseInt(parts[1]);
            int right = Integer.parseInt(parts[2]);
            if (left == right) {
                throw new IllegalArgumentException("parity_match slots must differ");
            }
            AbstractCard leftCard = CommunicationStatePatch.matchCardForSlot(event, left);
            AbstractCard rightCard = CommunicationStatePatch.matchCardForSlot(event, right);
            if (leftCard == null || rightCard == null) {
                throw new IllegalArgumentException("parity_match references a removed or unknown slot");
            }
            click(event, leftCard);
            click(event, rightCard);
            return SpireReturn.Return(Boolean.TRUE);
        }
    }
}
