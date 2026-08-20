/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.unique.ArmamentsAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class BlessingOfTheForge
extends AbstractPotion {
    public static final String POTION_ID = "BlessingOfTheForge";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("BlessingOfTheForge");

    public BlessingOfTheForge() {
        super(BlessingOfTheForge.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.COMMON, AbstractPotion.PotionSize.ANVIL, AbstractPotion.PotionColor.FIRE);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = BlessingOfTheForge.potionStrings.DESCRIPTIONS[0];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.UPGRADE.NAMES[0]), GameDictionary.keywords.get(GameDictionary.UPGRADE.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ArmamentsAction(true));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 0;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new BlessingOfTheForge();
    }
}

