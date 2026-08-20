/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class SadisticPower
extends AbstractPower {
    public static final String POWER_ID = "Sadistic";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Sadistic");
    public static final String NAME = SadisticPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = SadisticPower.powerStrings.DESCRIPTIONS;

    public SadisticPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("sadistic");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void onApplyPower(AbstractPower power, AbstractCreature target, AbstractCreature source) {
        if (power.type == AbstractPower.PowerType.DEBUFF && !power.ID.equals("Shackled") && source == this.owner && target != this.owner && !target.hasPower("Artifact")) {
            this.flash();
            this.addToBot(new DamageAction(target, new DamageInfo(this.owner, this.amount, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.FIRE));
        }
    }
}

