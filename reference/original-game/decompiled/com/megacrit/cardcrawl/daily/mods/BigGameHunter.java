/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class BigGameHunter
extends AbstractDailyMod {
    public static final String ID = "Elite Swarm";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("Elite Swarm");
    public static final String NAME = BigGameHunter.modStrings.NAME;
    public static final String DESC = BigGameHunter.modStrings.DESCRIPTION;

    public BigGameHunter() {
        super(ID, NAME, DESC, "elite_swarm.png", false);
    }
}

