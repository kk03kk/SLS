/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.Iterator;

public class RestoreRetainedCardsAction
extends AbstractGameAction {
    private CardGroup group;

    public RestoreRetainedCardsAction(CardGroup group) {
        this.setValues(AbstractDungeon.player, this.source, -1);
        this.group = group;
    }

    @Override
    public void update() {
        this.isDone = true;
        Iterator<AbstractCard> c = this.group.group.iterator();
        while (c.hasNext()) {
            AbstractCard e = c.next();
            if (!e.retain && !e.selfRetain) continue;
            e.onRetained();
            AbstractDungeon.player.hand.addToTop(e);
            e.retain = false;
            c.remove();
        }
        AbstractDungeon.player.hand.refreshHandLayout();
    }
}

