/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class RedoAction
extends AbstractGameAction {
    private AbstractOrb orb;

    public RedoAction() {
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (!AbstractDungeon.player.orbs.isEmpty()) {
            this.orb = AbstractDungeon.player.orbs.get(0);
            if (this.orb instanceof EmptyOrbSlot) {
                this.isDone = true;
            } else {
                this.addToTop(new ChannelAction(this.orb, false));
                this.addToTop(new EvokeOrbAction(1));
            }
        }
        this.isDone = true;
    }
}

