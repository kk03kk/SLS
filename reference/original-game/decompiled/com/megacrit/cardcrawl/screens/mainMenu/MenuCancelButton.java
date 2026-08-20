/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class MenuCancelButton {
    private static final int W = 512;
    private static final int H = 256;
    private static final Color HOVER_BLEND_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.4f);
    private static final float SHOW_X = 256.0f * Settings.scale;
    private static final float DRAW_Y = 128.0f * Settings.scale;
    public static final float HIDE_X = SHOW_X - 400.0f * Settings.scale;
    public float current_x;
    private float target_x;
    public boolean isHidden;
    private float glowAlpha;
    private Color glowColor;
    private String buttonText;
    private static final float TEXT_OFFSET_X = -136.0f * Settings.scale;
    private static final float TEXT_OFFSET_Y = 57.0f * Settings.scale;
    public Hitbox hb;

    public MenuCancelButton() {
        this.target_x = this.current_x = HIDE_X;
        this.isHidden = true;
        this.glowAlpha = 0.0f;
        this.glowColor = Settings.GOLD_COLOR.cpy();
        this.buttonText = "NOT_SET";
        this.hb = new Hitbox(300.0f * Settings.scale, 100.0f * Settings.scale);
        this.hb.move(SHOW_X - 106.0f * Settings.scale, DRAW_Y + 60.0f * Settings.scale);
    }

    public void update() {
        if (!this.isHidden) {
            this.updateGlow();
            this.hb.update();
            if (InputHelper.justClickedLeft && this.hb.hovered) {
                this.hb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (this.hb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (CInputActionSet.cancel.isJustPressed()) {
                this.hb.clicked = true;
            }
        }
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
            }
        }
    }

    private void updateGlow() {
        float tmp;
        this.glowAlpha += Gdx.graphics.getDeltaTime() * 3.0f;
        if (this.glowAlpha < 0.0f) {
            this.glowAlpha *= -1.0f;
        }
        this.glowColor.a = (tmp = MathUtils.cos(this.glowAlpha)) < 0.0f ? -tmp / 2.0f + 0.3f : tmp / 2.0f + 0.3f;
    }

    public boolean hovered() {
        return this.hb.hovered;
    }

    public void hide() {
        if (!this.isHidden) {
            this.hb.clicked = false;
            this.hb.hovered = false;
            InputHelper.justClickedLeft = false;
            this.target_x = HIDE_X;
            this.isHidden = true;
        }
    }

    public void hideInstantly() {
        if (!this.isHidden) {
            this.hb.hovered = false;
            InputHelper.justClickedLeft = false;
            this.current_x = this.target_x = HIDE_X;
            this.isHidden = true;
        }
    }

    public void show(String buttonText) {
        if (this.isHidden) {
            this.glowAlpha = 0.0f;
            this.current_x = HIDE_X;
            this.target_x = SHOW_X;
            this.isHidden = false;
            this.buttonText = buttonText;
        } else {
            this.current_x = HIDE_X;
            this.buttonText = buttonText;
        }
        this.hb.hovered = false;
    }

    public void showInstantly(String buttonText) {
        this.current_x = SHOW_X;
        this.target_x = SHOW_X;
        this.isHidden = false;
        this.buttonText = buttonText;
        this.hb.hovered = false;
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        this.renderShadow(sb);
        sb.setColor(this.glowColor);
        this.renderOutline(sb);
        sb.setColor(Color.WHITE);
        this.renderButton(sb);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_BLEND_COLOR);
            this.renderButton(sb);
            sb.setBlendFunction(770, 771);
        }
        Color tmpColor = Settings.LIGHT_YELLOW_COLOR;
        if (this.hb.clickStarted) {
            tmpColor = Color.LIGHT_GRAY;
        }
        if (Settings.isControllerMode) {
            FontHelper.renderFontLeft(sb, FontHelper.buttonLabelFont, this.buttonText, this.current_x + TEXT_OFFSET_X - 30.0f * Settings.scale, DRAW_Y + TEXT_OFFSET_Y, tmpColor);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.buttonText, this.current_x + TEXT_OFFSET_X, DRAW_Y + TEXT_OFFSET_Y, tmpColor);
        }
        this.renderControllerUi(sb);
        if (!this.isHidden) {
            this.hb.render(sb);
        }
    }

    private void renderShadow(SpriteBatch sb) {
        sb.draw(ImageMaster.CANCEL_BUTTON_SHADOW, this.current_x - 256.0f, DRAW_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
    }

    private void renderOutline(SpriteBatch sb) {
        sb.draw(ImageMaster.CANCEL_BUTTON_OUTLINE, this.current_x - 256.0f, DRAW_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
    }

    private void renderButton(SpriteBatch sb) {
        sb.draw(ImageMaster.CANCEL_BUTTON, this.current_x - 256.0f, DRAW_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
    }

    private void renderControllerUi(SpriteBatch sb) {
        if (Settings.isControllerMode) {
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.cancel.getKeyImg(), this.current_x - 32.0f - 210.0f * Settings.scale, DRAW_Y - 32.0f + 57.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }
}

