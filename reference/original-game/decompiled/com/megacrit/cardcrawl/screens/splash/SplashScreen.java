/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.splash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class SplashScreen {
    private Texture img;
    private float timer = 0.0f;
    private static final float BOUNCE_DUR = 1.2f;
    private static final float FADE_DUR = 3.0f;
    private static final float WAIT_DUR = 1.5f;
    private static final float FADE_OUT_DUR = 1.0f;
    private static final int W = 512;
    private static final int H = 512;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color bgColor = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    private Color shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Phase phase = Phase.INIT;
    public boolean isDone = false;
    private static final float OFFSET_Y = 8.0f * Settings.scale;
    private static final float OFFSET_X = 12.0f * Settings.scale;
    private float x = (float)Settings.WIDTH / 2.0f;
    private float y = (float)Settings.HEIGHT / 2.0f - OFFSET_Y;
    private float sX = (float)Settings.WIDTH / 2.0f;
    private float sY = (float)Settings.HEIGHT / 2.0f;
    private Color cream = Color.valueOf("ffffdbff");
    private Color bgBlue = Color.valueOf("074254ff");
    private boolean playSfx = false;
    private long sfxId = -1L;
    private String sfxKey = null;

    public SplashScreen() {
        this.img = ImageMaster.loadImage("images/megaCrit.png");
    }

    public void update() {
        if ((InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) && this.phase != Phase.FADE_OUT) {
            this.phase = Phase.FADE_OUT;
            this.timer = 1.0f;
            if (this.sfxKey != null) {
                CardCrawlGame.sound.fadeOut(this.sfxKey, this.sfxId);
            }
        }
        switch (this.phase) {
            case INIT: {
                this.timer -= Gdx.graphics.getDeltaTime();
                if (!(this.timer < 0.0f)) break;
                this.phase = Phase.BOUNCE;
                this.timer = 1.2f;
                break;
            }
            case BOUNCE: {
                this.timer -= Gdx.graphics.getDeltaTime();
                this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.timer / 1.2f);
                this.y = Interpolation.elasticIn.apply((float)Settings.HEIGHT / 2.0f, (float)Settings.HEIGHT / 2.0f - 200.0f * Settings.scale, this.timer / 1.2f);
                if (this.timer < 0.96000004f && !this.playSfx) {
                    this.playSfx = true;
                    this.sfxId = CardCrawlGame.sound.play("SPLASH");
                }
                if (!(this.timer < 0.0f)) break;
                this.phase = Phase.FADE;
                this.timer = 3.0f;
                break;
            }
            case FADE: {
                this.timer -= Gdx.graphics.getDeltaTime();
                this.color.r = Interpolation.fade.apply(this.cream.r, 1.0f, this.timer / 3.0f);
                this.color.g = Interpolation.fade.apply(this.cream.g, 1.0f, this.timer / 3.0f);
                this.color.b = Interpolation.fade.apply(this.cream.b, 1.0f, this.timer / 3.0f);
                this.bgColor.r = Interpolation.fade.apply(this.bgBlue.r, 0.0f, this.timer / 3.0f);
                this.bgColor.g = Interpolation.fade.apply(this.bgBlue.g, 0.0f, this.timer / 3.0f);
                this.bgColor.b = Interpolation.fade.apply(this.bgBlue.b, 0.0f, this.timer / 3.0f);
                this.sX = Interpolation.exp5Out.apply((float)Settings.WIDTH / 2.0f + OFFSET_X, (float)Settings.WIDTH / 2.0f, this.timer / 3.0f);
                this.sY = Interpolation.exp5Out.apply((float)Settings.HEIGHT / 2.0f - OFFSET_Y, (float)Settings.HEIGHT / 2.0f, this.timer / 3.0f);
                if (!(this.timer < 0.0f)) break;
                this.phase = Phase.WAIT;
                this.timer = 1.5f;
                break;
            }
            case WAIT: {
                this.timer -= Gdx.graphics.getDeltaTime();
                if (!(this.timer < 0.0f)) break;
                this.phase = Phase.FADE_OUT;
                this.timer = 1.0f;
                break;
            }
            case FADE_OUT: {
                this.bgColor.a = Interpolation.fade.apply(0.0f, 1.0f, this.timer / 1.0f);
                this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.timer / 1.0f);
                this.timer -= Gdx.graphics.getDeltaTime();
                if (!(this.timer < 0.0f)) break;
                this.img.dispose();
                this.isDone = true;
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(this.shadowColor);
        sb.draw(this.img, this.sX - 256.0f, this.sY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
        Color s = new Color(0.0f, 0.0f, 0.0f, this.color.a / 5.0f);
        sb.setColor(s);
        sb.draw(this.img, this.x - 256.0f + 14.0f * Settings.scale, this.y - 256.0f - 14.0f * Settings.scale, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 256.0f, this.y - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
    }

    private static enum Phase {
        INIT,
        BOUNCE,
        FADE,
        WAIT,
        FADE_OUT;

    }
}

