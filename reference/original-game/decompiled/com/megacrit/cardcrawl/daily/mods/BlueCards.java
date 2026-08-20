/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class BlueCards
extends AbstractDailyMod {
    public static final String ID = "Blue Cards";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Blue Cards");
    public static final String NAME = BlueCards.modStrings.NAME;
    public static final String DESC = BlueCards.modStrings.DESCRIPTION;

    public BlueCards() {
        super(ID, NAME, DESC, "blue.png", true, AbstractPlayer.PlayerClass.DEFECT);
    }
}

