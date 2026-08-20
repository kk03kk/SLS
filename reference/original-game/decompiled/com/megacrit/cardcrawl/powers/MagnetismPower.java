/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MagnetismPower
extends AbstractPower {
    public static final String POWER_ID = "Magnetism";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Magnetism");
    public static final String NAME = MagnetismPower.powerStrings.NAME;
    public static final String SINGULAR_DESCRIPTION = MagnetismPower.powerStrings.DESCRIPTIONS[0];
    public static final String PLURAL_DESCRIPTION = MagnetismPower.powerStrings.DESCRIPTIONS[1];

    public MagnetismPower(AbstractCreature owner, int cardAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = cardAmount;
        this.updateDescription();
        this.loadRegion("magnet");
    }

    @Override
    public void atStartOfTurn() {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            for (int i = 0; i < this.amount; ++i) {
                this.addToBot(new MakeTempCardInHandAction(AbstractDungeon.returnTrulyRandomColorlessCardInCombat().makeCopy(), 1, false));
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
        this.description = this.amount > 1 ? String.format(PLURAL_DESCRIPTION, this.amount) : String.format(SINGULAR_DESCRIPTION, this.amount);
    }
}

