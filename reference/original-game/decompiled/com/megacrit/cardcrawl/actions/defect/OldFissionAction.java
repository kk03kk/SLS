/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.actions.defect.IncreaseMaxOrbAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class OldFissionAction
extends AbstractGameAction {
    public OldFissionAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            int orbCount = AbstractDungeon.player.filledOrbCount();
            for (int i = 0; i < orbCount; ++i) {
                this.addToBot(new AnimateOrbAction(1));
                this.addToBot(new EvokeOrbAction(1));
            }
            this.addToBot(new IncreaseMaxOrbAction(orbCount));
        }
        this.tickDuration();
    }
}

