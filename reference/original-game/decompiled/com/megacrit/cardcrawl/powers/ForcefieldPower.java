/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ForcefieldPower
extends AbstractPower {
    public static final String POWER_ID = "Nullify Attack";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Nullify Attack");
    public static final String NAME = ForcefieldPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ForcefieldPower.powerStrings.DESCRIPTIONS;

    public ForcefieldPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1;
        this.updateDescription();
        this.loadRegion("forcefield");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
        if (damage > 0.0f && type != DamageInfo.DamageType.HP_LOSS && type != DamageInfo.DamageType.THORNS) {
            return 0.0f;
        }
        return damage;
    }
}

