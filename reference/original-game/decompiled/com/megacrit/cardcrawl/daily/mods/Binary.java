/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Binary
extends AbstractDailyMod {
    public static final String ID = "Binary";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Binary");
    public static final String NAME = Binary.modStrings.NAME;
    public static final String DESC = Binary.modStrings.DESCRIPTION;

    public Binary() {
        super(ID, NAME, DESC, "binary.png", false);
    }
}

