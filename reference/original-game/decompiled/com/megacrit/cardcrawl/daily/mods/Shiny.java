/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Shiny
extends AbstractDailyMod {
    public static final String ID = "Shiny";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Shiny");
    public static final String NAME = Shiny.modStrings.NAME;
    public static final String DESC = Shiny.modStrings.DESCRIPTION;

    public Shiny() {
        super(ID, NAME, DESC, "shiny.png", true);
    }
}

