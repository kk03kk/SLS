/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.DuplicationPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class DuplicationPotion
extends AbstractPotion {
    public static final String POTION_ID = "DuplicationPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("DuplicationPotion");

    public DuplicationPotion() {
        super(DuplicationPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.CARD, AbstractPotion.PotionEffect.RAINBOW, Color.WHITE, null, null);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic("SacredBark") ? DuplicationPotion.potionStrings.DESCRIPTIONS[0] : DuplicationPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new DuplicationPower(AbstractDungeon.player, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 1;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new DuplicationPotion();
    }
}

