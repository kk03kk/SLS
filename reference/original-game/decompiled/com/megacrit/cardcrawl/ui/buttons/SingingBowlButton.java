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
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.ui.buttons.SkipCardButton;

public class SingingBowlButton {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CardRewardScreen");
    public static final String[] TEXT = SingingBowlButton.uiStrings.TEXT;
    private static final int W = 512;
    private static final int H = 256;
    private static final float SHOW_X = (float)Settings.WIDTH / 2.0f + 165.0f * Settings.scale;
    private static final float HIDE_X = (float)Settings.WIDTH / 2.0f;
    private float current_x;
    private float target_x;
    private Color textColor;
    private Color btnColor;
    private boolean isHidden;
    private RewardItem rItem;
    private float controllerImgTextWidth;
    private static final float HITBOX_W = 260.0f * Settings.scale;
    private static final float HITBOX_H = 80.0f * Settings.scale;
    public Hitbox hb;

    public SingingBowlButton() {
        this.target_x = this.current_x = HIDE_X;
        this.textColor = Color.WHITE.cpy();
        this.btnColor = Color.WHITE.cpy();
        this.isHidden = true;
        this.rItem = null;
        this.controllerImgTextWidth = 0.0f;
        this.hb = new Hitbox(0.0f, 0.0f, HITBOX_W, HITBOX_H);
        this.hb.move((float)Settings.WIDTH / 2.0f, SkipCardButton.TAKE_Y);
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
        if (this.hb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
            CInputActionSet.proceed.unpress();
            this.hb.clicked = false;
            this.onClick();
            AbstractDungeon.cardRewardScreen.closeFromBowlButton();
            AbstractDungeon.closeCurrentScreen();
            this.hide();
        }
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
                this.hb.move(this.current_x, SkipCardButton.TAKE_Y);
            }
        }
        this.btnColor.a = this.textColor.a = MathHelper.fadeLerpSnap(this.textColor.a, 1.0f);
    }

    public void onClick() {
        AbstractDungeon.player.getRelic("Singing Bowl").flash();
        CardCrawlGame.sound.playA("SINGING_BOWL", MathUtils.random(-0.2f, 0.1f));
        AbstractDungeon.player.increaseMaxHp(2, true);
        AbstractDungeon.combatRewardScreen.rewards.remove(this.rItem);
    }

    public void hide() {
        if (!this.isHidden) {
            this.isHidden = true;
        }
    }

    public void show(RewardItem rItem) {
        this.isHidden = false;
        this.textColor.a = 0.0f;
        this.btnColor.a = 0.0f;
        this.current_x = HIDE_X;
        this.target_x = SHOW_X;
        this.rItem = rItem;
    }

    public void render(SpriteBatch sb) {
        if (this.isHidden) {
            return;
        }
        this.renderButton(sb);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[2], this.current_x, SkipCardButton.TAKE_Y, this.textColor);
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    private void renderButton(SpriteBatch sb) {
        sb.setColor(this.btnColor);
        sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.current_x - 256.0f, SkipCardButton.TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.3f));
            sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, this.current_x - 256.0f, SkipCardButton.TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (Settings.isControllerMode) {
            if (this.controllerImgTextWidth == 0.0f) {
                this.controllerImgTextWidth = FontHelper.getSmartWidth(FontHelper.buttonLabelFont, TEXT[2], 9999.0f, 0.0f);
            }
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.current_x - 32.0f - this.controllerImgTextWidth / 2.0f - 38.0f * Settings.scale, SkipCardButton.TAKE_Y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.hb.render(sb);
    }
}

