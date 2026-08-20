/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.campfire;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CampfireDigEffect
extends AbstractGameEffect {
    private static final float DUR = 2.0f;
    private boolean hasDug = false;
    private Color screenColor = AbstractDungeon.fadeColor.cpy();

    public CampfireDigEffect() {
        this.duration = 2.0f;
        this.screenColor.a = 0.0f;
        ((RestRoom)AbstractDungeon.getCurrRoom()).cutFireSound();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.updateBlackScreenColor();
        if (this.duration < 1.0f && !this.hasDug) {
            this.hasDug = true;
            CardCrawlGame.sound.play("SHOVEL");
            AbstractDungeon.getCurrRoom().rewards.clear();
            AbstractDungeon.getCurrRoom().rewards.add(new RewardItem(AbstractDungeon.returnRandomRelic(AbstractDungeon.returnRandomRelicTier())));
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
            AbstractDungeon.combatRewardScreen.open();
            CardCrawlGame.metricData.addCampfireChoiceData("DIG");
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
            ((RestRoom)AbstractDungeon.getCurrRoom()).fadeIn();
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
        }
    }

    private void updateBlackScreenColor() {
        this.screenColor.a = this.duration > 1.5f ? Interpolation.fade.apply(1.0f, 0.0f, (this.duration - 1.5f) * 2.0f) : (this.duration < 1.0f ? Interpolation.fade.apply(0.0f, 1.0f, this.duration) : 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    @Override
    public void dispose() {
    }
}

