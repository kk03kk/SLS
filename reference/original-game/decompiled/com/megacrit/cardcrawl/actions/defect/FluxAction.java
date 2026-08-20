/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.orbs.Plasma;

public class FluxAction
extends AbstractGameAction {
    public FluxAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (int i = 0; i < AbstractDungeon.player.orbs.size(); ++i) {
                if (AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot || AbstractDungeon.player.orbs.get(i) instanceof Plasma) continue;
                Plasma plasma = new Plasma();
                plasma.cX = AbstractDungeon.player.orbs.get((int)i).cX;
                plasma.cY = AbstractDungeon.player.orbs.get((int)i).cY;
                plasma.setSlot(i, AbstractDungeon.player.maxOrbs);
                AbstractDungeon.player.orbs.set(i, plasma);
            }
        }
        this.tickDuration();
    }
}

