/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ConstrictedPower
extends AbstractPower {
    public static final String POWER_ID = "Constricted";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Constricted");
    public static final String NAME = ConstrictedPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ConstrictedPower.powerStrings.DESCRIPTIONS;
    public AbstractCreature source;

    public ConstrictedPower(AbstractCreature target, AbstractCreature source, int fadeAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = target;
        this.source = source;
        this.amount = fadeAmt;
        this.updateDescription();
        this.loadRegion("constricted");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.priority = 105;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_CONSTRICTED", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        this.flashWithoutSound();
        this.playApplyPowerSfx();
        this.addToBot(new DamageAction(this.owner, new DamageInfo(this.source, this.amount, DamageInfo.DamageType.THORNS)));
    }
}

