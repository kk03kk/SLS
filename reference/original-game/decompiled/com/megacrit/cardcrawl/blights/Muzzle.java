/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class Muzzle
extends AbstractBlight {
    public static final String ID = "FullBelly";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("FullBelly");
    public static final String NAME = Muzzle.blightStrings.NAME;
    public static final String[] DESC = Muzzle.blightStrings.DESCRIPTION;

    public Muzzle() {
        super(ID, NAME, DESC[0], "muzzle.png", true);
    }
}

