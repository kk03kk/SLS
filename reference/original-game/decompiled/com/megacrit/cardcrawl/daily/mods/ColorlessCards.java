/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class ColorlessCards
extends AbstractDailyMod {
    public static final String ID = "Colorless Cards";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Colorless Cards");
    public static final String NAME = ColorlessCards.modStrings.NAME;
    public static final String DESC = ColorlessCards.modStrings.DESCRIPTION;

    public ColorlessCards() {
        super(ID, NAME, DESC, "colorless.png", true);
    }
}

