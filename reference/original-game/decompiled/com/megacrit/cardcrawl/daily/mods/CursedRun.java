/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class CursedRun
extends AbstractDailyMod {
    public static final String ID = "Cursed Run";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Cursed Run");
    public static final String NAME = CursedRun.modStrings.NAME;
    public static final String DESC = CursedRun.modStrings.DESCRIPTION;

    public CursedRun() {
        super(ID, NAME, DESC, "cursed_run.png", false);
    }
}

