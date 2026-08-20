/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Midas
extends AbstractDailyMod {
    public static final String ID = "Midas";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Midas");
    public static final String NAME = Midas.modStrings.NAME;
    public static final String DESC = Midas.modStrings.DESCRIPTION;
    public static final float MULTIPLIER = 2.0f;

    public Midas() {
        super(ID, NAME, DESC, "midas.png", false);
    }
}

