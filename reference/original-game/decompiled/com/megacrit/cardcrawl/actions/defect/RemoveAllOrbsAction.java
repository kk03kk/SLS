/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class RemoveAllOrbsAction
extends AbstractGameAction {
    public RemoveAllOrbsAction() {
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
    }

    @Override
    public void update() {
        while (AbstractDungeon.player.filledOrbCount() > 0) {
            AbstractDungeon.player.removeNextOrb();
        }
        this.isDone = true;
    }
}

