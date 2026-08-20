/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class DeadlyEvents
extends AbstractDailyMod {
    public static final String ID = "DeadlyEvents";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("DeadlyEvents");
    public static final String NAME = DeadlyEvents.modStrings.NAME;
    public static final String DESC = DeadlyEvents.modStrings.DESCRIPTION;

    public DeadlyEvents() {
        super(ID, NAME, DESC, "deadly_events.png", false);
    }
}

