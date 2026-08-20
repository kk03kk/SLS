/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ResurrectPower
extends AbstractPower {
    public static final String POWER_ID = "Life Link";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Life Link");
    public static final String NAME = ResurrectPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ResurrectPower.powerStrings.DESCRIPTIONS;

    public ResurrectPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("regrow");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

