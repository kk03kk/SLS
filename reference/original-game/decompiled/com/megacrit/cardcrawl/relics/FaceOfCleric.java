/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class FaceOfCleric
extends AbstractRelic {
    public static final String ID = "FaceOfCleric";

    public FaceOfCleric() {
        super(ID, "clericFace.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onVictory() {
        this.flash();
        AbstractDungeon.player.increaseMaxHp(1, true);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new FaceOfCleric();
    }
}

