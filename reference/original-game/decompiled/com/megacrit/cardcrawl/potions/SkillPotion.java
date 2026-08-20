/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.unique.DiscoveryAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class SkillPotion
extends AbstractPotion {
    public static final String POTION_ID = "SkillPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("SkillPotion");

    public SkillPotion() {
        super(SkillPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.CARD, AbstractPotion.PotionColor.GREEN);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic("SacredBark") ? SkillPotion.potionStrings.DESCRIPTIONS[0] : SkillPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        this.addToBot(new DiscoveryAction(AbstractCard.CardType.SKILL, this.potency));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 1;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new SkillPotion();
    }
}

