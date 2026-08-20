/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.campfire;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.vfx.campfire.CampfireSleepEffect;
import com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect;

public class RestOption
extends AbstractCampfireOption {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Rest Option");
    public static final String[] TEXT = RestOption.uiStrings.TEXT;

    public RestOption(boolean active) {
        this.label = TEXT[0];
        this.usable = active;
        int healAmt = ModHelper.isModEnabled("Night Terrors") ? (int)((float)AbstractDungeon.player.maxHealth * 1.0f) : (int)((float)AbstractDungeon.player.maxHealth * 0.3f);
        if (Settings.isEndless && AbstractDungeon.player.hasBlight("FullBelly")) {
            healAmt /= 2;
        }
        if (ModHelper.isModEnabled("Night Terrors")) {
            this.description = TEXT[1] + healAmt + ")" + LocalizedStrings.PERIOD;
            if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
                this.description = this.description + "\n+15" + TEXT[2] + AbstractDungeon.player.getRelic((String)"Regal Pillow").name + LocalizedStrings.PERIOD;
            }
        } else {
            this.description = TEXT[3] + healAmt + ")" + LocalizedStrings.PERIOD;
            if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
                this.description = this.description + "\n+15" + TEXT[2] + AbstractDungeon.player.getRelic((String)"Regal Pillow").name + LocalizedStrings.PERIOD;
            }
        }
        this.updateUsability(active);
    }

    public void updateUsability(boolean canUse) {
        if (!canUse) {
            this.description = TEXT[4];
        }
        this.img = ImageMaster.CAMPFIRE_REST_BUTTON;
    }

    @Override
    public void useOption() {
        CardCrawlGame.sound.play("SLEEP_BLANKET");
        AbstractDungeon.effectList.add(new CampfireSleepEffect());
        for (int i = 0; i < 30; ++i) {
            AbstractDungeon.topLevelEffects.add(new CampfireSleepScreenCoverEffect());
        }
        ++CardCrawlGame.metricData.campfire_rested;
        CardCrawlGame.metricData.addCampfireChoiceData("REST");
    }
}

