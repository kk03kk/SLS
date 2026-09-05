package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import communicationmod.GameStateConverter;
import java.util.HashMap;

/** Public card state only. No RNG, future draws, or action-queue internals. */
public class CardStatePatch {
    @SpirePatch(clz = GameStateConverter.class, method = "convertCardToJson")
    public static class AddDynamicFields {
        @SpirePostfixPatch
        public static HashMap<String, Object> Postfix(
                HashMap<String, Object> result, AbstractCard card) {
            result.put("base_cost", card.cost);
            result.put("cost_for_turn", card.costForTurn);
            result.put("special_data", card.misc);
            // Rampage grows baseDamage, not misc. Keep these meanings distinct.
            result.put("base_damage", card.baseDamage);
            result.put("free_to_play_once", card.freeToPlayOnce);
            result.put("retain", card.retain);
            result.put("self_retain", card.selfRetain);
            result.put("bottled_flame", card.inBottleFlame);
            result.put("bottled_lightning", card.inBottleLightning);
            result.put("bottled_tornado", card.inBottleTornado);
            return result;
        }
    }
}
