/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.PlayTopCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class DistilledChaosPotion
extends AbstractPotion {
    public static final String POTION_ID = "DistilledChaos";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("DistilledChaos");

    public DistilledChaosPotion() {
        super(DistilledChaosPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.T, AbstractPotion.PotionEffect.RAINBOW, Color.WHITE, null, null);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = DistilledChaosPotion.potionStrings.DESCRIPTIONS[0] + this.potency + DistilledChaosPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        for (int i = 0; i < this.potency; ++i) {
            this.addToBot(new PlayTopCardAction(AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true, AbstractDungeon.cardRandomRng), false));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new DistilledChaosPotion();
    }
}

