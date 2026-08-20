/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class NeowsLament
extends AbstractRelic {
    public static final String ID = "NeowsBlessing";

    public NeowsLament() {
        super(ID, "lament.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
        this.counter = 3;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        if (this.counter > 0) {
            --this.counter;
            if (this.counter == 0) {
                this.setCounter(-2);
                this.description = this.DESCRIPTIONS[1];
                this.tips.clear();
                this.tips.add(new PowerTip(this.name, this.description));
                this.initializeTips();
            }
            this.flash();
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                m.currentHealth = 1;
                m.healthBarUpdatedEvent();
            }
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        }
    }

    @Override
    public void setCounter(int setCounter) {
        this.counter = setCounter;
        if (setCounter <= 0) {
            this.usedUp();
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new NeowsLament();
    }
}

