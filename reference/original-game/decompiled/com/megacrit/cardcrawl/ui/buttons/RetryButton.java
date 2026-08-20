/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.DrawMaster;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.TintEffect;

public class RetryButton {
    public static final int RAW_W = 512;
    private static final float BUTTON_W = 240.0f * Settings.scale;
    private static final float BUTTON_H = 160.0f * Settings.scale;
    private static final float LERP_SPEED = 9.0f;
    private static final Color TEXT_SHOW_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);
    private static final Color HIGHLIGHT_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Color IDLE_COLOR = new Color(0.7f, 0.7f, 0.7f, 1.0f);
    private static final Color FADE_COLOR = new Color(0.3f, 0.3f, 0.3f, 1.0f);
    public String label;
    public float x;
    public float y;
    public Hitbox hb;
    protected TintEffect tint = new TintEffect();
    protected TintEffect textTint = new TintEffect();
    public boolean pressed = false;
    public boolean isMoving = false;
    public boolean show = false;
    public int height;
    public int width;

    public RetryButton() {
        this.tint.color.a = 0.0f;
        this.textTint.color.a = 0.0f;
        this.hb = new Hitbox(-10000.0f, -10000.0f, BUTTON_W, BUTTON_H);
    }

    public void appear(float x, float y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
        this.pressed = false;
        this.isMoving = true;
        this.show = true;
        this.tint.changeColor(IDLE_COLOR, 9.0f);
        this.textTint.changeColor(TEXT_SHOW_COLOR, 9.0f);
    }

    public void hide() {
        this.show = false;
        this.isMoving = false;
        this.tint.changeColor(FADE_COLOR, 9.0f);
        this.textTint.changeColor(FADE_COLOR, 9.0f);
    }

    public void update() {
        this.tint.update();
        this.textTint.update();
        if (this.show) {
            this.hb.move(this.x, this.y);
            this.hb.update();
            if (InputHelper.justClickedLeft && this.hb.hovered) {
                this.hb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (this.hb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (this.hb.hovered) {
                this.tint.changeColor(HIGHLIGHT_COLOR, 18.0f);
            } else {
                this.tint.changeColor(IDLE_COLOR, 9.0f);
            }
            if (this.hb.clicked) {
                this.hb.clicked = false;
            }
        }
        if (this.textTint.color.a != 0.0f && this.label != null) {
            if (this.hb.clickStarted) {
                DrawMaster.queue(FontHelper.panelEndTurnFont, this.label, this.x, this.y, 650, 1.0f, Color.LIGHT_GRAY);
            } else {
                DrawMaster.queue(FontHelper.panelEndTurnFont, this.label, this.x, this.y, 650, 1.0f, this.textTint.color);
            }
            if (this.hb.clickStarted) {
                DrawMaster.queue(ImageMaster.DYNAMIC_BTN_IMG3, this.x, this.y, 600, Color.LIGHT_GRAY);
            } else {
                DrawMaster.queue(ImageMaster.DYNAMIC_BTN_IMG3, this.x, this.y, 600, this.tint.color);
            }
        }
    }

    public void render(SpriteBatch sb) {
        if (!this.pressed && this.show) {
            this.hb.render(sb);
        }
    }
}

