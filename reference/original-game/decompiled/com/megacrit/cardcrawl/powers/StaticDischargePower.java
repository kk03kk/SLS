/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class StaticDischargePower
extends AbstractPower {
    public static final String POWER_ID = "StaticDischarge";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("StaticDischarge");

    public StaticDischargePower(AbstractCreature owner, int lightningAmount) {
        this.name = StaticDischargePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = lightningAmount;
        this.updateDescription();
        this.loadRegion("static_discharge");
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.type != DamageInfo.DamageType.THORNS && info.type != DamageInfo.DamageType.HP_LOSS && info.owner != null && info.owner != this.owner && damageAmount > 0) {
            this.flash();
            for (int i = 0; i < this.amount; ++i) {
                this.addToTop(new ChannelAction(new Lightning()));
            }
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = StaticDischargePower.powerStrings.DESCRIPTIONS[0] + this.amount + StaticDischargePower.powerStrings.DESCRIPTIONS[1];
    }
}

