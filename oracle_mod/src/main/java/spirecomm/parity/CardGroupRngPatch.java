package spirecomm.parity;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import java.util.ArrayList;
import java.util.Collections;

public final class CardGroupRngPatch {
    private static AbstractCard choose(ArrayList<AbstractCard> cards) {
        return cards.get(ParityRng.requireMathRng().random(cards.size() - 1));
    }

    @SpirePatch(clz = CardGroup.class, method = "getRandomCard", paramtypez = {boolean.class})
    public static class AnyCard {
        public static SpireReturn<AbstractCard> Prefix(CardGroup __instance, boolean useRng) {
            if (useRng) return SpireReturn.Continue();
            return SpireReturn.Return(choose(__instance.group));
        }
    }

    @SpirePatch(
        clz = CardGroup.class,
        method = "getRandomCard",
        paramtypez = {boolean.class, AbstractCard.CardRarity.class}
    )
    public static class ByRarity {
        public static SpireReturn<AbstractCard> Prefix(
            CardGroup __instance, boolean useRng, AbstractCard.CardRarity rarity) {
            if (useRng) return SpireReturn.Continue();
            ArrayList<AbstractCard> candidates = new ArrayList<AbstractCard>();
            for (AbstractCard card : __instance.group) {
                if (card.rarity == rarity) candidates.add(card);
            }
            if (candidates.isEmpty()) return SpireReturn.Return(null);
            Collections.sort(candidates);
            return SpireReturn.Return(choose(candidates));
        }
    }

    @SpirePatch(
        clz = CardGroup.class,
        method = "getRandomCard",
        paramtypez = {AbstractCard.CardType.class, boolean.class}
    )
    public static class ByType {
        public static SpireReturn<AbstractCard> Prefix(
            CardGroup __instance, AbstractCard.CardType type, boolean useRng) {
            if (useRng) return SpireReturn.Continue();
            ArrayList<AbstractCard> candidates = new ArrayList<AbstractCard>();
            for (AbstractCard card : __instance.group) {
                if (card.type == type) candidates.add(card);
            }
            if (candidates.isEmpty()) return SpireReturn.Return(null);
            Collections.sort(candidates);
            return SpireReturn.Return(choose(candidates));
        }
    }
}
