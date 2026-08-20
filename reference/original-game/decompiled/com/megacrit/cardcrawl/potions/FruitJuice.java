/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.WeMeetAgain;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class FruitJuice
extends AbstractPotion {
    public static final String POTION_ID = "Fruit Juice";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Fruit Juice");

    public FruitJuice() {
        super(FruitJuice.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.HEART, AbstractPotion.PotionColor.FRUIT);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = FruitJuice.potionStrings.DESCRIPTIONS[0] + this.potency + FruitJuice.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        AbstractDungeon.player.increaseMaxHp(this.potency, true);
    }

    @Override
    public boolean canUse() {
        if (AbstractDungeon.actionManager.turnHasEnded && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            return false;
        }
        return AbstractDungeon.getCurrRoom().event == null || !(AbstractDungeon.getCurrRoom().event instanceof WeMeetAgain);
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 5;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new FruitJuice();
    }
}

