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
import com.megacrit.cardcrawl.powers.StrengthPower;

public class AngryPower
extends AbstractPower {
    public static final String POWER_ID = "Angry";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Angry");
    public static final String NAME = AngryPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = AngryPower.powerStrings.DESCRIPTIONS;

    public AngryPower(AbstractCreature owner, int attackAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = attackAmount;
        this.updateDescription();
        this.isPostActionPower = true;
        this.loadRegion("anger");
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.owner != null && damageAmount > 0 && info.type != DamageInfo.DamageType.HP_LOSS && info.type != DamageInfo.DamageType.THORNS) {
            this.addToTop(new ApplyPowerAction(this.owner, this.owner, new StrengthPower(this.owner, this.amount), this.amount));
            this.flash();
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }
}

