/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class BottledMiracle
extends AbstractPotion {
    public static final String POTION_ID = "BottledMiracle";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("BottledMiracle");

    public BottledMiracle() {
        super(BottledMiracle.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.BOTTLE, AbstractPotion.PotionColor.ENERGY);
        this.labOutlineColor = Settings.PURPLE_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = BottledMiracle.potionStrings.DESCRIPTIONS[0] + this.potency + BottledMiracle.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new MakeTempCardInHandAction((AbstractCard)new Miracle(), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 2;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new BottledMiracle();
    }
}

