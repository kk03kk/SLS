/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Insanity
extends AbstractDailyMod {
    public static final String ID = "Insanity";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Insanity");
    public static final String NAME = Insanity.modStrings.NAME;
    public static final String DESC = Insanity.modStrings.DESCRIPTION;

    public Insanity() {
        super(ID, NAME, DESC, "restless_journey.png", false);
    }
}

