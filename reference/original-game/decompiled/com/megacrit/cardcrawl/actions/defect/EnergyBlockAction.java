/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class EnergyBlockAction
extends AbstractGameAction {
    private boolean upg = false;

    public EnergyBlockAction(boolean upgraded) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.upg = upgraded;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.upg) {
                this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, EnergyPanel.totalCount * 2));
            } else {
                this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, EnergyPanel.totalCount));
            }
        }
        this.tickDuration();
    }
}

