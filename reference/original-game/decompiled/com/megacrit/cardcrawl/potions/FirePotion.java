/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class FirePotion
extends AbstractPotion {
    public static final String POTION_ID = "Fire Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Fire Potion");

    public FirePotion() {
        super(FirePotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.SPHERE, AbstractPotion.PotionColor.FIRE);
        this.isThrown = true;
        this.targetRequired = true;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = FirePotion.potionStrings.DESCRIPTIONS[0] + this.potency + FirePotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        DamageInfo info = new DamageInfo(AbstractDungeon.player, this.potency, DamageInfo.DamageType.THORNS);
        info.applyEnemyPowersOnly(target);
        this.addToBot(new DamageAction(target, info, AbstractGameAction.AttackEffect.FIRE));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 20;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new FirePotion();
    }
}

