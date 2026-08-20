/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class BlackBlood
extends AbstractRelic {
    public static final String ID = "Black Blood";

    public BlackBlood() {
        super(ID, "blackBlood.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 12 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onVictory() {
        this.flash();
        AbstractPlayer p = AbstractDungeon.player;
        this.addToTop(new RelicAboveCreatureAction(p, this));
        if (p.currentHealth > 0) {
            p.heal(12);
        }
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.hasRelic("Burning Blood");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BlackBlood();
    }
}

