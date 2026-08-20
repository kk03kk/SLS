/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PoisonPower;

public class EnvenomPower
extends AbstractPower {
    public static final String POWER_ID = "Envenom";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Envenom");
    public static final String NAME = EnvenomPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = EnvenomPower.powerStrings.DESCRIPTIONS;

    public EnvenomPower(AbstractCreature owner, int newAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = newAmount;
        this.updateDescription();
        this.loadRegion("envenom");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
        if (damageAmount > 0 && target != this.owner && info.type == DamageInfo.DamageType.NORMAL) {
            this.flash();
            this.addToTop(new ApplyPowerAction(target, this.owner, (AbstractPower)new PoisonPower(target, this.owner, this.amount), this.amount, true));
        }
    }
}

