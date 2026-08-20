/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect;

public class InteractableTorchEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private Hitbox hb;
    private boolean activated = true;
    private float particleTimer1 = 0.0f;
    private static final float PARTICLE_EMIT_INTERVAL = 0.1f;
    private static TextureAtlas.AtlasRegion img;
    private TorchSize size = TorchSize.M;

    public InteractableTorchEffect(float x, float y, TorchSize size) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("env/torch");
        }
        this.size = size;
        this.x = x;
        this.y = y;
        if (Settings.isLetterbox) {
            this.y += Settings.LETTERBOX_OFFSET_Y;
        }
        this.hb = new Hitbox(50.0f * Settings.scale, 60.0f * Settings.scale);
        this.hb.move(x, this.y);
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.4f);
        switch (size) {
            case S: {
                this.scale = Settings.scale * 0.6f;
                break;
            }
            case M: {
                this.scale = Settings.scale;
                break;
            }
            case L: {
                this.scale = Settings.scale * 1.4f;
                break;
            }
        }
    }

    public InteractableTorchEffect(float x, float y) {
        this(x, y, TorchSize.M);
    }

    @Override
    public void update() {
        this.hb.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            boolean bl = this.activated = !this.activated;
            if (this.activated) {
                CardCrawlGame.sound.playA("ATTACK_FIRE", 0.4f);
            } else {
                CardCrawlGame.sound.play("SCENE_TORCH_EXTINGUISH");
            }
        }
        if (this.activated && !Settings.DISABLE_EFFECTS) {
            this.particleTimer1 -= Gdx.graphics.getDeltaTime();
            if (this.particleTimer1 < 0.0f) {
                this.particleTimer1 = 0.1f;
                switch (this.size) {
                    case S: {
                        AbstractDungeon.effectsQueue.add(new TorchParticleSEffect(this.x, this.y - 10.0f * Settings.scale));
                        AbstractDungeon.effectsQueue.add(new LightFlareSEffect(this.x, this.y - 10.0f * Settings.scale));
                        break;
                    }
                    case M: {
                        AbstractDungeon.effectsQueue.add(new TorchParticleMEffect(this.x, this.y));
                        AbstractDungeon.effectsQueue.add(new LightFlareMEffect(this.x, this.y));
                        break;
                    }
                    case L: {
                        AbstractDungeon.effectsQueue.add(new TorchParticleLEffect(this.x, this.y + 14.0f * Settings.scale));
                        AbstractDungeon.effectsQueue.add(new LightFlareLEffect(this.x, this.y + 14.0f * Settings.scale));
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (Settings.DISABLE_EFFECTS) {
            return;
        }
        sb.setColor(this.color);
        sb.draw(img, this.x - (float)(InteractableTorchEffect.img.packedWidth / 2), this.y - (float)(InteractableTorchEffect.img.packedHeight / 2) - 24.0f * Settings.yScale, (float)InteractableTorchEffect.img.packedWidth / 2.0f, (float)InteractableTorchEffect.img.packedHeight / 2.0f, InteractableTorchEffect.img.packedWidth, InteractableTorchEffect.img.packedHeight, this.scale, this.scale, this.rotation);
        this.hb.render(sb);
    }

    @Override
    public void dispose() {
    }

    public static enum TorchSize {
        S,
        M,
        L;

    }
}

