/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.localization.RunModStrings;

public class SealedDeck
extends AbstractDailyMod {
    public static final String ID = "SealedDeck";
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString("SealedDeck");
    public static final String NAME = SealedDeck.modStrings.NAME;
    public static final String DESC = SealedDeck.modStrings.DESCRIPTION;

    public SealedDeck() {
        super(ID, NAME, DESC, "sealed_deck.png", true);
    }
}

