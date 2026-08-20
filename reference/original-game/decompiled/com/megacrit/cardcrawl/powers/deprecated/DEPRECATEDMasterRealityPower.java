/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageRandomEnemyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDMasterRealityPower
extends AbstractPower {
    public static final String POWER_ID = "MasterRealityPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("MasterRealityPower");

    public DEPRECATEDMasterRealityPower(AbstractCreature owner, int amt) {
        this.name = DEPRECATEDMasterRealityPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("master_smite");
    }

    @Override
    public void onAfterCardPlayed(AbstractCard card) {
        if (card.retain || card.selfRetain) {
            this.flash();
            this.addToBot(new DamageRandomEnemyAction(new DamageInfo(null, this.amount), AbstractGameAction.AttackEffect.FIRE));
        }
    }

    @Override
    public void updateDescription() {
        this.description = DEPRECATEDMasterRealityPower.powerStrings.DESCRIPTIONS[0] + this.amount + DEPRECATEDMasterRealityPower.powerStrings.DESCRIPTIONS[1];
    }
}

