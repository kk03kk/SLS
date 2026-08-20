/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class FairyPotion
extends AbstractPotion {
    public static final String POTION_ID = "FairyPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("FairyPotion");

    public FairyPotion() {
        super(FairyPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.FAIRY, AbstractPotion.PotionColor.FAIRY);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = FairyPotion.potionStrings.DESCRIPTIONS[0] + this.potency + FairyPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        float percent = (float)this.potency / 100.0f;
        int healAmt = (int)((float)AbstractDungeon.player.maxHealth * percent);
        if (healAmt < 1) {
            healAmt = 1;
        }
        AbstractDungeon.player.heal(healAmt, true);
        AbstractDungeon.topPanel.destroyPotion(this.slot);
    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 30;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new FairyPotion();
    }
}

