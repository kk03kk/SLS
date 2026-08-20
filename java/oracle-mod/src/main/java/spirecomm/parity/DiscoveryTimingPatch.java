package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.unique.DiscoveryAction;
import com.megacrit.cardcrawl.core.Settings;
import basemod.ReflectionHacks;

/** Observation-only evidence for stock DiscoveryAction's frame-driven RNG bug. */
public final class DiscoveryTimingPatch {
    public static int completionSerial = 0;
    public static int lastRetrievalUpdates = 0;
    private static int activeRetrievalUpdates = 0;

    @SpirePatch(clz = DiscoveryAction.class, method = "update")
    public static class CountUpdates {
        @SpirePrefixPatch
        public static void Prefix(DiscoveryAction action) {
            float duration = ReflectionHacks.getPrivate(
                action, com.megacrit.cardcrawl.actions.AbstractGameAction.class, "duration"
            );
            if (duration == Settings.ACTION_DUR_FAST) {
                activeRetrievalUpdates = 0;
            } else {
                ++activeRetrievalUpdates;
            }
        }

        @SpirePostfixPatch
        public static void Postfix(DiscoveryAction action) {
            if (action.isDone) {
                lastRetrievalUpdates = activeRetrievalUpdates;
                ++completionSerial;
            }
        }
    }
}
