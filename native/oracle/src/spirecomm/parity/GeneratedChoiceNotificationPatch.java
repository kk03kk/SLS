package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import communicationmod.GameStateListener;

/** Notify the bridge when consecutive generated choices reuse CARD_REWARD.
 * Does not select a card, update an action, or change any stock/RNG field.
 */
public final class GeneratedChoiceNotificationPatch {
    @SpirePatch(clz = CardRewardScreen.class, method = "customCombatOpen")
    public static class Open {
        @SpirePostfixPatch
        public static void Postfix() {
            GameStateListener.registerStateChange();
        }
    }
}
