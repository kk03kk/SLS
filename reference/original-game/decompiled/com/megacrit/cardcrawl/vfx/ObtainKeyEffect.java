/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ObtainKeyEffect
extends AbstractGameEffect {
    private Texture img;
    private float x;
    private float y;
    private KeyColor keyColor = null;

    public ObtainKeyEffect(KeyColor keyColor) {
        this.keyColor = keyColor;
        switch (keyColor) {
            case RED: {
                this.img = ImageMaster.RUBY_KEY;
                break;
            }
            case GREEN: {
                this.img = ImageMaster.EMERALD_KEY;
                break;
            }
            case BLUE: {
                this.img = ImageMaster.SAPPHIRE_KEY;
                break;
            }
        }
        this.duration = 0.33f;
        this.startingDuration = 0.33f;
        this.x = -32.0f + 46.0f * Settings.scale;
        this.y = (float)(Settings.HEIGHT - 32) - 35.0f * Settings.scale;
        this.color = Color.WHITE.cpy();
        this.color.a = 0.0f;
        this.rotation = 180.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
            this.color.a = 0.0f;
            CardCrawlGame.sound.playA("KEY_OBTAIN", -0.2f);
            switch (this.keyColor) {
                case RED: {
                    this.img = ImageMaster.RUBY_KEY;
                    Settings.hasRubyKey = true;
                    break;
                }
                case GREEN: {
                    this.img = ImageMaster.EMERALD_KEY;
                    Settings.hasEmeraldKey = true;
                    break;
                }
                case BLUE: {
                    this.img = ImageMaster.SAPPHIRE_KEY;
                    Settings.hasSapphireKey = true;
                    break;
                }
            }
        } else {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.duration * 3.0f);
            this.scale = Interpolation.pow4In.apply(1.1f, 5.0f, this.duration * 3.0f) * Settings.scale;
            this.rotation = Interpolation.pow4In.apply(0.0f, 180.0f, this.duration * 3.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.duration * 3.0f));
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    public static enum KeyColor {
        RED,
        GREEN,
        BLUE;

    }
}

