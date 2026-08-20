/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;

public class HorizontalScrollBar {
    public ScrollBarListener sliderListener;
    public boolean isBackgroundVisible = true;
    private Hitbox sliderHb;
    private float currentScrollPercent;
    private boolean isDragging;
    public static final float TRACK_H = 54.0f * Settings.scale;
    private final float TRACK_CAP_WIDTH = TRACK_H;
    private final float CURSOR_W = 60.0f * Settings.scale;
    private final float CURSOR_H = 38.0f * Settings.scale;
    private final float DRAW_BORDER = this.CURSOR_W / 4.0f;
    private float cursorDrawPosition = 0.0f;

    public HorizontalScrollBar(ScrollBarListener listener) {
        this(listener, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT - 150.0f * Settings.scale - TRACK_H / 2.0f, (float)Settings.WIDTH - 256.0f * Settings.scale);
        this.cursorDrawPosition = this.getXPositionForPercent(0.0f);
    }

    public HorizontalScrollBar(ScrollBarListener listener, float x, float y, float width) {
        this.sliderListener = listener;
        this.currentScrollPercent = 0.0f;
        this.isDragging = false;
        this.sliderHb = new Hitbox(width, TRACK_H);
        this.sliderHb.move(x, y);
        this.reset();
    }

    public void setCenter(float x, float y) {
        this.sliderHb.move(x, y);
        this.reset();
    }

    public void move(float xOffset, float yOffset) {
        this.sliderHb.move(this.sliderHb.cX + xOffset, this.sliderHb.cY + yOffset);
        this.reset();
    }

    public void changeWidth(float newWidth) {
        this.sliderHb.width = newWidth;
        this.sliderHb.move(this.sliderHb.cX, this.sliderHb.cY);
        this.reset();
    }

    public float width() {
        return this.sliderHb.width;
    }

    public void reset() {
        this.cursorDrawPosition = this.getXPositionForPercent(0.0f);
    }

    public boolean update() {
        this.sliderHb.update();
        if (this.sliderHb.hovered && InputHelper.isMouseDown) {
            this.isDragging = true;
        }
        if (this.isDragging && InputHelper.justReleasedClickLeft) {
            this.isDragging = false;
            return true;
        }
        if (this.isDragging) {
            float newPercent = this.getPercentFromX(InputHelper.mX);
            this.sliderListener.scrolledUsingBar(newPercent);
            return true;
        }
        return false;
    }

    public void parentScrolledToPercent(float percent) {
        this.currentScrollPercent = this.boundedPercent(percent);
    }

    private float getPercentFromX(float x) {
        float minX = this.sliderHb.x + this.DRAW_BORDER;
        float maxX = this.sliderHb.x + this.sliderHb.width - this.DRAW_BORDER;
        return this.boundedPercent(MathHelper.percentFromValueBetween(minX, maxX, x));
    }

    public void render(SpriteBatch sb) {
        Color previousColor = sb.getColor();
        sb.setColor(Color.WHITE);
        if (this.isBackgroundVisible) {
            this.renderTrack(sb);
        }
        this.renderCursor(sb);
        this.sliderHb.render(sb);
        sb.setColor(previousColor);
    }

    private float getXPositionForPercent(float percent) {
        float topX = this.sliderHb.x - this.DRAW_BORDER;
        float bottomX = this.sliderHb.x + this.sliderHb.width - this.CURSOR_W + this.DRAW_BORDER;
        return MathHelper.valueFromPercentBetween(topX, bottomX, this.boundedPercent(percent));
    }

    private void renderCursor(SpriteBatch sb) {
        float y = this.sliderHb.cY - this.CURSOR_H / 2.0f;
        float xForPercent = this.getXPositionForPercent(this.currentScrollPercent);
        this.cursorDrawPosition = MathHelper.scrollSnapLerpSpeed(this.cursorDrawPosition, xForPercent);
        sb.draw(ImageMaster.SCROLL_BAR_HORIZONTAL_TRAIN, this.cursorDrawPosition, y, this.CURSOR_W, this.CURSOR_H);
    }

    private void renderTrack(SpriteBatch sb) {
        sb.draw(ImageMaster.SCROLL_BAR_HORIZONTAL_MIDDLE, this.sliderHb.x, this.sliderHb.y, this.sliderHb.width, this.sliderHb.height);
        sb.draw(ImageMaster.SCROLL_BAR_RIGHT, this.sliderHb.x + this.sliderHb.width, this.sliderHb.y, this.TRACK_CAP_WIDTH, this.sliderHb.height);
        sb.draw(ImageMaster.SCROLL_BAR_LEFT, this.sliderHb.x - this.TRACK_CAP_WIDTH, this.sliderHb.y, this.TRACK_CAP_WIDTH, this.sliderHb.height);
    }

    private float boundedPercent(float percent) {
        return Math.max(0.0f, Math.min(percent, 1.0f));
    }
}

