/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class Shield
extends AbstractBlight {
    public static final String ID = "ToughEnemies";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("ToughEnemies");
    public static final String NAME = Shield.blightStrings.NAME;
    public static final String[] DESC = Shield.blightStrings.DESCRIPTION;
    public float toughMod = 1.5f;

    public Shield() {
        super(ID, NAME, DESC[0] + 50 + DESC[1], "shield.png", true);
        this.counter = 1;
    }

    @Override
    public void incrementUp() {
        this.toughMod += 0.5f;
        ++this.increment;
        ++this.counter;
        this.description = DESC[0] + (int)((this.toughMod - 1.0f) * 100.0f) + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public float effectFloat() {
        return this.toughMod;
    }
}

