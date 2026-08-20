/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class NirvanaPower
extends AbstractPower {
    public static final String POWER_ID = "Nirvana";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Nirvana");

    public NirvanaPower(AbstractCreature owner, int amt) {
        this.name = NirvanaPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("nirvana");
    }

    @Override
    public void updateDescription() {
        this.description = NirvanaPower.powerStrings.DESCRIPTIONS[0] + this.amount + NirvanaPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void onScry() {
        this.flash();
        this.addToBot(new GainBlockAction(this.owner, this.amount));
    }
}

