/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class MimicInfestation
extends AbstractBlight {
    public static final String ID = "MimicInfestation";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("MimicInfestation");
    public static final String NAME = MimicInfestation.blightStrings.NAME;
    public static final String[] DESC = MimicInfestation.blightStrings.DESCRIPTION;

    public MimicInfestation() {
        super(ID, NAME, DESC[0], "mimic.png", true);
    }
}

