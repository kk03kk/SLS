/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.ObtainPotionAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.WeMeetAgain;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.ObtainPotionEffect;

public class EntropicBrew
extends AbstractPotion {
    public static final String POTION_ID = "EntropicBrew";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("EntropicBrew");

    public EntropicBrew() {
        super(EntropicBrew.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.M, AbstractPotion.PotionEffect.RAINBOW, Color.WHITE, null, null);
        this.description = EntropicBrew.potionStrings.DESCRIPTIONS[0];
        this.potency = this.getPotency();
        this.isThrown = false;
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            for (int i = 0; i < AbstractDungeon.player.potionSlots; ++i) {
                this.addToBot(new ObtainPotionAction(AbstractDungeon.returnRandomPotion(true)));
            }
        } else if (AbstractDungeon.player.hasRelic("Sozu")) {
            AbstractDungeon.player.getRelic("Sozu").flash();
        } else {
            for (int i = 0; i < AbstractDungeon.player.potionSlots; ++i) {
                AbstractDungeon.effectsQueue.add(new ObtainPotionEffect(AbstractDungeon.returnRandomPotion()));
            }
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
        if (AbstractDungeon.player != null) {
            return AbstractDungeon.player.potionSlots;
        }
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new EntropicBrew();
    }
}

