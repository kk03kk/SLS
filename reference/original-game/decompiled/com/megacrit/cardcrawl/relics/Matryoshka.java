/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Matryoshka
extends AbstractRelic {
    public static final String ID = "Matryoshka";

    public Matryoshka() {
        super(ID, "matryoshka.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.SOLID);
        this.counter = 2;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onChestOpen(boolean bossChest) {
        if (!bossChest && this.counter > 0) {
            --this.counter;
            this.flash();
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            if (AbstractDungeon.relicRng.randomBoolean(0.75f)) {
                AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.COMMON);
            } else {
                AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
            }
            if (this.counter == 0) {
                this.setCounter(-2);
                this.description = this.DESCRIPTIONS[2];
            } else {
                this.description = this.DESCRIPTIONS[1];
            }
        }
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (setCounter == -2) {
            this.usedUp();
            this.counter = -2;
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 40;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Matryoshka();
    }
}

