package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import communicationmod.GameStateConverter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Public card state only. No RNG, future draws, or action-queue internals. */
public class CardStatePatch {
    /** Reward previews are freely viewable before choosing a card or skipping.
     * Keep the same public card fields as the opened card-reward screen.
     */
    @SpirePatch(clz = GameStateConverter.class, method = "getCombatRewardState")
    public static class AddRewardPreviews {
        @SpirePostfixPatch
        @SuppressWarnings("unchecked")
        public static HashMap<String, Object> Postfix(HashMap<String, Object> result) {
            try {
                Method convert = GameStateConverter.class.getDeclaredMethod("convertCardToJson", AbstractCard.class);
                convert.setAccessible(true);
                List<HashMap<String, Object>> rows = (List<HashMap<String, Object>>) result.get("rewards");
                for (int i = 0; i < AbstractDungeon.combatRewardScreen.rewards.size(); ++i) {
                    RewardItem reward = AbstractDungeon.combatRewardScreen.rewards.get(i);
                    if (reward.type != RewardItem.RewardType.CARD) continue;
                    ArrayList<Object> cards = new ArrayList<>();
                    for (AbstractCard card : reward.cards) cards.add(convert.invoke(null, card));
                    rows.get(i).put("cards", cards);
                }
                return result;
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot project public reward card previews", error);
            }
        }
    }

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
