/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;

public class HideHealthBarAction
extends AbstractGameAction {
    public HideHealthBarAction(AbstractCreature owner) {
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.source = owner;
    }

    @Override
    public void update() {
        this.source.hideHealthBar();
        this.isDone = true;
    }
}

