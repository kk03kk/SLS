/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class HelloPower
extends AbstractPower {
    public static final String POWER_ID = "Hello";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Hello");
    public static final String NAME = HelloPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = HelloPower.powerStrings.DESCRIPTIONS;

    public HelloPower(AbstractCreature owner, int cardAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = cardAmt;
        this.updateDescription();
        this.loadRegion("hello");
    }

    @Override
    public void atStartOfTurn() {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            for (int i = 0; i < this.amount; ++i) {
                this.addToBot(new MakeTempCardInHandAction(AbstractDungeon.getCard(AbstractCard.CardRarity.COMMON, AbstractDungeon.cardRandomRng).makeCopy(), 1, false));
            }
        }
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
    }

    @Override
    public void updateDescription() {
        this.description = this.amount > 1 ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
    }
}

