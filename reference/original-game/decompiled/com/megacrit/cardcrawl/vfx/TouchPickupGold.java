/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ShineLinesEffect;

public class TouchPickupGold
extends AbstractGameEffect {
    private static final float RAW_IMG_WIDTH = 32.0f;
    private static final float IMG_WIDTH = 32.0f * Settings.scale;
    private TextureAtlas.AtlasRegion img = MathUtils.randomBoolean() ? ImageMaster.COPPER_COIN_1 : ImageMaster.COPPER_COIN_2;
    private boolean isPickupable = false;
    public boolean pickedup = false;
    private float x;
    private float y;
    private float landingY;
    private boolean willBounce;
    private boolean hasBounced = true;
    private float bounceY;
    private float bounceX;
    private float vY = -0.2f;
    private float vX = 0.0f;
    private float gravity = -0.3f;
    private Hitbox hitbox;

    public TouchPickupGold() {
        boolean bl = this.willBounce = MathUtils.random(3) != 0;
        if (this.willBounce) {
            this.hasBounced = false;
            this.bounceY = MathUtils.random(1.0f, 4.0f);
            this.bounceX = MathUtils.random(-3.0f, 3.0f);
        }
        this.y = (float)Settings.HEIGHT * MathUtils.random(1.1f, 1.3f) - (float)this.img.packedHeight / 2.0f;
        this.x = MathUtils.random((float)Settings.WIDTH * 0.3f, (float)Settings.WIDTH * 0.95f) - (float)this.img.packedWidth / 2.0f;
        this.landingY = MathUtils.random(AbstractDungeon.floorY - (float)Settings.HEIGHT * 0.05f, AbstractDungeon.floorY + (float)Settings.HEIGHT * 0.08f);
        this.rotation = MathUtils.random(360.0f);
        this.scale = Settings.scale;
    }

    public TouchPickupGold(boolean centerOnPlayer) {
        this();
        if (centerOnPlayer) {
            this.x = MathUtils.random(AbstractDungeon.player.drawX - AbstractDungeon.player.hb_w, AbstractDungeon.player.drawX + AbstractDungeon.player.hb_w);
            this.gravity = -0.7f;
        }
    }

    @Override
    public void update() {
        if (!this.isPickupable) {
            this.x += this.vX * Gdx.graphics.getDeltaTime() * 60.0f;
            this.y += this.vY * Gdx.graphics.getDeltaTime() * 60.0f;
            this.vY += this.gravity;
            if (this.y < this.landingY) {
                if (this.hasBounced) {
                    this.y = this.landingY;
                    this.isPickupable = true;
                    this.hitbox = new Hitbox(this.x - IMG_WIDTH * 2.0f, this.y - IMG_WIDTH * 2.0f, IMG_WIDTH * 4.0f, IMG_WIDTH * 4.0f);
                } else {
                    if (MathUtils.random(1) == 0) {
                        this.hasBounced = true;
                    }
                    this.y = this.landingY;
                    this.vY = this.bounceY;
                    this.vX = this.bounceX;
                    this.bounceY *= 0.5f;
                    this.bounceX *= 0.3f;
                }
            }
        } else if (!this.pickedup) {
            this.pickedup = true;
            this.isDone = true;
            this.playGainGoldSFX();
            AbstractDungeon.effectsQueue.add(new ShineLinesEffect(this.x, this.y));
        }
    }

    private void playGainGoldSFX() {
        int roll = MathUtils.random(2);
        switch (roll) {
            case 0: {
                CardCrawlGame.sound.play("GOLD_GAIN", 0.1f);
                break;
            }
            case 1: {
                CardCrawlGame.sound.play("GOLD_GAIN_3", 0.1f);
                break;
            }
            default: {
                CardCrawlGame.sound.play("GOLD_GAIN_5", 0.1f);
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        if (this.hitbox != null) {
            this.hitbox.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

