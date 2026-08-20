/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.TintEffect;

public class DynamicBanner {
    public static final int RAW_W = 1112;
    public static final int RAW_H = 238;
    private static final float Y_OFFSET = -50.0f * Settings.scale;
    private static final float ANIM_TIME = 0.5f;
    private static final float LERP_SPEED = 9.0f;
    private static final Color TEXT_SHOW_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color IDLE_COLOR = new Color(0.7f, 0.7f, 0.7f, 1.0f);
    private static final Color FADE_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private String label;
    private float animateTimer = 0.0f;
    public float y;
    public float targetY;
    public float startY;
    public float scale;
    public static final float Y = (float)Settings.HEIGHT / 2.0f + 260.0f * Settings.scale;
    protected TintEffect tint = new TintEffect();
    protected TintEffect textTint = new TintEffect();
    public boolean pressed = false;
    public boolean isMoving = false;
    public boolean show = false;
    public boolean isLarge = false;
    public int height;
    public int width;

    public DynamicBanner() {
        this.tint.color.a = 0.0f;
        this.textTint.color.a = 0.0f;
    }

    public void appear() {
        this.appear(Y, this.label);
    }

    public void appear(String label) {
        this.appear(Y, label);
    }

    public void appearInstantly(String label) {
        this.appearInstantly(Y, label);
    }

    public void appear(float y, String label) {
        this.startY = y + Y_OFFSET;
        this.y = y + Y_OFFSET;
        this.targetY = y;
        this.label = label;
        this.scale = 0.25f;
        this.pressed = false;
        this.isMoving = true;
        this.show = true;
        this.animateTimer = 0.5f;
        this.tint.changeColor(IDLE_COLOR, 9.0f);
        this.textTint.changeColor(TEXT_SHOW_COLOR, 9.0f);
    }

    public void appearInstantly(float y, String label) {
        this.isMoving = false;
        this.animateTimer = 0.0f;
        this.y = y;
        this.targetY = y;
        this.scale = 1.2f;
        this.label = label;
        this.pressed = false;
        this.show = true;
        this.tint.changeColor(IDLE_COLOR, 9.0f);
        this.textTint.changeColor(TEXT_SHOW_COLOR, 9.0f);
    }

    public void hide() {
        this.show = false;
        this.isMoving = false;
        this.tint.changeColor(FADE_COLOR, 18.0f);
        this.textTint.changeColor(FADE_COLOR, 18.0f);
    }

    public void update() {
        this.tint.update();
        this.textTint.update();
        if (this.show) {
            this.animateTimer -= Gdx.graphics.getDeltaTime();
            if (this.animateTimer < 0.0f) {
                this.animateTimer = 0.0f;
                this.isMoving = false;
            } else {
                this.y = Interpolation.swingOut.apply(this.startY, this.targetY, (0.5f - this.animateTimer) * 2.0f);
                this.scale = Interpolation.swingOut.apply(0.0f, 1.2f, (0.5f - this.animateTimer) * 2.0f);
                if (this.scale <= 0.0f) {
                    this.scale = 0.01f;
                }
            }
            this.tint.changeColor(IDLE_COLOR, 9.0f);
        }
    }

    public void render(SpriteBatch sb) {
        if (this.textTint.color.a != 0.0f && this.label != null) {
            sb.setColor(this.tint.color);
            sb.draw(ImageMaster.VICTORY_BANNER, (float)Settings.WIDTH / 2.0f - 556.0f, this.y - 119.0f, 556.0f, 119.0f, 1112.0f, 238.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1112, 238, false, false);
            FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, this.label, (float)Settings.WIDTH / 2.0f, this.y + 22.0f * Settings.scale, this.textTint.color, this.scale);
        }
    }
}

