/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class UnravelingAction
extends AbstractGameAction {
    public UnravelingAction() {
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            block3: for (AbstractCard c : AbstractDungeon.player.hand.group) {
                for (CardQueueItem q : AbstractDungeon.actionManager.cardQueue) {
                    if (q.card != c) continue;
                }
                c.freeToPlayOnce = true;
                switch (c.target) {
                    case SELF_AND_ENEMY: 
                    case ENEMY: {
                        AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(c, AbstractDungeon.getRandomMonster()));
                        continue block3;
                    }
                }
                AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(c, null));
            }
        }
        this.tickDuration();
    }
}

