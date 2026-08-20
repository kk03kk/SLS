/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class Scatterbrain
extends AbstractBlight {
    public static final String ID = "Scatterbrain";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("Scatterbrain");
    public static final String NAME = Scatterbrain.blightStrings.NAME;
    public static final String[] DESC = Scatterbrain.blightStrings.DESCRIPTION;

    public Scatterbrain() {
        super(ID, NAME, DESC[0] + 1 + DESC[1], "scatter.png", false);
        this.counter = 1;
    }

    @Override
    public void stack() {
        --AbstractDungeon.player.masterHandSize;
        ++this.counter;
        this.updateDescription();
        this.flash();
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0] + this.counter + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onEquip() {
        --AbstractDungeon.player.masterHandSize;
    }
}

