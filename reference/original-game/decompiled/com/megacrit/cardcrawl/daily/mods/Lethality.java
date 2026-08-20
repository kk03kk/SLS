/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Lethality
extends AbstractDailyMod {
    public static final String ID = "Lethality";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Lethality");
    public static final String NAME = Lethality.modStrings.NAME;
    public static final String DESC = Lethality.modStrings.DESCRIPTION;
    public static final int STR_AMT = 3;

    public Lethality() {
        super(ID, NAME, DESC, "lethal_enemies.png", false);
    }
}

