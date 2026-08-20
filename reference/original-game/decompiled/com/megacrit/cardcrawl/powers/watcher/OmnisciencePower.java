/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class OmnisciencePower
extends AbstractPower {
    public static final String POWER_ID = "OmnisciencePower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("OmnisciencePower");

    public OmnisciencePower(AbstractCreature owner, int newAmount) {
        this.name = OmnisciencePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("devotion");
    }

    @Override
    public void updateDescription() {
        this.description = OmnisciencePower.powerStrings.DESCRIPTIONS[0] + this.amount + OmnisciencePower.powerStrings.DESCRIPTIONS[1];
    }
}

