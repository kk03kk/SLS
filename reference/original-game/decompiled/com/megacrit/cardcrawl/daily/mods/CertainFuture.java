/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class CertainFuture
extends AbstractDailyMod {
    public static final String ID = "Uncertain Future";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Uncertain Future");
    public static final String NAME = CertainFuture.modStrings.NAME;
    public static final String DESC = CertainFuture.modStrings.DESCRIPTION;

    public CertainFuture() {
        super(ID, NAME, DESC, "certain_future.png", false);
    }
}

