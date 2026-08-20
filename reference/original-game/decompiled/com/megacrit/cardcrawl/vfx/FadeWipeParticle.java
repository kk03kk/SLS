/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FadeWipeParticle
extends AbstractGameEffect {
    private static final float DUR = 1.0f;
    private float y;
    private float lerpTimer;
    private float delayTimer;
    private TextureAtlas.AtlasRegion img = ImageMaster.SCENE_TRANSITION_FADER;
    private Texture flatImg = ImageMaster.WHITE_SQUARE_IMG;

    public FadeWipeParticle() {
        this.color = AbstractDungeon.fadeColor.cpy();
        this.color.a = 0.0f;
        this.duration = 1.0f;
        this.startingDuration = 1.0f;
        this.y = Settings.HEIGHT + this.img.packedHeight;
        this.delayTimer = 0.1f;
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.duration = 0.0f;
            this.lerpTimer += Gdx.graphics.getDeltaTime();
            if (this.lerpTimer > 0.5f) {
                this.color.a = MathHelper.slowColorLerpSnap(this.color.a, 0.0f);
                if (this.color.a == 0.0f) {
                    this.isDone = true;
                }
            }
        } else {
            this.color.a = Interpolation.pow5In.apply(1.0f, 0.0f, this.duration / 1.0f);
            this.y = Interpolation.pow3In.apply(0.0f - (float)this.img.packedHeight, Settings.HEIGHT + this.img.packedHeight, this.duration / 1.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, 0.0f, this.y, (float)Settings.WIDTH, (float)this.img.packedHeight);
        sb.draw(this.flatImg, 0.0f, this.y + (float)this.img.packedHeight - 1.0f * Settings.scale, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    @Override
    public void dispose() {
    }
}

