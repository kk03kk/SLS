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
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.ImpactSparkEffect;

public class LightningEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img = null;

    public LightningEffect(float x, float y) {
        if (this.img == null) {
            this.img = ImageMaster.vfxAtlas.findRegion("combat/lightning");
        }
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y;
        this.color = Color.WHITE.cpy();
        this.duration = 0.5f;
        this.startingDuration = 0.5f;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.MED, false);
            for (int i = 0; i < 15; ++i) {
                AbstractDungeon.topLevelEffectsQueue.add(new ImpactSparkEffect(this.x + MathUtils.random(-20.0f, 20.0f) * Settings.scale + 150.0f * Settings.scale, this.y + MathUtils.random(-20.0f, 20.0f) * Settings.scale));
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = Interpolation.bounceIn.apply(this.duration * 2.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, 0.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

