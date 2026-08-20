/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.defect.EvokeAllOrbsAction;
import com.megacrit.cardcrawl.actions.defect.RemoveAllOrbsAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FissionAction
extends AbstractGameAction {
    private boolean upgraded = false;

    public FissionAction(boolean upgraded) {
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.ENERGY;
        this.upgraded = upgraded;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_XFAST) {
            int orbCount = AbstractDungeon.player.filledOrbCount();
            this.addToTop(new DrawCardAction(AbstractDungeon.player, orbCount));
            this.addToTop(new GainEnergyAction(orbCount));
            if (this.upgraded) {
                this.addToTop(new EvokeAllOrbsAction());
            } else {
                this.addToTop(new RemoveAllOrbsAction());
            }
        }
        this.tickDuration();
    }
}

