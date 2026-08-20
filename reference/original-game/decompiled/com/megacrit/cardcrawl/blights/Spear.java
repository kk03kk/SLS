/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class Spear
extends AbstractBlight {
    public static final String ID = "DeadlyEnemies";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("DeadlyEnemies");
    public static final String NAME = Spear.blightStrings.NAME;
    public static final String[] DESC = Spear.blightStrings.DESCRIPTION;
    public float damageMod = 2.0f;

    public Spear() {
        super(ID, NAME, DESC[0] + 100 + DESC[1], "spear.png", true);
        this.counter = 1;
    }

    @Override
    public void incrementUp() {
        this.damageMod += 0.75f;
        ++this.increment;
        ++this.counter;
        this.description = DESC[0] + (int)((this.damageMod - 1.0f) * 100.0f) + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public float effectFloat() {
        return this.damageMod;
    }
}

