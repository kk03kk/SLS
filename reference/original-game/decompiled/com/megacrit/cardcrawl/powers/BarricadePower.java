/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BarricadePower
extends AbstractPower {
    public static final String POWER_ID = "Barricade";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Barricade");
    public static final String NAME = BarricadePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = BarricadePower.powerStrings.DESCRIPTIONS;

    public BarricadePower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("barricade");
    }

    @Override
    public void updateDescription() {
        this.description = this.owner.isPlayer ? DESCRIPTIONS[0] : DESCRIPTIONS[1];
    }
}

