/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDSerenityPower
extends AbstractPower {
    public static final String POWER_ID = "Serenity";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Serenity");
    public static final String NAME = DEPRECATEDSerenityPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = DEPRECATEDSerenityPower.powerStrings.DESCRIPTIONS;

    public DEPRECATEDSerenityPower(AbstractCreature owner, int amt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("platedarmor");
    }

    @Override
    public void playApplyPowerSfx() {
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + LocalizedStrings.PERIOD;
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (damageAmount > 0 && ((AbstractPlayer)this.owner).stance.ID.equals("Calm")) {
            this.flash();
            if ((damageAmount -= this.amount) < this.amount) {
                damageAmount = 0;
            }
        }
        return damageAmount;
    }
}

