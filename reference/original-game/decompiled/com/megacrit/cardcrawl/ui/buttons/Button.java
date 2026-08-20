/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class Button {
    public float x;
    public float y;
    private Texture img;
    protected Hitbox hb;
    protected Color activeColor = Color.WHITE;
    protected Color inactiveColor = new Color(0.6f, 0.6f, 0.6f, 1.0f);
    public boolean pressed = false;
    public int height;
    public int width;

    public Button(float x, float y, Texture img) {
        this.x = x;
        this.y = y;
        this.img = img;
        this.hb = new Hitbox(x, y, img.getWidth(), img.getHeight());
        this.height = img.getHeight();
        this.width = img.getWidth();
    }

    public void update() {
        this.hb.update(this.x, this.y);
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.pressed = true;
            InputHelper.justClickedLeft = false;
        }
    }

    public void render(SpriteBatch sb) {
        if (this.hb.hovered) {
            sb.setColor(this.activeColor);
        } else {
            sb.setColor(this.inactiveColor);
        }
        sb.draw(this.img, this.x, this.y);
        sb.setColor(Color.WHITE);
        this.hb.render(sb);
    }
}

