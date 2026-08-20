/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class BarrageAction
extends AbstractGameAction {
    private DamageInfo info = null;
    private AbstractCreature target;

    public BarrageAction(AbstractCreature m, DamageInfo info) {
        this.info = info;
        this.target = m;
    }

    @Override
    public void update() {
        for (int i = 0; i < AbstractDungeon.player.orbs.size(); ++i) {
            if (AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot) continue;
            this.addToTop(new DamageAction(this.target, this.info, AbstractGameAction.AttackEffect.BLUNT_LIGHT, true));
        }
        this.isDone = true;
    }
}

