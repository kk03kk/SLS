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
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class SteroidPotion
extends AbstractPotion {
    public static final String POTION_ID = "SteroidPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("SteroidPotion");

    public SteroidPotion() {
        super(SteroidPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.FAIRY, AbstractPotion.PotionColor.STEROID);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = SteroidPotion.potionStrings.DESCRIPTIONS[0] + this.potency + SteroidPotion.potionStrings.DESCRIPTIONS[1] + this.potency + SteroidPotion.potionStrings.DESCRIPTIONS[2];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.STRENGTH.NAMES[0]), GameDictionary.keywords.get(GameDictionary.STRENGTH.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new StrengthPower(target, this.potency), this.potency));
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new LoseStrengthPower(target, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 5;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new SteroidPotion();
    }
}

