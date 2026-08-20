/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class AnimateOrbAction
extends AbstractGameAction {
    private int orbCount;

    public AnimateOrbAction(int amount) {
        this.orbCount = amount;
    }

    @Override
    public void update() {
        for (int i = 0; i < this.orbCount; ++i) {
            AbstractDungeon.player.triggerEvokeAnimation(i);
        }
        this.isDone = true;
    }
}

