/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EvokeAllOrbsAction
extends AbstractGameAction {
    public EvokeAllOrbsAction() {
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        for (int i = 0; i < AbstractDungeon.player.orbs.size(); ++i) {
            this.addToTop(new EvokeOrbAction(1));
        }
        this.isDone = true;
    }
}

