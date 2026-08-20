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
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;

public class SkipCardButton {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CardRewardScreen");
    public static final String[] TEXT = SkipCardButton.uiStrings.TEXT;
    private static final int W = 512;
    private static final int H = 256;
    public static final float TAKE_Y = (float)Settings.HEIGHT / 2.0f - 340.0f * Settings.scale;
    private static final float SHOW_X = (float)Settings.WIDTH / 2.0f;
    private static final float HIDE_X = (float)Settings.WIDTH / 2.0f;
    private float current_x;
    private float target_x;
    private boolean isHidden;
    private Color textColor;
    private Color btnColor;
    public boolean screenDisabled;
    private static final float HITBOX_W = 260.0f * Settings.scale;
    private static final float HITBOX_H = 80.0f * Settings.scale;
    public Hitbox hb;
    private float controllerImgTextWidth;

    public SkipCardButton() {
        this.target_x = this.current_x = HIDE_X;
        this.isHidden = true;
        this.textColor = Color.WHITE.cpy();
        this.btnColor = Color.WHITE.cpy();
        this.screenDisabled = false;
        this.hb = new Hitbox(0.0f, 0.0f, HITBOX_W, HITBOX_H);
        this.controllerImgTextWidth = 0.0f;
        this.hb.move((float)Settings.WIDTH / 2.0f, TAKE_Y);
    }

    public void update() {
        if (this.isHidden) {
            return;
        }
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        }
        if ((this.hb.clicked || InputActionSet.cancel.isJustPressed() || CInputActionSet.cancel.isJustPressed()) && !this.screenDisabled) {
            this.hb.clicked = false;
            AbstractDungeon.closeCurrentScreen();
        }
        this.screenDisabled = false;
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
                this.hb.move(this.current_x, TAKE_Y);
            }
        }
        this.btnColor.a = this.textColor.a = MathHelper.fadeLerpSnap(this.textColor.a, 1.0f);
    }

    public void hideInstantly() {
        this.current_x = HIDE_X;
        this.target_x = HIDE_X;
        this.isHidden = true;
        this.textColor.a = 0.0f;
        this.btnColor.a = 0.0f;
    }

    public void hide() {
        this.isHidden = true;
    }

    public void show() {
        this.isHidden = false;
        this.textColor.a = 0.0f;
        this.btnColor.a = 0.0f;
        this.current_x = HIDE_X;
        this.target_x = SHOW_X;
        this.hb.move(SHOW_X, TAKE_Y);
    }

    public void show(boolean singingBowl) {
        this.isHidden = false;
        this.textColor.a = 0.0f;
        this.btnColor.a = 0.0f;
        this.current_x = HIDE_X;
        this.target_x = SHOW_X - 165.0f * Settings.scale;
    }

    public void render(SpriteBatch sb) {
        if (this.isHidden) {
            return;
        }
        this.renderButton(sb);
        if (FontHelper.getSmartWidth(FontHelper.buttonLabelFont, TEXT[0], 9999.0f, 0.0f) > 200.0f * Settings.scale) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[0], this.current_x, TAKE_Y, this.textColor, 0.8f);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[0], this.current_x, TAKE_Y, this.textColor);
        }
    }

    private void renderButton(SpriteBatch sb) {
        sb.setColor(this.btnColor);
        sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.current_x - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.3f));
            sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.current_x - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (Settings.isControllerMode) {
            if (this.controllerImgTextWidth == 0.0f) {
                this.controllerImgTextWidth = FontHelper.getSmartWidth(FontHelper.buttonLabelFont, TEXT[0], 99999.0f, 0.0f) / 2.0f;
            }
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.cancel.getKeyImg(), this.current_x - 32.0f - this.controllerImgTextWidth - 38.0f * Settings.scale, TAKE_Y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.hb.render(sb);
    }
}

