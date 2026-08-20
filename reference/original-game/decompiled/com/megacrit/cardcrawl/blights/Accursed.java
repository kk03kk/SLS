/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Accursed
extends AbstractBlight {
    public static final String ID = "Accursed";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("Accursed");
    public static final String NAME = Accursed.blightStrings.NAME;
    public static final String[] DESC = Accursed.blightStrings.DESCRIPTION;

    public Accursed() {
        super(ID, NAME, DESC[0] + 2 + DESC[1], "accursed.png", false);
        this.counter = 2;
    }

    @Override
    public void stack() {
        this.counter += 2;
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
    public void onBossDefeat() {
        this.flash();
        Random posRandom = new Random();
        for (int i = 0; i < this.counter; ++i) {
            AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(AbstractDungeon.getCardWithoutRng(AbstractCard.CardRarity.CURSE), (float)Settings.WIDTH / 2.0f + posRandom.random(-((float)Settings.WIDTH / 3.0f), (float)Settings.WIDTH / 3.0f), (float)Settings.HEIGHT / 2.0f + posRandom.random(-((float)Settings.HEIGHT / 3.0f), (float)Settings.HEIGHT / 3.0f)));
        }
    }
}

