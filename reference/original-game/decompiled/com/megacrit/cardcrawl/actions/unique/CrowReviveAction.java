/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.TintEffect;

public class CrowReviveAction
extends AbstractGameAction {
    public CrowReviveAction(AbstractMonster target, AbstractCreature source) {
        this.setValues(target, source, 0);
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f && this.target instanceof AbstractMonster) {
            this.target.isDying = false;
            this.target.heal(this.target.maxHealth);
            this.target.healthBarRevivedEvent();
            ((AbstractMonster)this.target).deathTimer = 0.0f;
            ((AbstractMonster)this.target).tint = new TintEffect();
            ((AbstractMonster)this.target).tintFadeOutCalled = false;
            ((AbstractMonster)this.target).isDead = false;
            this.target.powers.clear();
        }
        this.tickDuration();
    }
}

