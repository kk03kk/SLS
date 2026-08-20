/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
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
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class UnlockConfirmButton {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("Unlock Confirm Button");
    public static final String[] TEXT = UnlockConfirmButton.uiStrings.TEXT;
    private static final int W = 512;
    private static final int H = 256;
    private static final float TAKE_Y = (float)Settings.HEIGHT / 2.0f - 410.0f * Settings.scale;
    private static final float X = (float)Settings.WIDTH / 2.0f;
    private Color hoverColor = Color.WHITE.cpy();
    private Color textColor = Color.WHITE.cpy();
    private Color btnColor = Color.WHITE.cpy();
    private float target_a = 0.0f;
    private boolean done = false;
    private float animTimer = 0.0f;
    private static final float ANIM_TIME = 0.4f;
    private float scale = 0.8f;
    private static final float HOVER_BRIGHTNESS = 0.33f;
    private static final float SCALE_START = 0.6f;
    private String buttonText = "NOT_SET";
    private static final float HITBOX_W = 260.0f * Settings.scale;
    private static final float HITBOX_H = 80.0f * Settings.scale;
    public Hitbox hb = new Hitbox(0.0f, 0.0f, HITBOX_W, HITBOX_H);

    public UnlockConfirmButton() {
        this.buttonText = TEXT[0];
        this.hb.move((float)Settings.WIDTH / 2.0f, TAKE_Y);
    }

    public void update() {
        this.animateIn();
        if (!this.done && this.animTimer < 0.2f) {
            this.hb.update();
        }
        this.hoverColor.a = this.hb.hovered && !this.done ? 0.33f : MathHelper.fadeLerpSnap(this.hoverColor.a, 0.0f);
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        }
        if (this.hb.clicked || CInputActionSet.select.isJustPressed()) {
            CInputActionSet.select.unpress();
            this.hb.clicked = false;
            this.hb.hovered = false;
            if (AbstractDungeon.unlockScreen.unlock != null) {
                UnlockTracker.hardUnlock(AbstractDungeon.unlockScreen.unlock.key);
                CardCrawlGame.sound.stop("UNLOCK_SCREEN", AbstractDungeon.unlockScreen.id);
            } else if (AbstractDungeon.unlocks != null) {
                for (AbstractUnlock u : AbstractDungeon.unlocks) {
                    UnlockTracker.hardUnlock(u.key);
                }
            }
            InputHelper.justClickedLeft = false;
            this.hide();
            AbstractDungeon.previousScreen = !AbstractDungeon.is_victory ? AbstractDungeon.CurrentScreen.DEATH : AbstractDungeon.CurrentScreen.VICTORY;
            AbstractDungeon.closeCurrentScreen();
        }
        this.btnColor.a = this.textColor.a = MathHelper.fadeLerpSnap(this.textColor.a, this.target_a);
    }

    private void animateIn() {
        if (this.animTimer != 0.0f) {
            this.animTimer -= Gdx.graphics.getDeltaTime();
            if (this.animTimer < 0.0f) {
                this.animTimer = 0.0f;
            }
            this.scale = Interpolation.elasticIn.apply(1.0f, 0.6f, this.animTimer / 0.4f);
        }
    }

    public void hide() {
        this.textColor = Color.LIGHT_GRAY.cpy();
        this.done = true;
    }

    public void show() {
        this.textColor = Color.WHITE.cpy();
        this.animTimer = 0.4f;
        this.hoverColor.a = 0.0f;
        this.textColor.a = 0.0f;
        this.target_a = 1.0f;
        this.scale = 0.6f;
        this.done = false;
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.btnColor);
        this.renderButton(sb);
        if (!this.hb.clickStarted && !this.done) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.hoverColor);
            this.renderButton(sb);
            sb.setBlendFunction(770, 771);
        }
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.buttonText, (float)Settings.WIDTH / 2.0f, TAKE_Y, this.textColor, this.scale);
    }

    private void renderButton(SpriteBatch sb) {
        if (this.hb.clickStarted) {
            sb.setColor(Color.LIGHT_GRAY);
        }
        if (!this.done) {
            sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON, X - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, this.scale * Settings.scale, this.scale * Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
        } else {
            sb.draw(ImageMaster.REWARD_SCREEN_TAKE_USED_BUTTON, X - 256.0f, TAKE_Y - 128.0f, 256.0f, 128.0f, 512.0f, 256.0f, this.scale * Settings.scale, this.scale * Settings.scale, 0.0f, 0, 0, 512, 256, false, false);
        }
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.select.getKeyImg(), X - 32.0f - 130.0f * Settings.scale, TAKE_Y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.hb.render(sb);
    }
}

