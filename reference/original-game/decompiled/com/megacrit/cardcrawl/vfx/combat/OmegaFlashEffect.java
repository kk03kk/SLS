/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class OmegaFlashEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img = AbstractPower.atlas.findRegion("128/omega");
    private boolean playedSound = false;

    public OmegaFlashEffect(float x, float y) {
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.duration = this.startingDuration = 0.5f;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        if (!this.playedSound) {
            CardCrawlGame.sound.playA("BLOCK_ATTACK", -0.5f);
            this.playedSound = true;
        }
        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(2.9f, 3.1f), this.scale * MathUtils.random(2.9f, 3.1f), this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(2.9f, 3.1f), this.scale * MathUtils.random(2.9f, 3.1f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

