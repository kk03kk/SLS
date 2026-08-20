/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class CurlUpPower
extends AbstractPower {
    public static final String POWER_ID = "Curl Up";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Curl Up");
    public static final String NAME = CurlUpPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = CurlUpPower.powerStrings.DESCRIPTIONS;
    private boolean triggered = false;

    public CurlUpPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
        this.loadRegion("closeUp");
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (!this.triggered && damageAmount < this.owner.currentHealth && damageAmount > 0 && info.owner != null && info.type == DamageInfo.DamageType.NORMAL) {
            this.flash();
            this.triggered = true;
            this.addToBot(new ChangeStateAction((AbstractMonster)this.owner, "CLOSED"));
            this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        }
        return damageAmount;
    }
}

