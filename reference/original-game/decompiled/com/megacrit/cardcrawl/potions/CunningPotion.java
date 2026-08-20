/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class CunningPotion
extends AbstractPotion {
    public static final String POTION_ID = "CunningPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("CunningPotion");

    public CunningPotion() {
        super(CunningPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.SPIKY, AbstractPotion.PotionEffect.NONE, Color.GRAY, Color.DARK_GRAY, null);
        this.labOutlineColor = Settings.GREEN_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = CunningPotion.potionStrings.DESCRIPTIONS[0] + this.potency + CunningPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        Shiv shiv = new Shiv();
        ((AbstractCard)shiv).upgrade();
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new MakeTempCardInHandAction(shiv.makeStatEquivalentCopy(), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new CunningPotion();
    }
}

