/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class InspirationAction
extends AbstractGameAction {
    public InspirationAction(int drawAmt) {
        this.source = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = drawAmt;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST && this.amount - AbstractDungeon.player.hand.size() > 0) {
            this.addToTop(new DrawCardAction(this.source, this.amount - AbstractDungeon.player.hand.size()));
        }
        this.tickDuration();
    }
}

