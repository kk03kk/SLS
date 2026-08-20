/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class NightTerrors
extends AbstractDailyMod {
    public static final String ID = "Night Terrors";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Night Terrors");
    public static final String NAME = NightTerrors.modStrings.NAME;
    public static final String DESC = NightTerrors.modStrings.DESCRIPTION;
    public static final float HEAL_AMT = 1.0f;
    public static final int MAX_HP_LOSS = 5;

    public NightTerrors() {
        super(ID, NAME, DESC, "night_terrors.png", false);
    }
}

