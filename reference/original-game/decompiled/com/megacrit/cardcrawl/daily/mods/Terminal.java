/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Terminal
extends AbstractDailyMod {
    public static final String ID = "Terminal";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Terminal");
    public static final String NAME = Terminal.modStrings.NAME;
    public static final String DESC = Terminal.modStrings.DESCRIPTION;
    public static final int ARMOR_AMT = 5;

    public Terminal() {
        super(ID, NAME, DESC, "tough_enemies.png", false);
    }
}

