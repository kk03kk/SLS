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
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShopSpeechBubble
extends AbstractGameEffect {
    private static final int RAW_W = 512;
    private static final float SHADOW_OFFSET = 16.0f * Settings.scale;
    private static final float WAVY_DISTANCE = 2.0f * Settings.scale;
    private static final float ADJUST_X = 170.0f * Settings.scale;
    private static final float ADJUST_Y = 116.0f * Settings.scale;
    public static final float FADE_TIME = 0.3f;
    private float shadow_offset = 0.0f;
    private float x;
    private float y;
    private float wavy_y;
    private float wavyHelper;
    private float scaleTimer = 0.3f;
    private boolean facingRight;
    public Hitbox hb;
    private Color shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    public ShopSpeechBubble(float x, float y, String msg, boolean isPlayer) {
        this(x, y, 2.0f, msg, isPlayer);
    }

    public ShopSpeechBubble(float x, float y, float duration, String msg, boolean isPlayer) {
        this.x = isPlayer ? x + ADJUST_X : x - ADJUST_X;
        this.y = y + ADJUST_Y;
        this.scale = Settings.scale / 2.0f;
        this.color = new Color(0.8f, 0.9f, 0.9f, 0.0f);
        this.duration = duration;
        this.facingRight = !isPlayer;
        this.hb = !this.facingRight ? new Hitbox(x, y, 350.0f * Settings.scale, 270.0f * Settings.scale) : new Hitbox(x - 350.0f * Settings.scale, y, 350.0f * Settings.scale, 270.0f * Settings.scale);
    }

    @Override
    public void update() {
        this.updateScale();
        this.hb.update();
        this.wavyHelper += Gdx.graphics.getDeltaTime() * 4.0f;
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
        this.scale = Settings.isMobile ? Interpolation.swingIn.apply(Settings.scale * 1.15f, Settings.scale / 2.0f, this.scaleTimer / 0.3f) : Interpolation.swingIn.apply(Settings.scale, Settings.scale / 2.0f, this.scaleTimer / 0.3f);
    }

    @Override
    public void render(SpriteBatch sb) {
        this.shadowColor.a = this.color.a / 4.0f;
        sb.setColor(this.shadowColor);
        sb.draw(ImageMaster.SHOP_SPEECH_BUBBLE_IMG, this.x - 256.0f + this.shadow_offset, this.y - 256.0f - this.shadow_offset + this.wavy_y, 256.0f, 256.0f, 512.0f, 512.0f, this.scale, this.scale, this.rotation, 0, 0, 512, 512, this.facingRight, false);
        sb.setColor(this.color);
        sb.draw(ImageMaster.SHOP_SPEECH_BUBBLE_IMG, this.x - 256.0f, this.y - 256.0f + this.wavy_y, 256.0f, 256.0f, 512.0f, 512.0f, this.scale, this.scale, this.rotation, 0, 0, 512, 512, this.facingRight, false);
        this.hb.render(sb);
    }

    @Override
    public void dispose() {
    }
}

