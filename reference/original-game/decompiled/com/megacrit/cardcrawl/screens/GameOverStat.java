/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class GameOverStat {
    public final String label;
    public final String description;
    public final String value;
    private static final float VALUE_X = 430.0f * Settings.scale;
    public boolean hidden = true;
    private Color color = Settings.CREAM_COLOR.cpy();
    private float offsetX = -50.0f * Settings.scale;
    public Hitbox hb = null;

    public GameOverStat() {
        this.label = null;
        this.description = null;
        this.value = null;
        this.hb = new Hitbox(250.0f * Settings.scale, 32.0f * Settings.scale);
        this.color.a = 0.0f;
    }

    public GameOverStat(String label, String description, String value) {
        this.label = label;
        this.description = description != null && description.equals("") ? null : description;
        this.hb = new Hitbox(250.0f * Settings.scale, 32.0f * Settings.scale);
        this.value = value;
        this.color.a = 0.0f;
    }

    public void update() {
        if (!this.hidden) {
            this.color.a = MathHelper.slowColorLerpSnap(this.color.a, 1.0f);
            this.offsetX = MathHelper.uiLerpSnap(this.offsetX, 0.0f);
            if (this.hb != null) {
                this.hb.update();
                if (this.hb.hovered && this.description != null) {
                    TipHelper.renderGenericTip((float)InputHelper.mX + 80.0f * Settings.scale, InputHelper.mY, this.label, this.description);
                }
            }
        }
    }

    public void renderLine(SpriteBatch sb, boolean isWide, float y) {
        if (isWide) {
            sb.setColor(this.color);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH / 2.0f - 525.0f * Settings.scale, y - 10.0f * Settings.scale, 1050.0f * Settings.scale, 4.0f * Settings.scale);
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.color.a / 4.0f));
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH / 2.0f - 525.0f * Settings.scale, y - 10.0f * Settings.scale, 1050.0f * Settings.scale, 1.0f * Settings.scale);
        } else {
            sb.setColor(this.color);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH / 2.0f - 222.0f * Settings.scale, y - 10.0f * Settings.scale, 434.0f * Settings.scale, 4.0f * Settings.scale);
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.color.a / 4.0f));
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH / 2.0f - 222.0f * Settings.scale, y - 10.0f * Settings.scale, 434.0f * Settings.scale, 1.0f * Settings.scale);
        }
    }

    public void render(SpriteBatch sb, float x, float y) {
        if (this.label == null || this.value == null) {
            return;
        }
        Color prevColor = null;
        if (this.hb != null && this.hb.hovered) {
            prevColor = this.color;
            this.color = new Color(0.0f, 0.9f, 0.0f, this.color.a);
        }
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, this.label, x + this.offsetX, y, this.color);
        FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont, this.value, x + VALUE_X + this.offsetX, y, this.color);
        if (this.hb != null) {
            this.hb.move(x + 120.0f * Settings.scale, y - 8.0f * Settings.scale);
            this.hb.render(sb);
            if (this.hb.hovered) {
                this.color = prevColor;
            }
        }
    }
}

