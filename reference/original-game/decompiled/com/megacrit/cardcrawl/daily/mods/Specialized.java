/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Specialized
extends AbstractDailyMod {
    public static final String ID = "Specialized";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Specialized");
    public static final String NAME = Specialized.modStrings.NAME;
    public static final String DESC = Specialized.modStrings.DESCRIPTION;

    public Specialized() {
        super(ID, NAME, DESC, "specialized.png", true);
    }
}

