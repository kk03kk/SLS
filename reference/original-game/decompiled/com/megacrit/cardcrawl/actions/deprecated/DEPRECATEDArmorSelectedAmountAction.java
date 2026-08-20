/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.MetallicizePower;

public class DEPRECATEDArmorSelectedAmountAction
extends AbstractGameAction {
    public DEPRECATEDArmorSelectedAmountAction(AbstractCreature target, AbstractCreature source, int multiplier) {
        this.setValues(target, source, multiplier);
        this.actionType = AbstractGameAction.ActionType.POWER;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            this.amount = AbstractDungeon.handCardSelectScreen.numSelected * this.amount;
            AbstractDungeon.actionManager.addToTop(new ApplyPowerAction(this.target, this.target, new MetallicizePower(this.target, this.amount), this.amount));
        }
        this.tickDuration();
    }
}

