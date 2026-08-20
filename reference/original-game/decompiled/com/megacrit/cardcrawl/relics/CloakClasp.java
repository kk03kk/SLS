/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class CloakClasp
extends AbstractRelic {
    public static final String ID = "CloakClasp";

    public CloakClasp() {
        super(ID, "clasp.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onPlayerEndTurn() {
        if (!AbstractDungeon.player.hand.group.isEmpty()) {
            this.flash();
            this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, null, AbstractDungeon.player.hand.group.size() * 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CloakClasp();
    }
}

