/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.StrikeEffect;

public class DamageHeartEffect
extends AbstractGameEffect {
    private static int blockSound = 0;
    public TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float sY;
    private float tY;
    private static final float DURATION = 0.6f;
    private AbstractGameAction.AttackEffect effect;
    private boolean triggered = false;
    private float delayTimer = 2.0f;
    private int damage;

    public DamageHeartEffect(float delay, float x, float y, AbstractGameAction.AttackEffect effect, int damage) {
        this.duration = 0.6f;
        this.delayTimer = delay;
        this.startingDuration = 0.6f;
        this.effect = effect;
        this.img = this.loadImage();
        this.color = Color.WHITE.cpy();
        this.scale = Settings.scale;
        this.damage = damage;
        this.x = x - (float)this.img.packedWidth / 2.0f + MathUtils.random(-150.0f, 150.0f) * Settings.scale;
        y -= (float)this.img.packedHeight / 2.0f + MathUtils.random(-150.0f, 150.0f) * Settings.scale;
        switch (effect) {
            case SHIELD: {
                this.sY = this.y = y + 80.0f * Settings.scale;
                this.tY = y;
                break;
            }
            default: {
                this.y = y;
                this.sY = y;
                this.tY = y;
            }
        }
    }

    private TextureAtlas.AtlasRegion loadImage() {
        switch (this.effect) {
            case SLASH_DIAGONAL: {
                return ImageMaster.ATK_SLASH_D;
            }
            case SLASH_HEAVY: {
                return ImageMaster.ATK_SLASH_HEAVY;
            }
            case SLASH_HORIZONTAL: {
                return ImageMaster.ATK_SLASH_H;
            }
            case SLASH_VERTICAL: {
                return ImageMaster.ATK_SLASH_V;
            }
            case BLUNT_LIGHT: {
                return ImageMaster.ATK_BLUNT_LIGHT;
            }
            case BLUNT_HEAVY: {
                this.rotation = MathUtils.random(360.0f);
                return ImageMaster.ATK_BLUNT_HEAVY;
            }
            case FIRE: {
                return ImageMaster.ATK_FIRE;
            }
            case POISON: {
                return ImageMaster.ATK_POISON;
            }
            case SHIELD: {
                return ImageMaster.ATK_SHIELD;
            }
            case NONE: {
                return null;
            }
        }
        return ImageMaster.ATK_SLASH_D;
    }

    private void playSound(AbstractGameAction.AttackEffect effect) {
        switch (effect) {
            case SLASH_HEAVY: {
                CardCrawlGame.sound.play("ATTACK_HEAVY");
                break;
            }
            case BLUNT_LIGHT: {
                CardCrawlGame.sound.play("BLUNT_FAST");
                break;
            }
            case BLUNT_HEAVY: {
                CardCrawlGame.sound.play("BLUNT_HEAVY");
                break;
            }
            case FIRE: {
                CardCrawlGame.sound.play("ATTACK_FIRE");
                break;
            }
            case POISON: {
                CardCrawlGame.sound.play("ATTACK_POISON");
                break;
            }
            case SHIELD: {
                this.playBlockSound();
                break;
            }
            case NONE: {
                break;
            }
            default: {
                CardCrawlGame.sound.play("ATTACK_FAST");
            }
        }
    }

    private void playBlockSound() {
        if (blockSound == 0) {
            CardCrawlGame.sound.play("BLOCK_GAIN_1");
        } else if (blockSound == 1) {
            CardCrawlGame.sound.play("BLOCK_GAIN_2");
        } else {
            CardCrawlGame.sound.play("BLOCK_GAIN_3");
        }
        if (++blockSound > 2) {
            blockSound = 0;
        }
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            if (this.delayTimer < 0.0f) {
                this.playSound(this.effect);
                AbstractDungeon.effectsQueue.add(new StrikeEffect(null, this.x + (float)this.img.packedWidth / 2.0f, this.y + (float)this.img.packedHeight / 2.0f, this.damage));
            }
            return;
        }
        switch (this.effect) {
            case SHIELD: {
                this.duration -= Gdx.graphics.getDeltaTime();
                if (this.duration < 0.0f) {
                    this.isDone = true;
                    this.color.a = 0.0f;
                } else {
                    this.color.a = this.duration < 0.2f ? this.duration * 5.0f : Interpolation.fade.apply(1.0f, 0.0f, this.duration * 0.75f / 0.6f);
                }
                this.y = Interpolation.exp10In.apply(this.tY, this.sY, this.duration / 0.6f);
                if (!(this.duration < 0.4f) || this.triggered) break;
                this.triggered = true;
                break;
            }
            default: {
                super.update();
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.delayTimer < 0.0f) {
            sb.setColor(this.color);
            sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        }
    }

    @Override
    public void dispose() {
    }
}

