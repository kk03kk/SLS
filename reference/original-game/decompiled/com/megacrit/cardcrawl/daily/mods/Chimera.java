/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Chimera
extends AbstractDailyMod {
    public static final String ID = "Chimera";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Chimera");
    public static final String NAME = Chimera.modStrings.NAME;
    public static final String DESC = Chimera.modStrings.DESCRIPTION;

    public Chimera() {
        super(ID, NAME, DESC, "chimera.png", true);
    }
}

