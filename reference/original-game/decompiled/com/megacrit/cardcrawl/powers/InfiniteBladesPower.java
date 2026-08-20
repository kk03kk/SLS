/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class InfiniteBladesPower
extends AbstractPower {
    public static final String POWER_ID = "Infinite Blades";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Infinite Blades");

    public InfiniteBladesPower(AbstractCreature owner, int bladeAmt) {
        this.name = InfiniteBladesPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = bladeAmt;
        this.updateDescription();
        this.loadRegion("infiniteBlades");
    }

    @Override
    public void atStartOfTurn() {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            this.addToBot(new MakeTempCardInHandAction((AbstractCard)new Shiv(), this.amount, false));
        }
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount > 1 ? InfiniteBladesPower.powerStrings.DESCRIPTIONS[0] + this.amount + InfiniteBladesPower.powerStrings.DESCRIPTIONS[1] : InfiniteBladesPower.powerStrings.DESCRIPTIONS[0] + this.amount + InfiniteBladesPower.powerStrings.DESCRIPTIONS[2];
    }
}

