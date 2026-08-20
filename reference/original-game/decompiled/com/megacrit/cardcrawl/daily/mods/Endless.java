/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Endless
extends AbstractDailyMod {
    public static final String ID = "Endless";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Endless");
    public static final String NAME = Endless.modStrings.NAME;
    public static final String DESC = Endless.modStrings.DESCRIPTION;

    public Endless() {
        super(ID, NAME, DESC, "endless.png", true);
    }
}

