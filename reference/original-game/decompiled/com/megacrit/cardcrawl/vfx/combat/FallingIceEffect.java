/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.IceShatterEffect;

public class FallingIceEffect
extends AbstractGameEffect {
    private float waitTimer;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float floorY;
    private Texture img;
    private int frostCount = 0;

    public FallingIceEffect(int frostCount, boolean flipped) {
        this.frostCount = frostCount;
        switch (MathUtils.random(2)) {
            case 0: {
                this.img = ImageMaster.FROST_ORB_RIGHT;
                break;
            }
            case 1: {
                this.img = ImageMaster.FROST_ORB_LEFT;
                break;
            }
            default: {
                this.img = ImageMaster.FROST_ORB_MIDDLE;
            }
        }
        this.waitTimer = MathUtils.random(0.0f, 0.5f);
        if (flipped) {
            this.x = MathUtils.random(420.0f, 1420.0f) * Settings.scale - 48.0f;
            this.vX = MathUtils.random(-600.0f, -900.0f);
            this.vX += (float)frostCount * 5.0f;
        } else {
            this.x = MathUtils.random(500.0f, 1500.0f) * Settings.scale - 48.0f;
            this.vX = MathUtils.random(600.0f, 900.0f);
            this.vX -= (float)frostCount * 5.0f;
        }
        this.y = (float)Settings.HEIGHT + MathUtils.random(100.0f, 300.0f) - 48.0f;
        this.vY = MathUtils.random(2500.0f, 4000.0f);
        this.vY -= (float)frostCount * 10.0f;
        this.vY *= Settings.scale;
        this.vX *= Settings.scale;
        this.duration = 2.0f;
        this.scale = MathUtils.random(1.0f, 1.5f);
        this.scale += (float)frostCount * 0.04f;
        this.vX *= this.scale;
        this.scale *= Settings.scale;
        this.color = new Color(0.9f, 0.9f, 1.0f, MathUtils.random(0.9f, 1.0f));
        Vector2 derp = new Vector2(this.vX, this.vY);
        this.rotation = flipped ? derp.angle() + 225.0f - (float)frostCount / 3.0f : derp.angle() - 45.0f + (float)frostCount / 3.0f;
        this.renderBehind = MathUtils.randomBoolean();
        this.floorY = AbstractDungeon.floorY + MathUtils.random(-200.0f, 50.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.waitTimer -= Gdx.graphics.getDeltaTime();
        if (this.waitTimer > 0.0f) {
            return;
        }
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y -= this.vY * Gdx.graphics.getDeltaTime();
        if (this.y < this.floorY) {
            float pitch = 0.8f;
            pitch -= (float)this.frostCount * 0.025f;
            CardCrawlGame.sound.playA("ORB_FROST_EVOKE", pitch += MathUtils.random(-0.2f, 0.2f));
            for (int i = 0; i < 4; ++i) {
                AbstractDungeon.effectsQueue.add(new IceShatterEffect(this.x, this.y));
            }
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.waitTimer < 0.0f) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            sb.draw(this.img, this.x, this.y, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, this.rotation, 0, 0, 96, 96, false, false);
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}

