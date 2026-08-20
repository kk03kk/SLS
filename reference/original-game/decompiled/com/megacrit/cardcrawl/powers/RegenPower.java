/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.unique.RegenAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class RegenPower
extends AbstractPower {
    public static final String POWER_ID = "Regeneration";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Regeneration");
    public static final String NAME = RegenPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = RegenPower.powerStrings.DESCRIPTIONS;

    public RegenPower(AbstractCreature owner, int heal) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = heal;
        this.updateDescription();
        this.loadRegion("regen");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flashWithoutSound();
        this.addToTop(new RegenAction(this.owner, this.amount));
    }

    @Override
    public void stackPower(int stackAmount) {
        super.stackPower(stackAmount);
    }
}

