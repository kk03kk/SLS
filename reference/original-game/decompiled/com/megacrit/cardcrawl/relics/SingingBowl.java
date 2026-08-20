/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class SingingBowl
extends AbstractRelic {
    public static final String ID = "Singing Bowl";
    public static final int HP_AMT = 2;

    public SingingBowl() {
        super(ID, "singingBowl.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void update() {
        super.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.playA("SINGING_BOWL", MathUtils.random(-0.2f, 0.1f));
            this.flash();
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new SingingBowl();
    }
}

