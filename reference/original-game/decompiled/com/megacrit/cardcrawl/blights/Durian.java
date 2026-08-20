/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class Durian
extends AbstractBlight {
    public static final String ID = "BlightedDurian";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("BlightedDurian");
    public static final String NAME = Durian.blightStrings.NAME;
    public static final String[] DESC = Durian.blightStrings.DESCRIPTION;

    public Durian() {
        super(ID, NAME, DESC[0] + 50 + DESC[1], "durian.png", false);
        this.counter = 1;
    }

    @Override
    public void stack() {
        ++this.counter;
        AbstractDungeon.player.decreaseMaxHealth(AbstractDungeon.player.maxHealth / 2);
    }

    @Override
    public void onEquip() {
        AbstractDungeon.player.decreaseMaxHealth(AbstractDungeon.player.maxHealth / 2);
    }
}

