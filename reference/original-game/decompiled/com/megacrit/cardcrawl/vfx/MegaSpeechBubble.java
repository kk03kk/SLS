/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.MegaDialogTextEffect;

public class MegaSpeechBubble
extends AbstractGameEffect {
    private static final int RAW_W = 512;
    private float shadow_offset = 0.0f;
    private static final float SHADOW_OFFSET = 16.0f * Settings.scale;
    private float x;
    private float y;
    private float scale_x;
    private float scale_y;
    private float wavy_y;
    private float wavyHelper;
    private static final float WAVY_DISTANCE = 2.0f * Settings.scale;
    private static final float SCALE_TIME = 0.3f;
    private float scaleTimer = 0.3f;
    private static final float ADJUST_X = 170.0f * Settings.scale;
    private static final float ADJUST_Y = 116.0f * Settings.scale;
    private boolean facingRight;
    private static final float DEFAULT_DURATION = 2.0f;
    private static final float FADE_TIME = 0.3f;

    public MegaSpeechBubble(float x, float y, String msg, boolean isPlayer) {
        this(x, y, 2.0f, msg, isPlayer);
    }

    public MegaSpeechBubble(float x, float y, float duration, String msg, boolean isPlayer) {
        float effect_x = -170.0f * Settings.scale;
        if (isPlayer) {
            effect_x = 170.0f * Settings.scale;
        }
        AbstractDungeon.effectsQueue.add(new MegaDialogTextEffect(x + effect_x, y + 124.0f * Settings.scale, duration, msg, DialogWord.AppearEffect.BUMP_IN));
        this.x = isPlayer ? x + ADJUST_X : x - ADJUST_X;
        this.y = y + ADJUST_Y;
        this.scale_x = Settings.scale * 0.7f;
        this.scale_y = Settings.scale * 0.7f;
        this.scaleTimer = 0.3f;
        this.color = new Color(0.8f, 0.9f, 0.9f, 0.0f);
        this.duration = duration;
        this.facingRight = !isPlayer;
    }

    @Override
    public void update() {
        this.updateScale();
        this.wavyHelper += Gdx.graphics.getDeltaTime() * 5.0f;
        this.wavy_y = MathUtils.sin(this.wavyHelper) * WAVY_DISTANCE;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration > 0.3f ? MathUtils.lerp(this.color.a, 1.0f, Gdx.graphics.getDeltaTime() * 12.0f) : MathUtils.lerp(this.color.a, 0.0f, Gdx.graphics.getDeltaTime() * 12.0f);
        this.shadow_offset = MathUtils.lerp(this.shadow_offset, SHADOW_OFFSET, Gdx.graphics.getDeltaTime() * 4.0f);
    }

    private void updateScale() {
        this.scaleTimer -= Gdx.graphics.getDeltaTime();
        if (this.scaleTimer < 0.0f) {
            this.scaleTimer = 0.0f;
        }
        if (Settings.isMobile) {
            this.scale_x = Interpolation.swingIn.apply(this.scale_x * 1.15f, Settings.scale, this.scaleTimer / 0.3f);
            this.scale_y = Interpolation.swingIn.apply(this.scale_y * 1.15f, Settings.scale, this.scaleTimer / 0.3f);
        } else {
            this.scale_x = Interpolation.swingIn.apply(this.scale_x, Settings.scale, this.scaleTimer / 0.3f);
            this.scale_y = Interpolation.swingIn.apply(this.scale_y, Settings.scale, this.scaleTimer / 0.3f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.color.a / 4.0f));
        sb.draw(ImageMaster.SPEECH_BUBBLE_IMG, this.x - 256.0f + this.shadow_offset, this.y - 256.0f - this.shadow_offset + this.wavy_y, 256.0f, 256.0f, 512.0f, 512.0f, this.scale_x, this.scale_y, this.rotation, 0, 0, 512, 512, this.facingRight, false);
        sb.setColor(this.color);
        sb.draw(ImageMaster.SPEECH_BUBBLE_IMG, this.x - 256.0f, this.y - 256.0f + this.wavy_y, 256.0f, 256.0f, 512.0f, 512.0f, this.scale_x, this.scale_y, this.rotation, 0, 0, 512, 512, this.facingRight, false);
    }

    @Override
    public void dispose() {
    }
}

