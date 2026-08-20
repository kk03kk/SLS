/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class MeatOnTheBone
extends AbstractRelic {
    public static final String ID = "Meat on the Bone";
    private static final int HEAL_AMT = 12;

    public MeatOnTheBone() {
        super(ID, "meat.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 12 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onTrigger() {
        AbstractPlayer p = AbstractDungeon.player;
        if ((float)p.currentHealth <= (float)p.maxHealth / 2.0f && p.currentHealth > 0) {
            this.flash();
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            p.heal(12);
            this.stopPulse();
        }
    }

    @Override
    public void onBloodied() {
        this.flash();
        this.pulse = true;
    }

    @Override
    public void onNotBloodied() {
        this.stopPulse();
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new MeatOnTheBone();
    }
}

