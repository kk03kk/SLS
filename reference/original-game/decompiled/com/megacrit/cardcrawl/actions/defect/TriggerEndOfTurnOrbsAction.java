/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class TriggerEndOfTurnOrbsAction
extends AbstractGameAction {
    @Override
    public void update() {
        if (!AbstractDungeon.player.orbs.isEmpty()) {
            for (AbstractOrb o : AbstractDungeon.player.orbs) {
                o.onEndOfTurn();
            }
            if (AbstractDungeon.player.hasRelic("Cables") && !(AbstractDungeon.player.orbs.get(0) instanceof EmptyOrbSlot)) {
                AbstractDungeon.player.orbs.get(0).onEndOfTurn();
            }
        }
        this.isDone = true;
    }
}

