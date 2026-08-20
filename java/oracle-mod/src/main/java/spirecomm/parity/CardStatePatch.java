package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import communicationmod.GameStateConverter;
import java.util.HashMap;

/** Adds ordering-relevant dynamic card flags omitted by CommunicationMod. */
public final class CardStatePatch {
    private CardStatePatch() {}

    @SpirePatch(clz = GameStateConverter.class, method = "convertCardToJson")
    public static class AddDynamicFields {
        @SpirePostfixPatch
        public static HashMap<String, Object> Postfix(
            HashMap<String, Object> result, AbstractCard card
        ) {
            result.put("base_cost", card.cost);
            result.put("special_data", card.misc);
            result.put("free_to_play_once", card.freeToPlayOnce);
            result.put("retain", card.retain);
            result.put("self_retain", card.selfRetain);
            return result;
        }
    }
}
