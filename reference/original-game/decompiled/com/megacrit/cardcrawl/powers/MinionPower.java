/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MinionPower
extends AbstractPower {
    public static final String POWER_ID = "Minion";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Minion");
    public static final String NAME = MinionPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = MinionPower.powerStrings.DESCRIPTIONS;

    public MinionPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("minion");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}

