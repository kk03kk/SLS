/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;

public class ClearCardQueueAction
extends AbstractGameAction {
    @Override
    public void update() {
        for (CardQueueItem c : AbstractDungeon.actionManager.cardQueue) {
            if (!AbstractDungeon.player.limbo.contains(c.card)) continue;
            AbstractDungeon.effectList.add(new ExhaustCardEffect(c.card));
            AbstractDungeon.player.limbo.group.remove(c.card);
        }
        AbstractDungeon.actionManager.cardQueue.clear();
        this.isDone = true;
    }
}

