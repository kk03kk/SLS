/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.campfire;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.vfx.campfire.CampfireRecallEffect;

public class RecallOption
extends AbstractCampfireOption {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Recall Option");
    public static final String[] TEXT = RecallOption.uiStrings.TEXT;

    public RecallOption() {
        this.label = TEXT[0];
        this.description = TEXT[1];
        this.img = ImageMaster.CAMPFIRE_RECALL_BUTTON;
    }

    @Override
    public void useOption() {
        AbstractDungeon.effectList.add(new CampfireRecallEffect());
    }
}

