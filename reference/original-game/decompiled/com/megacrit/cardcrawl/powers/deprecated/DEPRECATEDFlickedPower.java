/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDFlickedPower
extends AbstractPower {
    public static final String POWER_ID = "FlickPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("FlickPower");
    private static final int FLICK_DMG = 50;

    public DEPRECATEDFlickedPower(AbstractCreature owner, int amt) {
        this.name = DEPRECATEDFlickedPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("talk_to_hand");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void stackPower(int stackAmount) {
        this.amount += stackAmount;
        if (this.amount >= 3) {
            this.addToBot(new DamageAction(this.owner, new DamageInfo(null, 50, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.BLUNT_HEAVY, true));
            this.addToBot(new RemoveSpecificPowerAction(this.owner, null, POWER_ID));
        } else {
            this.fontScale = 8.0f;
            this.updateDescription();
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[0] + DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[1] + 50 + DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[3] : DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[0] + DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[2] + 50 + DEPRECATEDFlickedPower.powerStrings.DESCRIPTIONS[3];
    }
}

