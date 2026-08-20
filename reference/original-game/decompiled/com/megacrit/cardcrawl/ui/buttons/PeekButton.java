/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.combat.LightFlareParticleEffect;

public class PeekButton {
    private static final Color HOVER_BLEND_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.4f);
    private static final float SHOW_X = 140.0f * Settings.scale;
    private static final float DRAW_Y = (float)Settings.HEIGHT / 2.0f;
    private static final float HIDE_X = SHOW_X - 400.0f * Settings.scale;
    private float current_x;
    private float target_x;
    private boolean isHidden;
    public static boolean isPeeking = false;
    private float particleTimer;
    public Hitbox hb;

    public PeekButton() {
        this.target_x = this.current_x = HIDE_X;
        this.isHidden = true;
        this.particleTimer = 0.0f;
        this.hb = new Hitbox(170.0f * Settings.scale, 170.0f * Settings.scale);
        this.hb.move(SHOW_X, DRAW_Y);
    }

    public void update() {
        if (!this.isHidden) {
            this.hb.update();
            if (InputHelper.justClickedLeft && this.hb.hovered) {
                this.hb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (this.hb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (InputActionSet.peek.isJustPressed() || CInputActionSet.peek.isJustPressed()) {
                CInputActionSet.peek.unpress();
                this.hb.clicked = true;
            }
        }
        if (isPeeking) {
            this.particleTimer -= Gdx.graphics.getDeltaTime();
            if (this.particleTimer < 0.0f) {
                this.particleTimer = 0.2f;
                AbstractDungeon.effectsQueue.add(new LightFlareParticleEffect(this.hb.cX, this.hb.cY, Color.SKY));
                AbstractDungeon.effectsQueue.add(new LightFlareParticleEffect(this.hb.cX, this.hb.cY, Color.WHITE));
            }
        }
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
            }
        }
    }

    public void hideInstantly() {
        this.current_x = HIDE_X;
        this.target_x = HIDE_X;
        this.isHidden = true;
    }

    public void hide() {
        if (!this.isHidden) {
            this.target_x = HIDE_X;
            this.isHidden = true;
        }
    }

    public void show() {
        if (this.isHidden) {
            isPeeking = false;
            this.target_x = SHOW_X;
            this.isHidden = false;
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        this.renderButton(sb);
        if (isPeeking) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(0.6f, 1.0f, 1.0f, 1.0f));
            float derp = Interpolation.swingOut.apply(1.0f, 1.1f, MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) / 12.0f);
            sb.draw(ImageMaster.PEEK_BUTTON, this.current_x - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * derp, Settings.scale * derp, 0.0f, 0, 0, 128, 128, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (this.hb.hovered && !this.hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(HOVER_BLEND_COLOR);
            this.renderButton(sb);
            sb.setBlendFunction(770, 771);
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            boolean bl = isPeeking = !isPeeking;
            if (isPeeking) {
                AbstractDungeon.overlayMenu.hideBlackScreen();
                AbstractDungeon.dynamicBanner.hide();
            } else {
                AbstractDungeon.overlayMenu.showBlackScreen(0.75f);
                if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
                    AbstractDungeon.dynamicBanner.appear();
                }
            }
        }
        this.renderControllerUi(sb);
        if (!this.isHidden) {
            this.hb.render(sb);
        }
    }

    private void renderButton(SpriteBatch sb) {
        sb.draw(ImageMaster.PEEK_BUTTON, this.current_x - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
    }

    private void renderControllerUi(SpriteBatch sb) {
        if (Settings.isControllerMode && !this.isHidden) {
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.peek.getKeyImg(), 20.0f * Settings.scale, this.hb.cY - 56.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }
}

