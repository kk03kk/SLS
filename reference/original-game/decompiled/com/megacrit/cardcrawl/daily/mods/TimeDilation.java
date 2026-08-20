/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class TimeDilation
extends AbstractDailyMod {
    public static final String ID = "Time Dilation";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Time Dilation");
    public static final String NAME = TimeDilation.modStrings.NAME;
    public static final String DESC = TimeDilation.modStrings.DESCRIPTION;

    public TimeDilation() {
        super(ID, NAME, DESC, "slow_start.png", true);
    }
}

