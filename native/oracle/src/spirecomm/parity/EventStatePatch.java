package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.GremlinMatchGame;
import communicationmod.GameStateConverter;
import java.lang.reflect.Field;
import java.util.HashMap;

/** The attempt counter is rendered publicly by GremlinMatchGame.render. */
public class EventStatePatch {
    @SpirePatch(clz = GameStateConverter.class, method = "getEventState")
    public static class AddMatchAttempts {
        @SpirePostfixPatch
        public static HashMap<String, Object> Postfix(HashMap<String, Object> result) {
            if (AbstractDungeon.getCurrRoom().event instanceof GremlinMatchGame) {
                try {
                    Field counter = GremlinMatchGame.class.getDeclaredField("attemptCount");
                    counter.setAccessible(true);
                    result.put("attempts_remaining", counter.getInt(AbstractDungeon.getCurrRoom().event));
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Cannot project public Match and Keep attempts", error);
                }
            }
            return result;
        }
    }
}
