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
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CardGlowBorder
extends AbstractGameEffect {
    private AbstractCard card;
    private TextureAtlas.AtlasRegion img;
    private float scale;

    public CardGlowBorder(AbstractCard card) {
        this(card, Color.valueOf("30c8dcff"));
    }

    public CardGlowBorder(AbstractCard card, Color gColor) {
        this.card = card;
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
        this.duration = 1.2f;
        this.color = AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT ? gColor.cpy() : Color.GREEN.cpy();
    }

    @Override
    public void update() {
        this.scale = (1.0f + Interpolation.pow2Out.apply(0.03f, 0.11f, 1.0f - this.duration)) * this.card.drawScale * Settings.scale;
        this.color.a = this.duration / 2.0f;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.card.current_x + this.img.offsetX - (float)this.img.originalWidth / 2.0f, this.card.current_y + this.img.offsetY - (float)this.img.originalHeight / 2.0f, (float)this.img.originalWidth / 2.0f - this.img.offsetX, (float)this.img.originalHeight / 2.0f - this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.card.angle);
    }

    @Override
    public void dispose() {
    }
}

