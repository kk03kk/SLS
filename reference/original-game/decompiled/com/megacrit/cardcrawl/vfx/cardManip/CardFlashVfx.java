/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.cardManip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CardFlashVfx
extends AbstractGameEffect {
    private AbstractCard card;
    private TextureAtlas.AtlasRegion img;
    private float yScale = 0.0f;
    private boolean isSuper = false;

    public CardFlashVfx(AbstractCard card, boolean isSuper) {
        this(card, new Color(1.0f, 0.8f, 0.2f, 0.0f), isSuper);
    }

    public CardFlashVfx(AbstractCard card, Color c, boolean isSuper) {
        this.card = card;
        this.isSuper = isSuper;
        this.duration = 0.5f;
        if (isSuper) {
            this.img = ImageMaster.CARD_FLASH_VFX;
        } else {
            switch (card.type) {
                case POWER: {
                    this.img = ImageMaster.CARD_POWER_BG_SILHOUETTE;
                    break;
                }
                case ATTACK: {
                    this.img = ImageMaster.CARD_ATTACK_BG_SILHOUETTE;
                    break;
                }
                default: {
                    this.img = ImageMaster.CARD_SKILL_BG_SILHOUETTE;
                }
            }
        }
        this.color = c;
    }

    public CardFlashVfx(AbstractCard card) {
        this(card, new Color(1.0f, 0.8f, 0.2f, 0.0f), false);
    }

    public CardFlashVfx(AbstractCard card, Color c) {
        this.card = card;
        this.duration = 0.5f;
        switch (card.type) {
            case POWER: {
                this.img = ImageMaster.CARD_POWER_BG_SILHOUETTE;
                break;
            }
            case ATTACK: {
                this.img = ImageMaster.CARD_ATTACK_BG_SILHOUETTE;
                break;
            }
            default: {
                this.img = ImageMaster.CARD_SKILL_BG_SILHOUETTE;
            }
        }
        this.color = c;
        this.isSuper = false;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else {
            this.yScale = Interpolation.bounceIn.apply(1.2f, 1.1f, this.duration * 2.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.color.a = this.duration;
        sb.setColor(this.color);
        if (this.isSuper) {
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.52f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.53f) * Settings.scale, this.card.angle);
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.55f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.57f) * Settings.scale, this.card.angle);
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.58f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.6f) * Settings.scale, this.card.angle);
        } else {
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.52f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.52f) * Settings.scale, this.card.angle);
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.55f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.55f) * Settings.scale, this.card.angle);
            sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.card.drawScale * ((this.yScale + 1.0f) * 0.58f) * Settings.scale, this.card.drawScale * ((this.yScale + 1.0f) * 0.58f) * Settings.scale, this.card.angle);
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}

