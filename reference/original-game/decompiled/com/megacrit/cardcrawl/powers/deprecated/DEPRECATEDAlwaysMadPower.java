/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDAlwaysMadPower
extends AbstractPower {
    public static final String POWER_ID = "AlwaysMad";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("AlwaysMad");
    public static final String[] DESCRIPTIONS = DEPRECATEDAlwaysMadPower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDAlwaysMadPower(AbstractCreature owner) {
        this.name = DEPRECATEDAlwaysMadPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("anger");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

