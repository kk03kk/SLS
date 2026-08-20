/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class DoubleYourBlockAction
extends AbstractGameAction {
    public DoubleYourBlockAction(AbstractCreature target) {
        this.duration = 0.5f;
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.target = target;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f && this.target != null && this.target.currentBlock > 0) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.SHIELD));
            this.target.addBlock(this.target.currentBlock);
        }
        this.tickDuration();
    }
}

