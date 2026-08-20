/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class ReactivePower
extends AbstractPower {
    public static final String POWER_ID = "Compulsive";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Compulsive");
    public static final String NAME = ReactivePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ReactivePower.powerStrings.DESCRIPTIONS;

    public ReactivePower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("reactive");
        this.priority = 50;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS && info.type != DamageInfo.DamageType.THORNS && damageAmount > 0 && damageAmount < this.owner.currentHealth) {
            this.flash();
            this.addToBot(new RollMoveAction((AbstractMonster)this.owner));
        }
        return damageAmount;
    }
}

