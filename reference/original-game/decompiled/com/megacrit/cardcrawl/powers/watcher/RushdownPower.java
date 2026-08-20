/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.stances.AbstractStance;

public class RushdownPower
extends AbstractPower {
    public static final String POWER_ID = "Adaptation";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Adaptation");

    public RushdownPower(AbstractCreature owner, int amount) {
        this.name = RushdownPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("rushdown");
    }

    @Override
    public void updateDescription() {
        this.description = this.amount > 1 ? RushdownPower.powerStrings.DESCRIPTIONS[0] + this.amount + RushdownPower.powerStrings.DESCRIPTIONS[2] : RushdownPower.powerStrings.DESCRIPTIONS[0] + this.amount + RushdownPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void onChangeStance(AbstractStance oldStance, AbstractStance newStance) {
        if (!oldStance.ID.equals(newStance.ID) && newStance.ID.equals("Wrath")) {
            this.flash();
            this.addToBot(new DrawCardAction(this.owner, this.amount));
        }
    }
}

