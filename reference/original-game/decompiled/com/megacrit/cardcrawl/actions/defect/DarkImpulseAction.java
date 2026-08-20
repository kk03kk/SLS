/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.Dark;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class DarkImpulseAction
extends AbstractGameAction {
    public DarkImpulseAction() {
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST && !AbstractDungeon.player.orbs.isEmpty()) {
            for (AbstractOrb o : AbstractDungeon.player.orbs) {
                if (!(o instanceof Dark)) continue;
                o.onStartOfTurn();
                o.onEndOfTurn();
            }
            if (AbstractDungeon.player.hasRelic("Cables") && !(AbstractDungeon.player.orbs.get(0) instanceof EmptyOrbSlot) && AbstractDungeon.player.orbs.get(0) instanceof Dark) {
                AbstractDungeon.player.orbs.get(0).onStartOfTurn();
                AbstractDungeon.player.orbs.get(0).onEndOfTurn();
            }
        }
        this.tickDuration();
    }
}

