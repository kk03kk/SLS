/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;

public class UnhoverCardAction
extends AbstractGameAction {
    public UnhoverCardAction() {
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST && AbstractDungeon.player.hoveredCard != null) {
            AbstractDungeon.effectList.add(new ExhaustCardEffect(AbstractDungeon.player.hoveredCard));
            AbstractDungeon.player.hoveredCard = null;
        }
    }
}

