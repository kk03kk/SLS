/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Vintage
extends AbstractDailyMod {
    public static final String ID = "Vintage";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Vintage");
    public static final String NAME = Vintage.modStrings.NAME;
    public static final String DESC = Vintage.modStrings.DESCRIPTION;

    public Vintage() {
        super(ID, NAME, DESC, "vintage.png", true);
    }
}

