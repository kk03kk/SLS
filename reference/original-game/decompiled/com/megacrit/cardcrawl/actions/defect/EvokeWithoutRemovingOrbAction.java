/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EvokeWithoutRemovingOrbAction
extends AbstractGameAction {
    private int orbCount;

    public EvokeWithoutRemovingOrbAction(int amount) {
        this.startDuration = Settings.FAST_MODE ? Settings.ACTION_DUR_FAST : Settings.ACTION_DUR_XFAST;
        this.duration = this.startDuration;
        this.orbCount = amount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            for (int i = 0; i < this.orbCount; ++i) {
                AbstractDungeon.player.evokeWithoutLosingOrb();
            }
        }
        this.tickDuration();
    }
}

