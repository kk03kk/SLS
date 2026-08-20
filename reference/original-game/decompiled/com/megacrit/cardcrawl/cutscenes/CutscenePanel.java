/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cutscenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ScreenShake;

public class CutscenePanel {
    private Texture img;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    public boolean activated = false;
    public boolean finished = false;
    public boolean fadeOut = false;
    private String sfx = null;

    public CutscenePanel(String imgUrl, String sfx) {
        this.img = ImageMaster.loadImage(imgUrl);
        this.sfx = sfx;
    }

    public CutscenePanel(String imgUrl) {
        this(imgUrl, null);
    }

    public void update() {
        if (this.fadeOut) {
            this.color.a -= Gdx.graphics.getDeltaTime();
            if (this.color.a < 0.0f) {
                this.color.a = 0.0f;
                this.finished = true;
            }
            return;
        }
        if (this.activated && !this.finished) {
            this.color.a = this.sfx != null ? (this.color.a += Gdx.graphics.getDeltaTime() * 10.0f) : (this.color.a += Gdx.graphics.getDeltaTime());
            if (this.color.a > 1.0f) {
                this.color.a = 1.0f;
                this.finished = true;
            }
        }
    }

    public void activate() {
        if (this.sfx != null) {
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, false);
            CardCrawlGame.sound.play(this.sfx);
            CardCrawlGame.sound.playA(this.sfx, -0.2f);
        }
        this.activated = true;
    }

    public void render(SpriteBatch sb) {
        if (this.img != null) {
            sb.setColor(this.color);
            if (Settings.isSixteenByTen) {
                sb.draw(this.img, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
            } else {
                sb.draw(this.img, 0.0f, -50.0f * Settings.scale, (float)Settings.WIDTH, (float)Settings.HEIGHT + 110.0f * Settings.scale);
            }
        }
    }

    public void fadeOut() {
        this.fadeOut = true;
        this.finished = false;
    }

    public void dispose() {
        if (this.img != null) {
            this.img.dispose();
            this.img = null;
        }
    }
}

