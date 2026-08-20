/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Hoarder
extends AbstractDailyMod {
    public static final String ID = "Hoarder";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Hoarder");
    public static final String NAME = Hoarder.modStrings.NAME;
    public static final String DESC = Hoarder.modStrings.DESCRIPTION;

    public Hoarder() {
        super(ID, NAME, DESC, "greed.png", false);
    }
}

