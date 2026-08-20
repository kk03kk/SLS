/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class FocusPotion
extends AbstractPotion {
    public static final String POTION_ID = "FocusPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("FocusPotion");

    public FocusPotion() {
        super(FocusPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.S, AbstractPotion.PotionColor.SWIFT);
        this.labOutlineColor = Settings.BLUE_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = FocusPotion.potionStrings.DESCRIPTIONS[0] + this.potency + FocusPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.FOCUS.NAMES[0]), GameDictionary.keywords.get(GameDictionary.FOCUS.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new FocusPower(target, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 2;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new FocusPotion();
    }
}

