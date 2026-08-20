/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MasterRealityPower
extends AbstractPower {
    public static final String POWER_ID = "MasterRealityPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("MasterRealityPower");

    public MasterRealityPower(AbstractCreature owner) {
        this.name = MasterRealityPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("master_reality");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void updateDescription() {
        this.description = MasterRealityPower.powerStrings.DESCRIPTIONS[0];
    }
}

