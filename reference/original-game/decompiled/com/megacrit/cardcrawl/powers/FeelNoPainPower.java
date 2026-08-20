/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class FeelNoPainPower
extends AbstractPower {
    public static final String POWER_ID = "Feel No Pain";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Feel No Pain");
    public static final String NAME = FeelNoPainPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = FeelNoPainPower.powerStrings.DESCRIPTIONS;

    public FeelNoPainPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("noPain");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void onExhaust(AbstractCard card) {
        this.flash();
        this.addToBot(new GainBlockAction(this.owner, this.amount, Settings.FAST_MODE));
    }
}

