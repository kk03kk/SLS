/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BurningBlood
extends AbstractRelic {
    public static final String ID = "Burning Blood";
    private static final int HEALTH_AMT = 6;

    public BurningBlood() {
        super(ID, "burningBlood.png", AbstractRelic.RelicTier.STARTER, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 6 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onVictory() {
        this.flash();
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        AbstractPlayer p = AbstractDungeon.player;
        if (p.currentHealth > 0) {
            p.heal(6);
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BurningBlood();
    }
}

