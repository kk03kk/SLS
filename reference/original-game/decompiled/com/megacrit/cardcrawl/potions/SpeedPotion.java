/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.LoseDexterityPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class SpeedPotion
extends AbstractPotion {
    public static final String POTION_ID = "SpeedPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("SpeedPotion");

    public SpeedPotion() {
        super(SpeedPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.BOLT, AbstractPotion.PotionColor.SKILL);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = SpeedPotion.potionStrings.DESCRIPTIONS[0] + this.potency + SpeedPotion.potionStrings.DESCRIPTIONS[1] + this.potency + SpeedPotion.potionStrings.DESCRIPTIONS[2];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.DEXTERITY.NAMES[0]), GameDictionary.keywords.get(GameDictionary.DEXTERITY.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new DexterityPower(target, this.potency), this.potency));
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new LoseDexterityPower(target, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 5;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new SpeedPotion();
    }
}

