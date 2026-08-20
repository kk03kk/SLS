/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.unique.GamblingChipAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class GamblersBrew
extends AbstractPotion {
    public static final String POTION_ID = "GamblersBrew";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("GamblersBrew");

    public GamblersBrew() {
        super(GamblersBrew.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.S, AbstractPotion.PotionColor.SMOKE);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.description = GamblersBrew.potionStrings.DESCRIPTIONS[0];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (!AbstractDungeon.player.hand.isEmpty()) {
            this.addToBot(new GamblingChipAction(AbstractDungeon.player, true));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 0;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new GamblersBrew();
    }
}

