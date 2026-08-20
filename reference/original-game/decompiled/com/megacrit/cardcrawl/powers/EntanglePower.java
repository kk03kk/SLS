/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class EntanglePower
extends AbstractPower {
    public static final String POWER_ID = "Entangled";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Entangled");

    public EntanglePower(AbstractCreature owner) {
        this.name = EntanglePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 1;
        this.updateDescription();
        this.loadRegion("entangle");
        this.isTurnBased = true;
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_ENTANGLED", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = EntanglePower.powerStrings.DESCRIPTIONS[0];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
    }
}

