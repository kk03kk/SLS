/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class BlockPotion
extends AbstractPotion {
    public static final String POTION_ID = "Block Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Block Potion");

    public BlockPotion() {
        super(BlockPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.S, AbstractPotion.PotionColor.BLUE);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = BlockPotion.potionStrings.DESCRIPTIONS[0] + this.potency + BlockPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.BLOCK.NAMES[0]), GameDictionary.keywords.get(GameDictionary.BLOCK.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, this.potency));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 12;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new BlockPotion();
    }
}

