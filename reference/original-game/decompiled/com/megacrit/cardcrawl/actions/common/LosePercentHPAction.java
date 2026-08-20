/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class LosePercentHPAction
extends AbstractGameAction {
    public LosePercentHPAction(int percent) {
        this.amount = percent;
    }

    @Override
    public void update() {
        float percentConversion = (float)this.amount / 100.0f;
        int amountToLose = (int)((float)AbstractDungeon.player.currentHealth * percentConversion);
        this.addToTop(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, amountToLose, AbstractGameAction.AttackEffect.FIRE));
        this.isDone = true;
    }
}

