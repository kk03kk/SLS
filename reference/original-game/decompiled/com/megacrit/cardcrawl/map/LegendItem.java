/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.map.Legend;

public class LegendItem {
    private static final float ICON_X = 1575.0f * Settings.xScale;
    private static final float TEXT_X = 1670.0f * Settings.xScale;
    private static final float SPACE_Y = Settings.isMobile ? 64.0f * Settings.yScale : 58.0f * Settings.yScale;
    private static final float OFFSET_Y = Settings.isMobile ? 110.0f * Settings.yScale : 100.0f * Settings.yScale;
    private Texture img;
    private static final int W = 128;
    private int index;
    private String label;
    private String header;
    private String body;
    public Hitbox hb = new Hitbox(230.0f * Settings.xScale, SPACE_Y - 2.0f);

    public LegendItem(String label, Texture img, String tipHeader, String tipBody, int index) {
        this.label = label;
        this.img = img;
        this.header = tipHeader;
        this.body = tipBody;
        this.index = index;
    }

    public void update() {
        this.hb.update();
        if (this.hb.hovered) {
            TipHelper.renderGenericTip(1500.0f * Settings.xScale, 270.0f * Settings.scale, this.header, this.body);
        }
    }

    public void render(SpriteBatch sb, Color c) {
        sb.setColor(c);
        if (!Settings.isMobile) {
            if (this.hb.hovered) {
                sb.draw(this.img, ICON_X - 64.0f, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale / 1.2f, Settings.scale / 1.2f, 0.0f, 0, 0, 128, 128, false, false);
            } else {
                sb.draw(this.img, ICON_X - 64.0f, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale / 1.65f, Settings.scale / 1.65f, 0.0f, 0, 0, 128, 128, false, false);
            }
        } else if (this.hb.hovered) {
            sb.draw(this.img, ICON_X - 64.0f, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
        } else {
            sb.draw(this.img, ICON_X - 64.0f, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale / 1.3f, Settings.scale / 1.3f, 0.0f, 0, 0, 128, 128, false, false);
        }
        if (Settings.isMobile) {
            FontHelper.panelNameFont.getData().setScale(1.2f);
        }
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, this.label, TEXT_X - 50.0f * Settings.scale, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y + 13.0f * Settings.yScale, c);
        if (Settings.isMobile) {
            FontHelper.panelNameFont.getData().setScale(1.0f);
        }
        this.hb.move(TEXT_X, Legend.Y - SPACE_Y * (float)this.index + OFFSET_Y);
        if (c.a != 0.0f) {
            this.hb.render(sb);
        }
    }
}

