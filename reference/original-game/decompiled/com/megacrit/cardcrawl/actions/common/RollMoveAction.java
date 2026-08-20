/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class RollMoveAction
extends AbstractGameAction {
    private AbstractMonster monster;

    public RollMoveAction(AbstractMonster monster) {
        this.monster = monster;
    }

    @Override
    public void update() {
        this.monster.rollMove();
        this.isDone = true;
    }
}

