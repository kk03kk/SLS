/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ElectroPower
extends AbstractPower {
    public static final String POWER_ID = "Electro";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Electro");
    public static final String NAME = ElectroPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ElectroPower.powerStrings.DESCRIPTIONS;

    public ElectroPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("mastery");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

