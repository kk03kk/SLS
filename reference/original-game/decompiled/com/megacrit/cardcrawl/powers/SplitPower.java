/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SplitPower
extends AbstractPower {
    public static final String POWER_ID = "Split";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Split");
    public static final String NAME = SplitPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = SplitPower.powerStrings.DESCRIPTIONS;

    public SplitPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("split");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + FontHelper.colorString(this.owner.name, "y") + DESCRIPTIONS[1];
    }
}

