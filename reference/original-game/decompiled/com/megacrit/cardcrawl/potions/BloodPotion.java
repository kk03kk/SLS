/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.WeMeetAgain;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class BloodPotion
extends AbstractPotion {
    public static final String POTION_ID = "BloodPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("BloodPotion");

    public BloodPotion() {
        super(BloodPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.H, AbstractPotion.PotionColor.WHITE);
        this.labOutlineColor = Settings.RED_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = BloodPotion.potionStrings.DESCRIPTIONS[0] + this.potency + BloodPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new HealAction(AbstractDungeon.player, AbstractDungeon.player, (int)((float)AbstractDungeon.player.maxHealth * ((float)this.potency / 100.0f))));
        } else {
            AbstractDungeon.player.heal((int)((float)AbstractDungeon.player.maxHealth * ((float)this.potency / 100.0f)));
        }
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
        return 20;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new BloodPotion();
    }
}

