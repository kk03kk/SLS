/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect;

public class WaterDropEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private int frame = 0;
    private float animTimer = 0.1f;
    private static final int W = 64;

    public WaterDropEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.frame = 0;
        this.scale = MathUtils.random(2.5f, 3.0f) * Settings.scale;
        this.rotation = 0.0f;
        this.scale *= Settings.scale;
        this.color = new Color(1.0f, 0.05f, 0.05f, 0.0f);
    }

    @Override
    public void update() {
        this.color.a = MathHelper.fadeLerpSnap(this.color.a, 1.0f);
        this.animTimer -= Gdx.graphics.getDeltaTime();
        if (this.animTimer < 0.0f) {
            this.animTimer += 0.1f;
            ++this.frame;
            if (this.frame == 3) {
                for (int i = 0; i < 3; ++i) {
                    AbstractDungeon.effectsQueue.add(new WaterSplashParticleEffect(this.x, this.y));
                }
            }
            if (this.frame > 5) {
                this.frame = 5;
                this.isDone = true;
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        switch (this.frame) {
            case 0: {
                sb.draw(ImageMaster.WATER_DROP_VFX[0], this.x - 32.0f, this.y - 32.0f + 40.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
            case 1: {
                sb.draw(ImageMaster.WATER_DROP_VFX[1], this.x - 32.0f, this.y - 32.0f + 20.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
            case 2: {
                sb.draw(ImageMaster.WATER_DROP_VFX[2], this.x - 32.0f, this.y - 32.0f + 10.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
            case 3: {
                sb.draw(ImageMaster.WATER_DROP_VFX[3], this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
            case 4: {
                sb.draw(ImageMaster.WATER_DROP_VFX[4], this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
            case 5: {
                sb.draw(ImageMaster.WATER_DROP_VFX[5], this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
                break;
            }
        }
    }

    @Override
    public void dispose() {
    }
}

