/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class BlasterAction
extends AbstractGameAction {
    public BlasterAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            int counter = 0;
            for (AbstractOrb o : AbstractDungeon.player.orbs) {
                if (o instanceof EmptyOrbSlot) continue;
                ++counter;
            }
            if (counter != 0) {
                this.addToBot(new GainEnergyAction(counter));
            }
        }
        this.tickDuration();
    }
}

