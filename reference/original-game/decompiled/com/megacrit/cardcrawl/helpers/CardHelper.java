/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CardHelper {
    private static final Logger logger = LogManager.getLogger(CardHelper.class.getName());
    public static final int COMMON_CARD_LIMIT = 3;
    public static final int UNCOMMON_CARD_LIMIT = 2;
    public static HashMap<String, Integer> obtainedCards = new HashMap();
    public static ArrayList<CardInfo> removedCards = new ArrayList();

    public static void obtain(String key, AbstractCard.CardRarity rarity, AbstractCard.CardColor color) {
        if (rarity == AbstractCard.CardRarity.SPECIAL || rarity == AbstractCard.CardRarity.BASIC || rarity == AbstractCard.CardRarity.CURSE) {
            logger.info("No need to track rarity type: " + rarity.name());
            return;
        }
        if (obtainedCards.containsKey(key)) {
            int tmp = obtainedCards.get(key) + 1;
            obtainedCards.put(key, tmp);
            logger.info("Obtained " + key + " (" + rarity.name() + "). You have " + tmp + " now");
        } else {
            obtainedCards.put(key, 1);
            logger.info("Obtained " + key + " (" + rarity.name() + "). Creating new map entry.");
        }
        UnlockTracker.markCardAsSeen(key);
    }

    public static void clear() {
        logger.info("Clearing CardHelper (obtained cards)");
        removedCards.clear();
        obtainedCards.clear();
    }

    public static Color getColor(int r, int g, int b) {
        return new Color((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, 1.0f);
    }

    public static Color getColor(float r, float g, float b) {
        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
    }

    public static boolean hasCardWithXDamage(int damage) {
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (c.type != AbstractCard.CardType.ATTACK || c.baseDamage < 10) continue;
            logger.info(c.name + " is 10 Attack!");
            return true;
        }
        return false;
    }

    public static boolean hasCardWithID(String targetID) {
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (!c.cardID.equals(targetID)) continue;
            return true;
        }
        return false;
    }

    public static boolean hasCardType(AbstractCard.CardType hasType) {
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (c.type != hasType) continue;
            return true;
        }
        return false;
    }

    public static boolean hasCardWithType(AbstractCard.CardType type) {
        for (AbstractCard c : CardGroup.getGroupWithoutBottledCards((CardGroup)AbstractDungeon.player.masterDeck).group) {
            if (c.type != type) continue;
            return true;
        }
        return false;
    }

    public static AbstractCard returnCardOfType(AbstractCard.CardType type, Random rng) {
        ArrayList<AbstractCard> cards = new ArrayList<AbstractCard>();
        for (AbstractCard c : CardGroup.getGroupWithoutBottledCards((CardGroup)AbstractDungeon.player.masterDeck).group) {
            if (c.type != type) continue;
            cards.add(c);
        }
        return (AbstractCard)cards.remove(rng.random(cards.size() - 1));
    }

    public static boolean hasUpgradedCard() {
        for (AbstractCard c : CardGroup.getGroupWithoutBottledCards((CardGroup)AbstractDungeon.player.masterDeck).group) {
            if (!c.upgraded) continue;
            return true;
        }
        return false;
    }

    public static AbstractCard returnUpgradedCard(Random rng) {
        ArrayList<AbstractCard> cards = new ArrayList<AbstractCard>();
        for (AbstractCard c : CardGroup.getGroupWithoutBottledCards((CardGroup)AbstractDungeon.player.masterDeck).group) {
            if (!c.upgraded) continue;
            cards.add(c);
        }
        return (AbstractCard)cards.remove(rng.random(cards.size() - 1));
    }

    public static class CardInfo {
        String id;
        String name;
        AbstractCard.CardRarity rarity;
        AbstractCard.CardColor color;

        public CardInfo(String id, String name, AbstractCard.CardRarity rarity, AbstractCard.CardColor color) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.color = color;
        }
    }
}

