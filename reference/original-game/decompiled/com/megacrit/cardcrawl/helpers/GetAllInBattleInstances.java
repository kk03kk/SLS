/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.HashSet;
import java.util.UUID;

public class GetAllInBattleInstances {
    public static HashSet<AbstractCard> get(UUID uuid) {
        HashSet<AbstractCard> cards = new HashSet<AbstractCard>();
        if (AbstractDungeon.player.cardInUse.uuid.equals(uuid)) {
            cards.add(AbstractDungeon.player.cardInUse);
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (!c.uuid.equals(uuid)) continue;
            cards.add(c);
        }
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (!c.uuid.equals(uuid)) continue;
            cards.add(c);
        }
        for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
            if (!c.uuid.equals(uuid)) continue;
            cards.add(c);
        }
        for (AbstractCard c : AbstractDungeon.player.limbo.group) {
            if (!c.uuid.equals(uuid)) continue;
            cards.add(c);
        }
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.uuid.equals(uuid)) continue;
            cards.add(c);
        }
        return cards;
    }
}

