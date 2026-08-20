/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

public class OnSaleTag {
    public AbstractCard card;
    private static final float W = 128.0f * Settings.scale;
    public static Texture img = null;

    public OnSaleTag(AbstractCard c) {
        this.card = c;
        if (img == null) {
            switch (Settings.language) {
                case DEU: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/deu.png");
                    break;
                }
                case EPO: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/epo.png");
                    break;
                }
                case FIN: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/fin.png");
                    break;
                }
                case FRA: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/fra.png");
                    break;
                }
                case ITA: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/ita.png");
                    break;
                }
                case JPN: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/jpn.png");
                    break;
                }
                case KOR: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/kor.png");
                    break;
                }
                case RUS: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/rus.png");
                    break;
                }
                case THA: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/tha.png");
                    break;
                }
                case UKR: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/ukr.png");
                    break;
                }
                case ZHS: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/zhs.png");
                    break;
                }
                default: {
                    img = ImageMaster.loadImage("images/npcs/sale_tag/eng.png");
                }
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(img, this.card.current_x + 30.0f * Settings.scale + (this.card.drawScale - 0.75f) * 60.0f * Settings.scale, this.card.current_y + 70.0f * Settings.scale + (this.card.drawScale - 0.75f) * 90.0f * Settings.scale, W * this.card.drawScale, W * this.card.drawScale);
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, (MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) + 1.25f) / 3.0f));
        sb.draw(img, this.card.current_x + 30.0f * Settings.scale + (this.card.drawScale - 0.75f) * 60.0f * Settings.scale, this.card.current_y + 70.0f * Settings.scale + (this.card.drawScale - 0.75f) * 90.0f * Settings.scale, W * this.card.drawScale, W * this.card.drawScale);
        sb.setBlendFunction(770, 771);
    }
}

