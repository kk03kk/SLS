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
import com.megacrit.cardcrawl.powers.RegenPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class RegenPotion
extends AbstractPotion {
    public static final String POTION_ID = "Regen Potion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Regen Potion");

    public RegenPotion() {
        super(RegenPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.HEART, AbstractPotion.PotionColor.WHITE);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = RegenPotion.potionStrings.DESCRIPTIONS[0] + this.potency + RegenPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.REGEN.NAMES[0]), GameDictionary.keywords.get(GameDictionary.REGEN.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new RegenPower(AbstractDungeon.player, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 5;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new RegenPotion();
    }
}

