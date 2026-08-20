/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.stance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class StanceAuraEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private TextureAtlas.AtlasRegion img = ImageMaster.EXHAUST_L;
    public static boolean switcher = true;

    public StanceAuraEffect(String stanceId) {
        this.duration = 2.0f;
        this.scale = MathUtils.random(2.7f, 2.5f) * Settings.scale;
        this.color = stanceId.equals("Wrath") ? new Color(MathUtils.random(0.6f, 0.7f), MathUtils.random(0.0f, 0.1f), MathUtils.random(0.1f, 0.2f), 0.0f) : (stanceId.equals("Calm") ? new Color(MathUtils.random(0.5f, 0.55f), MathUtils.random(0.6f, 0.7f), 1.0f, 0.0f) : new Color(MathUtils.random(0.6f, 0.7f), MathUtils.random(0.0f, 0.1f), MathUtils.random(0.6f, 0.7f), 0.0f));
        this.x = AbstractDungeon.player.hb.cX + MathUtils.random(-AbstractDungeon.player.hb.width / 16.0f, AbstractDungeon.player.hb.width / 16.0f);
        this.y = AbstractDungeon.player.hb.cY + MathUtils.random(-AbstractDungeon.player.hb.height / 16.0f, AbstractDungeon.player.hb.height / 12.0f);
        this.x -= (float)this.img.packedWidth / 2.0f;
        this.y -= (float)this.img.packedHeight / 2.0f;
        switcher = !switcher;
        this.renderBehind = true;
        this.rotation = MathUtils.random(360.0f);
        if (switcher) {
            this.renderBehind = true;
            this.vY = MathUtils.random(0.0f, 40.0f);
        } else {
            this.renderBehind = false;
            this.vY = MathUtils.random(0.0f, -40.0f);
        }
    }

    @Override
    public void update() {
        this.color.a = this.duration > 1.0f ? Interpolation.fade.apply(0.3f, 0.0f, this.duration - 1.0f) : Interpolation.fade.apply(0.0f, 0.3f, this.duration);
        this.rotation += Gdx.graphics.getDeltaTime() * this.vY;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

