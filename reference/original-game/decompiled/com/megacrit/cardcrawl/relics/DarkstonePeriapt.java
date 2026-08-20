/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class DarkstonePeriapt
extends AbstractRelic {
    public static final String ID = "Darkstone Periapt";
    private static final int HP_AMT = 6;

    public DarkstonePeriapt() {
        super(ID, "darkstone.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public void onObtainCard(AbstractCard card) {
        if (card.color == AbstractCard.CardColor.CURSE) {
            if (ModHelper.isModEnabled("Hoarder")) {
                AbstractDungeon.player.increaseMaxHp(6, true);
                AbstractDungeon.player.increaseMaxHp(6, true);
            }
            AbstractDungeon.player.increaseMaxHp(6, true);
        }
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 6 + LocalizedStrings.PERIOD;
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new DarkstonePeriapt();
    }
}

