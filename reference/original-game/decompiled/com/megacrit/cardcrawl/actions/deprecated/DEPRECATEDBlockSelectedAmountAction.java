/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class DEPRECATEDBlockSelectedAmountAction
extends AbstractGameAction {
    public DEPRECATEDBlockSelectedAmountAction(AbstractCreature target, AbstractCreature source, int multiplier) {
        this.setValues(target, source, multiplier);
        this.actionType = AbstractGameAction.ActionType.BLOCK;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.SHIELD));
            this.amount *= AbstractDungeon.handCardSelectScreen.numSelected;
            this.target.addBlock(this.amount);
        }
        this.tickDuration();
    }
}

