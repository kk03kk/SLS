/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.Frost;

public class IceWallAction
extends AbstractGameAction {
    private int perOrbAmt;

    public IceWallAction(int blockAmt, int perOrbAmt) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = blockAmt;
        this.perOrbAmt = perOrbAmt;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            int count = 0;
            for (int i = 0; i < AbstractDungeon.player.orbs.size(); ++i) {
                if (!(AbstractDungeon.player.orbs.get(i) instanceof Frost)) continue;
                ++count;
            }
            this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, this.amount + count * this.perOrbAmt));
        }
        this.tickDuration();
    }
}

