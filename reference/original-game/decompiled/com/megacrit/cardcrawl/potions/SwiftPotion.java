/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class SwiftPotion
extends AbstractPotion {
    public static final String POTION_ID = "Swift Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Swift Potion");

    public SwiftPotion() {
        super(SwiftPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.H, AbstractPotion.PotionColor.SWIFT);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = SwiftPotion.potionStrings.DESCRIPTIONS[1] + this.potency + SwiftPotion.potionStrings.DESCRIPTIONS[2];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        this.addToBot(new DrawCardAction(AbstractDungeon.player, this.potency));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new SwiftPotion();
    }
}

