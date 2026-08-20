/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CalculatedGambleAction
extends AbstractGameAction {
    private float startingDuration;
    private boolean isUpgraded;

    public CalculatedGambleAction(boolean upgraded) {
        this.target = AbstractDungeon.player;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.startingDuration = Settings.ACTION_DUR_FAST;
        this.duration = Settings.ACTION_DUR_FAST;
        this.isUpgraded = upgraded;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            int count = AbstractDungeon.player.hand.size();
            if (this.isUpgraded) {
                this.addToTop(new DrawCardAction(this.target, count + 1));
                this.addToTop(new DiscardAction(this.target, this.target, count, true));
            } else if (count != 0) {
                this.addToTop(new DrawCardAction(this.target, count));
                this.addToTop(new DiscardAction(this.target, this.target, count, true));
            }
            this.isDone = true;
        }
    }
}

