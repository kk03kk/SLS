/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class BlightChests
extends AbstractDailyMod {
    public static final String ID = "Blight Chests";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Blight Chests");
    public static final String NAME = BlightChests.modStrings.NAME;
    public static final String DESC = BlightChests.modStrings.DESCRIPTION;

    public BlightChests() {
        super(ID, NAME, DESC, "endless.png", true);
    }
}

