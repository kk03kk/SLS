/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CanLoseAction
extends AbstractGameAction {
    @Override
    public void update() {
        AbstractDungeon.getCurrRoom().cannotLose = false;
        this.isDone = true;
    }
}

