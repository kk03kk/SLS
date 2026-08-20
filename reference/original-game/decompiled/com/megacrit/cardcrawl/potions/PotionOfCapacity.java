/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.defect.IncreaseMaxOrbAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class PotionOfCapacity
extends AbstractPotion {
    public static final String POTION_ID = "PotionOfCapacity";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("PotionOfCapacity");

    public PotionOfCapacity() {
        super(PotionOfCapacity.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.SPHERE, AbstractPotion.PotionColor.BLUE);
        this.labOutlineColor = Settings.BLUE_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = PotionOfCapacity.potionStrings.DESCRIPTIONS[0] + this.potency + PotionOfCapacity.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new IncreaseMaxOrbAction(this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 2;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new PotionOfCapacity();
    }
}

