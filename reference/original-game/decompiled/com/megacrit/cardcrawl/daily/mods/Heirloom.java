/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Heirloom
extends AbstractDailyMod {
    public static final String ID = "Heirloom";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Heirloom");
    public static final String NAME = Heirloom.modStrings.NAME;
    public static final String DESC = Heirloom.modStrings.DESCRIPTION;

    public Heirloom() {
        super(ID, NAME, DESC, "heirloom.png", true);
    }
}

