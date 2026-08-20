/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;

public class CardSelectConfirmButton {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Confirm Button");
    public static final String[] TEXT = CardSelectConfirmButton.uiStrings.TEXT;
    private static final int W = 512;
    private static final int H = 256;
    private static final float TAKE_Y = 475.0f * Settings.scale;
    private static final float SHOW_X = (float)Settings.WIDTH - 256.0f * Settings.scale;
    private static final float HIDE_X = SHOW_X + 400.0f * Settings.scale;
    private float current_x;
    private float target_x;
    private boolean isHidden;
    public boolean isDisabled;
    private Color textColor;
    private Color btnColor;
    private float target_a;
    private String buttonText;
    private static final float HITBOX_W = 260.0f * Settings.scale;
    private static final float HITBOX_H = 80.0f * Settings.scale;
    public Hitbox hb;

    public CardSelectConfirmButton() {
        this.target_x = this.current_x = HIDE_X;
        this.isHidden = true;
        this.isDisabled = true;
        this.textColor = Color.WHITE.cpy();
        this.btnColor = Color.WHITE.cpy();
        this.target_a = 0.0f;
        this.buttonText = "NOT_SET";
        this.hb = new Hitbox(0.0f, 0.0f, HITBOX_W, HITBOX_H);
        this.buttonText = TEXT[0];
        this.hb.move((float)Settings.WIDTH / 2.0f, TAKE_Y);
    }

    public void update() {
        if (!this.isHidden) {
            this.hb.update();
        }
        if (!this.isDisabled) {
            if (this.hb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (this.hb.hovered && InputHelper.justClickedLeft) {
                this.hb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
        }
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
            }
        }
        this.btnColor.a = this.textColor.a = MathHelper.fadeLerpSnap(this.textColor.a, this.target_a);
    }

    public void hideInstantly() {
        this.current_x = HIDE_X;
        this.target_x = HIDE_X;
        this.isHidden = true;
        this.target_a = 0.0f;
        this.textColor.a = 0.0f;
    }

    public void hide() {
        if (!this.isHidden) {
            this.target_a = 0.0f;
            this.target_x = HIDE_X;
            this.isHidden = true;
        }
    }

    public void show() {
        if (this.isHidden) {
            this.textColor.a = 0.0f;
            this.target_a = 1.0f;
            this.target_x = SHOW_X;
            this.isHidden = false;
        }
    }

    public void disable() {
        if (!this.isDisabled) {
            this.hb.hovered = false;
            this.isDisabled = true;
            this.btnColor = Color.GRAY.cpy();
            this.textColor = Color.LIGHT_GRAY.cpy();
        }
    }

    public void enable() {
        if (this.isDisabled) {
            this.isDisabled = false;
            this.btnColor = Color.WHITE.cpy();
            this.textColor = Settings.CREAM_COLOR.cpy();
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.btnColor);
        this.renderButton(sb);
        if (this.hb.hovered && !this.isDisabled && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.3f));
            this.renderButton(sb);
            sb.setBlendFunction(770, 771);
        }
        if (!this.isHidden) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.buttonText, (float)Settings.WIDTH / 2.0f, TAKE_Y, this.textColor);
        }
    }

    private void renderButton(SpriteBatch sb) {
        if (!this.isHidden) {
            if (this.isDisabled) {
                sb.draw(ImageMaster.REWARD_SCREEN_TAKE_USED_BUTTON, (float)Settings.WIDTH / 2.0f - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
            } else {
                if (this.hb.clickStarted) {
                    sb.setColor(Color.LIGHT_GRAY);
                }
                sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, (float)Settings.WIDTH / 2.0f - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
            }
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.proceed.getKeyImg(), this.hb.cX - 32.0f - 100.0f * Settings.scale, this.hb.cY - 32.0f + 2.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            this.hb.render(sb);
        }
    }
}

