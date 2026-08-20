/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.cardManip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.DamageImpactCurvyEffect;

public class PurgeCardEffect
extends AbstractGameEffect {
    private AbstractCard card;
    private static final float PADDING = 30.0f * Settings.scale;
    private float scaleY;
    private Color rarityColor;

    public PurgeCardEffect(AbstractCard card) {
        this(card, (float)Settings.WIDTH - 96.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f * Settings.scale);
    }

    public PurgeCardEffect(AbstractCard card, float x, float y) {
        this.card = card;
        this.duration = this.startingDuration = 2.0f;
        this.identifySpawnLocation(x, y);
        card.drawScale = 0.01f;
        card.targetDrawScale = 1.0f;
        CardCrawlGame.sound.play("CARD_BURN");
        this.initializeVfx();
    }

    private void initializeVfx() {
        switch (this.card.rarity) {
            case UNCOMMON: {
                this.rarityColor = new Color(0.2f, 0.8f, 0.8f, 0.01f);
                break;
            }
            case RARE: {
                this.rarityColor = new Color(0.8f, 0.7f, 0.2f, 0.01f);
                break;
            }
            default: {
                this.rarityColor = new Color(0.6f, 0.6f, 0.6f, 0.01f);
            }
        }
        switch (this.card.color) {
            case BLUE: {
                this.color = new Color(0.1f, 0.4f, 0.7f, 0.01f);
                break;
            }
            case COLORLESS: {
                this.color = new Color(0.4f, 0.4f, 0.4f, 0.01f);
                break;
            }
            case GREEN: {
                this.color = new Color(0.2f, 0.7f, 0.2f, 0.01f);
                break;
            }
            case RED: {
                this.color = new Color(0.9f, 0.3f, 0.2f, 0.01f);
                break;
            }
            default: {
                this.color = new Color(0.2f, 0.15f, 0.2f, 0.01f);
            }
        }
        this.scale = Settings.scale;
        this.scaleY = Settings.scale;
    }

    private void identifySpawnLocation(float x, float y) {
        int effectCount = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof PurgeCardEffect)) continue;
            ++effectCount;
        }
        for (AbstractGameEffect e : AbstractDungeon.topLevelEffects) {
            if (!(e instanceof PurgeCardEffect)) continue;
            ++effectCount;
        }
        this.card.current_x = x;
        this.card.current_y = y;
        this.card.target_y = (float)Settings.HEIGHT * 0.5f;
        switch (effectCount) {
            case 0: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f;
                break;
            }
            case 1: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f - PADDING - AbstractCard.IMG_WIDTH;
                break;
            }
            case 2: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f + PADDING + AbstractCard.IMG_WIDTH;
                break;
            }
            case 3: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f - (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                break;
            }
            case 4: {
                this.card.target_x = (float)Settings.WIDTH * 0.5f + (PADDING + AbstractCard.IMG_WIDTH) * 2.0f;
                break;
            }
            default: {
                this.card.target_x = MathUtils.random((float)Settings.WIDTH * 0.1f, (float)Settings.WIDTH * 0.9f);
                this.card.target_y = MathUtils.random((float)Settings.HEIGHT * 0.2f, (float)Settings.HEIGHT * 0.8f);
            }
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.5f) {
            if (!this.card.fadingOut) {
                this.card.fadingOut = true;
                if (!Settings.DISABLE_EFFECTS) {
                    int i;
                    for (i = 0; i < 16; ++i) {
                        AbstractDungeon.topLevelEffectsQueue.add(new DamageImpactCurvyEffect(this.card.current_x, this.card.current_y, this.color, false));
                    }
                    for (i = 0; i < 8; ++i) {
                        AbstractDungeon.effectsQueue.add(new DamageImpactCurvyEffect(this.card.current_x, this.card.current_y, this.rarityColor, false));
                    }
                }
            }
            this.updateVfx();
        }
        this.card.update();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    private void updateVfx() {
        this.rarityColor.a = this.color.a = MathHelper.fadeLerpSnap(this.color.a, 0.5f);
        this.scale = Interpolation.swingOut.apply(1.6f, 1.0f, this.duration * 2.0f) * Settings.scale;
        this.scaleY = Interpolation.fade.apply(0.005f, 1.0f, this.duration * 2.0f) * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        this.card.render(sb);
        this.renderVfx(sb);
    }

    private void renderVfx(SpriteBatch sb) {
        sb.setColor(this.color);
        TextureAtlas.AtlasRegion img = ImageMaster.CARD_POWER_BG_SILHOUETTE;
        sb.draw(img, this.card.current_x + img.offsetX - (float)img.originalWidth / 2.0f, this.card.current_y + img.offsetY - (float)img.originalHeight / 2.0f, (float)img.originalWidth / 2.0f - img.offsetX, (float)img.originalHeight / 2.0f - img.offsetY, img.packedWidth, img.packedHeight, this.scale * MathUtils.random(0.95f, 1.05f), this.scaleY * MathUtils.random(0.95f, 1.05f), this.rotation);
        sb.setBlendFunction(770, 1);
        sb.setColor(this.rarityColor);
        img = ImageMaster.CARD_SUPER_SHADOW;
        sb.draw(img, this.card.current_x + img.offsetX - (float)img.originalWidth / 2.0f, this.card.current_y + img.offsetY - (float)img.originalHeight / 2.0f, (float)img.originalWidth / 2.0f - img.offsetX, (float)img.originalHeight / 2.0f - img.offsetY, img.packedWidth, img.packedHeight, this.scale * 0.75f * MathUtils.random(0.95f, 1.05f), this.scaleY * 0.75f * MathUtils.random(0.95f, 1.05f), this.rotation);
        sb.draw(img, this.card.current_x + img.offsetX - (float)img.originalWidth / 2.0f, this.card.current_y + img.offsetY - (float)img.originalHeight / 2.0f, (float)img.originalWidth / 2.0f - img.offsetX, (float)img.originalHeight / 2.0f - img.offsetY, img.packedWidth, img.packedHeight, this.scale * 0.5f * MathUtils.random(0.95f, 1.05f), this.scaleY * 0.5f * MathUtils.random(0.95f, 1.05f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

