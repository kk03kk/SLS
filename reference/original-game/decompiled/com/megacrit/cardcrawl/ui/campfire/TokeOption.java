/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.campfire;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.vfx.campfire.CampfireTokeEffect;

public class TokeOption
extends AbstractCampfireOption {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Toke Option");
    public static final String[] TEXT = TokeOption.uiStrings.TEXT;

    public TokeOption(boolean active) {
        this.label = TEXT[0];
        this.usable = active;
        this.description = TEXT[1];
        this.img = ImageMaster.CAMPFIRE_TOKE_BUTTON;
    }

    @Override
    public void useOption() {
        if (this.usable) {
            AbstractDungeon.effectList.add(new CampfireTokeEffect());
        }
    }
}

