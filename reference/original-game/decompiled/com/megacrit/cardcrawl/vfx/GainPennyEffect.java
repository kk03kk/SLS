/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.GainGoldTextEffect;
import com.megacrit.cardcrawl.vfx.ShineLinesEffect;

public class GainPennyEffect
extends AbstractGameEffect {
    private static final float GRAVITY = 2000.0f * Settings.scale;
    private static final float START_VY = 800.0f * Settings.scale;
    private static final float START_VY_JITTER = 400.0f * Settings.scale;
    private static final float START_VX = 200.0f * Settings.scale;
    private static final float START_VX_JITTER = 300.0f * Settings.scale;
    private static final float TARGET_JITTER = 50.0f * Settings.scale;
    private float rotationSpeed;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float targetX;
    private float targetY;
    private TextureAtlas.AtlasRegion img = MathUtils.randomBoolean() ? ImageMaster.COPPER_COIN_1 : ImageMaster.COPPER_COIN_2;
    private float alpha = 0.0f;
    private float suctionTimer = 0.7f;
    private float staggerTimer;
    private boolean showGainEffect;
    private AbstractCreature owner;

    public GainPennyEffect(AbstractCreature owner, float x, float y, float targetX, float targetY, boolean showGainEffect) {
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.targetX = targetX + MathUtils.random(-TARGET_JITTER, TARGET_JITTER);
        this.targetY = targetY + MathUtils.random(-TARGET_JITTER, TARGET_JITTER * 2.0f);
        this.showGainEffect = showGainEffect;
        this.owner = owner;
        this.staggerTimer = MathUtils.random(0.0f, 0.5f);
        this.vX = MathUtils.random(START_VX - 50.0f * Settings.scale, START_VX_JITTER);
        this.rotationSpeed = MathUtils.random(500.0f, 2000.0f);
        if (MathUtils.randomBoolean()) {
            this.vX = -this.vX;
            this.rotationSpeed = -this.rotationSpeed;
        }
        this.vY = MathUtils.random(START_VY, START_VY_JITTER);
        this.scale = Settings.scale;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    }

    public GainPennyEffect(float x, float y) {
        this(AbstractDungeon.player, x, y, AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, true);
    }

    @Override
    public void update() {
        if (this.staggerTimer > 0.0f) {
            this.staggerTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        if (this.alpha != 1.0f) {
            this.alpha += Gdx.graphics.getDeltaTime() * 2.0f;
            if (this.alpha > 1.0f) {
                this.alpha = 1.0f;
            }
            this.color.a = this.alpha;
        }
        this.rotation += Gdx.graphics.getDeltaTime() * this.rotationSpeed;
        this.x += Gdx.graphics.getDeltaTime() * this.vX;
        this.y += Gdx.graphics.getDeltaTime() * this.vY;
        this.vY -= Gdx.graphics.getDeltaTime() * GRAVITY;
        if (this.suctionTimer > 0.0f) {
            this.suctionTimer -= Gdx.graphics.getDeltaTime();
        } else {
            this.vY = MathUtils.lerp(this.vY, 0.0f, Gdx.graphics.getDeltaTime() * 5.0f);
            this.vX = MathUtils.lerp(this.vX, 0.0f, Gdx.graphics.getDeltaTime() * 5.0f);
            this.x = MathUtils.lerp(this.x, this.targetX, Gdx.graphics.getDeltaTime() * 4.0f);
            this.y = MathUtils.lerp(this.y, this.targetY, Gdx.graphics.getDeltaTime() * 4.0f);
            if (Math.abs(this.x - this.targetX) < 20.0f) {
                this.isDone = true;
                if (MathUtils.randomBoolean()) {
                    CardCrawlGame.sound.play("GOLD_GAIN", 0.1f);
                }
                if (!this.owner.isPlayer) {
                    this.owner.gainGold(1);
                }
                AbstractDungeon.effectsQueue.add(new ShineLinesEffect(this.x, this.y));
                boolean textEffectFound = false;
                for (AbstractGameEffect e : AbstractDungeon.effectList) {
                    if (!(e instanceof GainGoldTextEffect) || !((GainGoldTextEffect)e).ping(1)) continue;
                    textEffectFound = true;
                    break;
                }
                if (!textEffectFound) {
                    for (AbstractGameEffect e : AbstractDungeon.effectsQueue) {
                        if (!(e instanceof GainGoldTextEffect) || !((GainGoldTextEffect)e).ping(1)) continue;
                        textEffectFound = true;
                    }
                }
                if (!textEffectFound && this.showGainEffect) {
                    AbstractDungeon.effectsQueue.add(new GainGoldTextEffect(1));
                }
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.staggerTimer > 0.0f) {
            return;
        }
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}

