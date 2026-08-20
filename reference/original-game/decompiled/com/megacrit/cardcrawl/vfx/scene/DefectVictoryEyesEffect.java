/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DefectVictoryEyesEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private static Texture img;

    public DefectVictoryEyesEffect() {
        this.renderBehind = true;
        if (img == null) {
            img = ImageMaster.loadImage("images/vfx/defect/eyes2.png");
        }
        this.x = (float)Settings.WIDTH / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f - 50.0f * Settings.scale;
        this.scale = 1.5f * Settings.scale;
        this.color = new Color(0.5f, 0.8f, 1.0f, 0.0f);
    }

    @Override
    public void update() {
        this.color.a = MathHelper.slowColorLerpSnap(this.color.a, 0.5f);
        this.duration += Gdx.graphics.getDeltaTime();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(img, this.x - 512.0f, this.y - 180.0f, 512.0f, 180.0f, 1024.0f, 360.0f, this.scale * (MathUtils.cos(this.duration * 4.0f) / 20.0f + 1.0f), this.scale * MathUtils.random(0.99f, 1.01f), this.rotation, 0, 0, 1024, 360, false, false);
        sb.draw(img, this.x - 512.0f, this.y - 180.0f, 512.0f, 180.0f, 1024.0f, 360.0f, this.scale * (MathUtils.cos(this.duration * 5.0f) / 30.0f + 1.0f) * MathUtils.random(0.99f, 1.01f), this.scale * MathUtils.random(0.99f, 1.01f), this.rotation, 0, 0, 1024, 360, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

