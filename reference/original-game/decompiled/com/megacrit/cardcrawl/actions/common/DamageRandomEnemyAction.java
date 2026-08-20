/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class DamageRandomEnemyAction
extends AbstractGameAction {
    private DamageInfo info;

    public DamageRandomEnemyAction(DamageInfo info, AbstractGameAction.AttackEffect effect) {
        this.info = info;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = effect;
    }

    @Override
    public void update() {
        this.target = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
        if (this.target != null) {
            this.addToTop(new DamageAction(this.target, this.info, this.attackEffect));
        }
        this.isDone = true;
    }
}

