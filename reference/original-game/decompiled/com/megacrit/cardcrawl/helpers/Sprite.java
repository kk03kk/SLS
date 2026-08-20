/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.AbstractDrawable;
import com.megacrit.cardcrawl.helpers.FontHelper;

public class Sprite
extends AbstractDrawable {
    private Texture img;
    private String label;
    private Color color;
    private float x;
    private float y;
    private float scale;
    private float rotation;
    private boolean text;

    public Sprite(Texture img, float x, float y, int z, float scale, float rotation, Color color) {
        this.img = img;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.rotation = rotation;
        this.color = color;
        this.text = false;
    }

    public Sprite(Texture img, float x, float y, int z, float scale, Color color) {
        this(img, x, y, z, Settings.scale, 0.0f, color);
    }

    public Sprite(Texture img, float x, float y, int z) {
        this(img, x, y, z, Settings.scale, null);
    }

    public Sprite(Texture img, float x, float y, int z, Color color) {
        this(img, x, y, z, Settings.scale, color);
    }

    public Sprite(String label, float x, float y, int z, Color color) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.text = true;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.text) {
            int w = this.img.getWidth();
            int h = this.img.getHeight();
            if (this.color != null) {
                sb.setColor(this.color);
            }
            if (this.isVisible()) {
                sb.draw(this.img, this.x - (float)w / 2.0f, this.y - (float)h / 2.0f, (float)w / 2.0f, (float)h / 2.0f, w, h, this.scale, this.scale, this.rotation, 0, 0, w, h, false, false);
            }
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.panelEndTurnFont, this.label, this.x, this.y, this.color);
        }
    }

    private boolean isVisible() {
        return true;
    }
}

