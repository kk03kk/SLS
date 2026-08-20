/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDHotHotPower
extends AbstractPower {
    public static final String POWER_ID = "HotHot";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("HotHot");
    public static final String NAME = DEPRECATEDHotHotPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DEPRECATEDHotHotPower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDHotHotPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("corruption");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.type != DamageInfo.DamageType.THORNS && info.type != DamageInfo.DamageType.HP_LOSS && info.owner != null && info.owner != this.owner && damageAmount > 0 && !this.owner.hasPower("Buffer")) {
            this.flash();
            AbstractDungeon.actionManager.addToTop(new DamageAction(info.owner, new DamageInfo(this.owner, this.amount, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.FIRE, true));
        }
        return damageAmount;
    }
}

