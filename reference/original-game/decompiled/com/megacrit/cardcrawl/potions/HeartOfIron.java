/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class HeartOfIron
extends AbstractPotion {
    public static final String POTION_ID = "HeartOfIron";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("HeartOfIron");

    public HeartOfIron() {
        super(HeartOfIron.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.HEART, AbstractPotion.PotionColor.SWIFT);
        this.labOutlineColor = Settings.RED_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = HeartOfIron.potionStrings.DESCRIPTIONS[0] + this.potency + HeartOfIron.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new MetallicizePower(target, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 6;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new HeartOfIron();
    }
}

