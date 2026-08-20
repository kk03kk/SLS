/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Colossus
extends AbstractDailyMod {
    public static final String ID = "MonsterHunter";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("MonsterHunter");
    public static final String NAME = Colossus.modStrings.NAME;
    public static final String DESC = Colossus.modStrings.DESCRIPTION;
    public static final float modAmount = 1.5f;

    public Colossus() {
        super(ID, NAME, DESC, "colossus.png", false);
    }
}

