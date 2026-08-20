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
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;

public class CampfireLiftEffect
extends AbstractGameEffect {
    private static final float DUR = 2.0f;
    private boolean hasTrained = false;
    private Color screenColor = AbstractDungeon.fadeColor.cpy();

    public CampfireLiftEffect() {
        this.duration = 2.0f;
        this.screenColor.a = 0.0f;
        ((RestRoom)AbstractDungeon.getCurrRoom()).cutFireSound();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.updateBlackScreenColor();
        if (this.duration < 1.0f && !this.hasTrained) {
            this.hasTrained = true;
            if (AbstractDungeon.player.hasRelic("Girya")) {
                AbstractDungeon.player.getRelic("Girya").flash();
                ++AbstractDungeon.player.getRelic((String)"Girya").counter;
                CardCrawlGame.sound.play("ATTACK_HEAVY");
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, true);
                CardCrawlGame.metricData.addCampfireChoiceData("LIFT", Integer.toString(AbstractDungeon.player.getRelic((String)"Girya").counter));
            }
            AbstractDungeon.topLevelEffects.add(new BorderFlashEffect(new Color(0.8f, 0.6f, 0.1f, 0.0f)));
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

