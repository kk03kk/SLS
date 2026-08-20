/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.VulnerablePower;

public class FearPotion
extends AbstractPotion {
    public static final String POTION_ID = "FearPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("FearPotion");

    public FearPotion() {
        super(FearPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.H, AbstractPotion.PotionColor.FEAR);
        this.isThrown = true;
        this.targetRequired = true;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = FearPotion.potionStrings.DESCRIPTIONS[0] + this.potency + FearPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.VULNERABLE.NAMES[0]), GameDictionary.keywords.get(GameDictionary.VULNERABLE.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new VulnerablePower(target, this.potency, false), this.potency));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new FearPotion();
    }
}

