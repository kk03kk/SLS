/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class ChangeStateAction
extends AbstractGameAction {
    private boolean called = false;
    private AbstractMonster m;
    private String stateName;

    public ChangeStateAction(AbstractMonster monster, String stateName) {
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.m = monster;
        this.stateName = stateName;
    }

    @Override
    public void update() {
        if (!this.called) {
            this.m.changeState(this.stateName);
            this.called = true;
            this.isDone = true;
        }
    }
}

