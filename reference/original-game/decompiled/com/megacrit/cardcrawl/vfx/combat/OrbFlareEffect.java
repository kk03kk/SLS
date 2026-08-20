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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class OrbFlareEffect
extends AbstractGameEffect {
    private static TextureAtlas.AtlasRegion outer;
    private static TextureAtlas.AtlasRegion inner;
    private float scaleY;
    private static final float DUR = 0.5f;
    private AbstractOrb orb;
    private OrbFlareColor flareColor;
    private Color color2;

    public OrbFlareEffect(AbstractOrb orb, OrbFlareColor setColor) {
        if (outer == null) {
            outer = ImageMaster.vfxAtlas.findRegion("combat/orbFlareOuter");
            inner = ImageMaster.vfxAtlas.findRegion("combat/orbFlareInner");
        }
        this.orb = orb;
        this.renderBehind = true;
        this.duration = 0.5f;
        this.startingDuration = 0.5f;
        this.flareColor = setColor;
        this.setColor();
        this.scale = 2.0f * Settings.scale;
        this.scaleY = 0.0f;
    }

    private void setColor() {
        switch (this.flareColor) {
            case DARK: {
                this.color = Color.VIOLET.cpy();
                this.color2 = Color.BLACK.cpy();
                break;
            }
            case FROST: {
                this.color = Settings.BLUE_TEXT_COLOR.cpy();
                this.color2 = Color.LIGHT_GRAY.cpy();
                break;
            }
            case LIGHTNING: {
                this.color = Color.CHARTREUSE.cpy();
                this.color2 = Color.WHITE.cpy();
                break;
            }
            case PLASMA: {
                this.color = Color.CORAL.cpy();
                this.color2 = Color.CYAN.cpy();
                break;
            }
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.duration = 0.0f;
            this.isDone = true;
        }
        this.scaleY = Interpolation.elasticIn.apply(2.2f, 0.8f, this.duration * 2.0f);
        this.scale = Interpolation.elasticIn.apply(2.1f, 1.9f, this.duration * 2.0f);
        this.color2.a = this.color.a = Interpolation.pow2Out.apply(0.0f, 0.6f, this.duration * 2.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color2);
        sb.draw(inner, this.orb.cX - (float)OrbFlareEffect.inner.packedWidth / 2.0f, this.orb.cY - (float)OrbFlareEffect.inner.packedHeight / 2.0f, (float)OrbFlareEffect.inner.packedWidth / 2.0f, (float)OrbFlareEffect.inner.packedHeight / 2.0f, OrbFlareEffect.inner.packedWidth, OrbFlareEffect.inner.packedHeight, this.scale * Settings.scale * 1.1f, this.scaleY * Settings.scale, MathUtils.random(-1.0f, 1.0f));
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(outer, this.orb.cX - (float)OrbFlareEffect.outer.packedWidth / 2.0f, this.orb.cY - (float)OrbFlareEffect.outer.packedHeight / 2.0f, (float)OrbFlareEffect.outer.packedWidth / 2.0f, (float)OrbFlareEffect.outer.packedHeight / 2.0f, OrbFlareEffect.outer.packedWidth, OrbFlareEffect.outer.packedHeight, this.scale, this.scaleY * Settings.scale, MathUtils.random(-2.0f, 2.0f));
        sb.draw(outer, this.orb.cX - (float)OrbFlareEffect.outer.packedWidth / 2.0f, this.orb.cY - (float)OrbFlareEffect.outer.packedHeight / 2.0f, (float)OrbFlareEffect.outer.packedWidth / 2.0f, (float)OrbFlareEffect.outer.packedHeight / 2.0f, OrbFlareEffect.outer.packedWidth, OrbFlareEffect.outer.packedHeight, this.scale, this.scaleY * Settings.scale, MathUtils.random(-2.0f, 2.0f));
        sb.setBlendFunction(770, 771);
        sb.setColor(this.color2);
        sb.draw(inner, this.orb.cX - (float)OrbFlareEffect.inner.packedWidth / 2.0f, this.orb.cY - (float)OrbFlareEffect.inner.packedHeight / 2.0f, (float)OrbFlareEffect.inner.packedWidth / 2.0f, (float)OrbFlareEffect.inner.packedHeight / 2.0f, OrbFlareEffect.inner.packedWidth, OrbFlareEffect.inner.packedHeight, this.scale * Settings.scale * 1.1f, this.scaleY * Settings.scale, MathUtils.random(-1.0f, 1.0f));
    }

    @Override
    public void dispose() {
    }

    public static enum OrbFlareColor {
        LIGHTNING,
        DARK,
        PLASMA,
        FROST;

    }
}

