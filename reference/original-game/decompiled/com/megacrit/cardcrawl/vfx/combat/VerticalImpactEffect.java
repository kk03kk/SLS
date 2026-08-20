/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.GenericSmokeEffect;

public class VerticalImpactEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private static final float DUR = 0.6f;
    private TextureAtlas.AtlasRegion img = ImageMaster.VERTICAL_IMPACT;
    private boolean playedSound = false;

    public VerticalImpactEffect(float x, float y) {
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight * 0.01f;
        this.startingDuration = 0.6f;
        this.duration = 0.6f;
        this.scale = Settings.scale;
        this.rotation = MathUtils.random(40.0f, 50.0f);
        this.color = Color.SCARLET.cpy();
        this.renderBehind = false;
        for (int i = 0; i < 50; ++i) {
            AbstractDungeon.effectsQueue.add(new GenericSmokeEffect(x + MathUtils.random(-280.0f, 250.0f) * Settings.scale, y - 80.0f * Settings.scale));
        }
    }

    private void playRandomSfX() {
        CardCrawlGame.sound.playA("BLUNT_HEAVY", -0.3f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration < 0.5f && !this.playedSound) {
            this.playRandomSfX();
            this.playedSound = true;
        }
        this.color.a = this.duration > 0.2f ? Interpolation.fade.apply(0.5f, 0.0f, (this.duration - 0.34f) * 5.0f) : Interpolation.fade.apply(0.0f, 0.5f, this.duration * 5.0f);
        this.scale = Interpolation.fade.apply(Settings.scale * 1.1f, Settings.scale * 1.05f, this.duration / 0.6f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.3f, this.scale * 0.8f, this.rotation - 18.0f);
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.3f, this.scale * 0.8f, this.rotation + MathUtils.random(12.0f, 18.0f));
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.4f, this.scale * 0.5f, this.rotation - MathUtils.random(-10.0f, 14.0f));
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.7f, this.scale * 0.9f, this.rotation + MathUtils.random(20.0f, 28.0f));
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 1.5f, this.scale * MathUtils.random(1.4f, 1.6f), this.rotation);
        Color c = Color.GOLD.cpy();
        c.a = this.color.a;
        sb.setColor(c);
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * MathUtils.random(0.4f, 0.6f), this.rotation);
        sb.draw(this.img, this.x + MathUtils.random(-10.0f, 10.0f) * Settings.scale, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.5f, this.scale * 0.7f, this.rotation + MathUtils.random(20.0f, 28.0f));
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

