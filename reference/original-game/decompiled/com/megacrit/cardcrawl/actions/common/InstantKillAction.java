/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;

public class InstantKillAction
extends AbstractGameAction {
    public InstantKillAction(AbstractCreature target) {
        this.source = null;
        this.target = target;
    }

    @Override
    public void update() {
        this.target.currentHealth = 0;
        this.target.healthBarUpdatedEvent();
        this.target.damage(new DamageInfo(null, 0, DamageInfo.DamageType.HP_LOSS));
        this.isDone = true;
    }
}

