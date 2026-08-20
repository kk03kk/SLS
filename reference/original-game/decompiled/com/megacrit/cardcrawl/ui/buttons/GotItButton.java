/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class GotItButton {
    public Hitbox hb = new Hitbox(220.0f * Settings.scale, 60.0f * Settings.scale);
    private static final int W = 210;
    private static final int H = 52;
    float x = x += 120.0f * Settings.scale;
    float y = y -= 160.0f * Settings.scale;

    public GotItButton(float x, float y) {
        this.hb.move(x, y);
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
        }
    }

    public void render(SpriteBatch sb) {
        sb.draw(ImageMaster.FTUE_BTN, this.x - 105.0f, this.y - 26.0f, 105.0f, 26.0f, 210.0f, 52.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 210, 52, false, false);
        if (this.hb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.25f));
            sb.draw(ImageMaster.FTUE_BTN, this.x - 105.0f, this.y - 26.0f, 105.0f, 26.0f, 210.0f, 52.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 210, 52, false, false);
            sb.setBlendFunction(770, 771);
        }
        this.hb.render(sb);
    }
}

