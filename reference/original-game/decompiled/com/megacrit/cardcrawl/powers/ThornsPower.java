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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThornsPower
extends AbstractPower {
    private static final Logger logger = LogManager.getLogger(ThornsPower.class.getName());
    public static final String POWER_ID = "Thorns";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Thorns");
    public static final String NAME = ThornsPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = ThornsPower.powerStrings.DESCRIPTIONS;

    public ThornsPower(AbstractCreature owner, int thornsDamage) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = thornsDamage;
        this.updateDescription();
        this.loadRegion("thorns");
    }

    @Override
    public void stackPower(int stackAmount) {
        if (this.amount == -1) {
            logger.info(this.name + " does not stack");
            return;
        }
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        this.updateDescription();
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.type != DamageInfo.DamageType.THORNS && info.type != DamageInfo.DamageType.HP_LOSS && info.owner != null && info.owner != this.owner) {
            this.flash();
            this.addToTop(new DamageAction(info.owner, new DamageInfo(this.owner, this.amount, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL, true));
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}

