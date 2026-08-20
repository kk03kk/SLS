/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class LightningOrbPassiveEffect
extends AbstractGameEffect {
    private Texture img = null;
    private int index = 0;
    private float x;
    private float y;
    private boolean flipX;
    private boolean flipY;
    private float intervalDuration;

    public LightningOrbPassiveEffect(float x, float y) {
        this.renderBehind = MathUtils.randomBoolean();
        this.x = x;
        this.y = y;
        this.color = Settings.LIGHT_YELLOW_COLOR.cpy();
        this.img = ImageMaster.LIGHTNING_PASSIVE_VFX.get(this.index);
        this.scale = MathUtils.random(0.6f, 1.0f) * Settings.scale;
        this.rotation = MathUtils.random(360.0f);
        if (this.rotation < 120.0f) {
            this.renderBehind = true;
        }
        this.flipX = MathUtils.randomBoolean();
        this.flipY = MathUtils.randomBoolean();
        this.duration = this.intervalDuration = MathUtils.random(0.03f, 0.06f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            ++this.index;
            if (this.index > ImageMaster.LIGHTNING_PASSIVE_VFX.size() - 1) {
                this.isDone = true;
                return;
            }
            this.img = ImageMaster.LIGHTNING_PASSIVE_VFX.get(this.index);
            this.duration = this.intervalDuration;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x - 61.0f, this.y - 61.0f, 61.0f, 61.0f, 122.0f, 122.0f, this.scale, this.scale, this.rotation, 0, 0, 122, 122, this.flipX, this.flipY);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

