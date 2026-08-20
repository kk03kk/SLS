/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MetallicizePower
extends AbstractPower {
    public static final String POWER_ID = "Metallicize";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Metallicize");
    public static final String NAME = MetallicizePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = MetallicizePower.powerStrings.DESCRIPTIONS;

    public MetallicizePower(AbstractCreature owner, int armorAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = armorAmt;
        this.updateDescription();
        this.loadRegion("armor");
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_METALLICIZE", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = this.owner.isPlayer ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[3];
    }

    @Override
    public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
        this.flash();
        this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
    }
}

