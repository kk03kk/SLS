/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class RedCards
extends AbstractDailyMod {
    public static final String ID = "Red Cards";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Red Cards");
    public static final String NAME = RedCards.modStrings.NAME;
    public static final String DESC = RedCards.modStrings.DESCRIPTION;

    public RedCards() {
        super(ID, NAME, DESC, "red.png", true, AbstractPlayer.PlayerClass.IRONCLAD);
    }
}

