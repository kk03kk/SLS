/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;

public class EndTurnDeathPower
extends AbstractPower {
    public static final String POWER_ID = "EndTurnDeath";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("EndTurnDeath");

    public EndTurnDeathPower(AbstractCreature owner) {
        this.name = EndTurnDeathPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("end_turn_death");
    }

    @Override
    public void updateDescription() {
        this.description = EndTurnDeathPower.powerStrings.DESCRIPTIONS[0];
    }

    @Override
    public void atStartOfTurn() {
        this.flash();
        this.addToBot(new VFXAction(new LightningEffect(this.owner.hb.cX, this.owner.hb.cY)));
        this.addToBot(new LoseHPAction(this.owner, this.owner, 99999));
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
    }
}

