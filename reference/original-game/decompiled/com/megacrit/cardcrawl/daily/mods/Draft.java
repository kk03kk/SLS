/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class Draft
extends AbstractDailyMod {
    public static final String ID = "Draft";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Draft");
    public static final String NAME = Draft.modStrings.NAME;
    public static final String DESC = Draft.modStrings.DESCRIPTION;

    public Draft() {
        super(ID, NAME, DESC, "draft.png", true);
    }
}

