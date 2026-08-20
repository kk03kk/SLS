/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class MoveNameEffect
extends AbstractGameEffect {
    private static final float TEXT_DURATION = 2.5f;
    private static final float STARTING_OFFSET_Y = 100.0f * Settings.scale;
    private static final float TARGET_OFFSET_Y = 120.0f * Settings.scale;
    private float x;
    private float y;
    private float offsetY;
    private String msg;
    private Color color2 = Color.BLACK.cpy();
    private TextureAtlas.AtlasRegion img = ImageMaster.MOVE_NAME_BG;

    public MoveNameEffect(float x, float y, String msg) {
        this.duration = 2.5f;
        this.startingDuration = 2.5f;
        if (msg == null) {
            this.isDone = true;
        } else {
            this.msg = msg;
        }
        this.x = x;
        this.y = y;
        this.color = Settings.CREAM_COLOR.cpy();
    }

    @Override
    public void update() {
        this.offsetY = Interpolation.exp10In.apply(TARGET_OFFSET_Y, STARTING_OFFSET_Y, this.duration / 2.5f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration > 2.0f) {
            this.color2.a = this.color.a = Interpolation.fade.apply(1.0f - (this.duration - 2.0f) / 0.5f);
        } else if (this.duration < 1.0f) {
            this.color2.a = this.color.a = Interpolation.fade.apply(this.duration);
        } else {
            this.color.a = 1.0f;
            this.color2.a = 1.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color2);
        sb.draw(this.img, this.x - (float)this.img.packedWidth / 2.0f, this.y - (float)this.img.packedHeight / 2.0f + this.offsetY, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, this.msg, this.x, this.y + this.offsetY, this.color, 1.0f);
    }

    @Override
    public void dispose() {
    }
}

