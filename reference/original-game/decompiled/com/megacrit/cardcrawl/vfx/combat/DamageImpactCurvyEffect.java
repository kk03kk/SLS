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
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;

public class DamageImpactCurvyEffect
extends AbstractGameEffect {
    private Vector2 pos = new Vector2();
    private float speed;
    private float speedStart;
    private float speedTarget;
    private float waveIntensity;
    private float waveSpeed;
    private TextureAtlas.AtlasRegion img;
    private ArrayList<Vector2> positions = new ArrayList();

    public DamageImpactCurvyEffect(float x, float y, Color color, boolean renderBehind) {
        this.img = ImageMaster.STRIKE_LINE_2;
        this.startingDuration = this.duration = MathUtils.random(0.8f, 1.1f);
        this.pos.x = x - (float)this.img.packedWidth / 2.0f;
        this.pos.y = y - (float)this.img.packedHeight / 2.0f;
        this.speedStart = this.speed = MathUtils.random(400.0f, 900.0f) * Settings.scale;
        this.speedTarget = MathUtils.random(200.0f, 300.0f) * Settings.scale;
        this.color = color;
        this.renderBehind = renderBehind;
        this.rotation = MathUtils.random(360.0f);
        this.waveIntensity = MathUtils.random(5.0f, 30.0f);
        this.waveSpeed = MathUtils.random(-20.0f, 20.0f);
        this.speedTarget = MathUtils.random(0.1f, 0.5f);
    }

    public DamageImpactCurvyEffect(float x, float y) {
        this(x, y, Color.GOLDENROD, true);
    }

    @Override
    public void update() {
        this.positions.add(this.pos);
        Vector2 tmp = new Vector2(MathUtils.cosDeg(this.rotation), MathUtils.sinDeg(this.rotation));
        tmp.x *= this.speed * Gdx.graphics.getDeltaTime();
        tmp.y *= this.speed * Gdx.graphics.getDeltaTime();
        this.speed = Interpolation.pow2OutInverse.apply(this.speedStart, this.speedTarget, 1.0f - this.duration / this.startingDuration);
        this.pos.x += tmp.x;
        this.pos.y += tmp.y;
        this.rotation += MathUtils.cos(this.duration * this.waveSpeed) * this.waveIntensity * Gdx.graphics.getDeltaTime() * 60.0f;
        this.scale = Settings.scale * this.duration / this.startingDuration * 0.75f;
        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        Color tmp = this.color.cpy();
        tmp.a = 0.25f;
        for (int i = this.positions.size() - 1; i > 0; --i) {
            sb.setColor(tmp);
            tmp.a *= 0.95f;
            if (!(tmp.a > 0.05f)) continue;
            sb.draw(this.img, this.positions.get((int)i).x, this.positions.get((int)i).y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 2.0f, this.scale * 2.0f, this.rotation);
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

