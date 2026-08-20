/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class EnergyPotion
extends AbstractPotion {
    public static final String POTION_ID = "Energy Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Energy Potion");

    public EnergyPotion() {
        super(EnergyPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.BOLT, AbstractPotion.PotionColor.ENERGY);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = EnergyPotion.potionStrings.DESCRIPTIONS[0] + this.potency + EnergyPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        this.addToBot(new GainEnergyAction(this.potency));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 2;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new EnergyPotion();
    }
}

