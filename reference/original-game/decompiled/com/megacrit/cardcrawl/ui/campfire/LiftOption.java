/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.campfire;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.vfx.campfire.CampfireLiftEffect;

public class LiftOption
extends AbstractCampfireOption {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Lift Option");
    public static final String[] TEXT = LiftOption.uiStrings.TEXT;

    public LiftOption(boolean active) {
        this.label = TEXT[0];
        this.usable = active;
        this.description = active ? TEXT[1] : TEXT[2];
        this.img = ImageMaster.CAMPFIRE_TRAIN_BUTTON;
    }

    @Override
    public void useOption() {
        if (this.usable) {
            AbstractDungeon.effectList.add(new CampfireLiftEffect());
        }
    }
}

