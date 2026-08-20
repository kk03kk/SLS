/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.custom;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.CharacterStrings;

public class CustomModeCharacterButton {
    private CharacterStrings charStrings;
    private Texture buttonImg;
    public AbstractPlayer c;
    public boolean selected = false;
    public boolean locked = false;
    public Hitbox hb;
    private static final int W = 128;
    public float x;
    public float y;
    private Color highlightColor = new Color(1.0f, 0.8f, 0.2f, 0.0f);
    private float drawScale = 1.0f;

    public CustomModeCharacterButton(AbstractPlayer c, boolean locked) {
        this.buttonImg = c.getCustomModeCharacterButtonImage();
        this.charStrings = c.getCharacterString();
        this.hb = Settings.isMobile ? new Hitbox(120.0f * Settings.scale, 120.0f * Settings.scale) : new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        this.drawScale = Settings.isMobile ? Settings.scale * 1.2f : Settings.scale;
        this.locked = locked;
        this.c = c;
    }

    public void move(float x, float y) {
        this.x = x;
        this.y = y;
        this.hb.move(x, y);
    }

    public void update(float x, float y) {
        this.x = x;
        this.y = y;
        this.hb.move(x, y);
        this.updateHitbox();
    }

    private void updateHitbox() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
        }
        if (InputHelper.justClickedLeft && !this.locked && this.hb.hovered) {
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.4f);
            this.hb.clickStarted = true;
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            if (!this.selected) {
                CardCrawlGame.mainMenuScreen.customModeScreen.deselectOtherOptions(this);
                this.selected = true;
                CardCrawlGame.chosenCharacter = this.c.chosenClass;
                CardCrawlGame.mainMenuScreen.customModeScreen.confirmButton.isDisabled = false;
                CardCrawlGame.mainMenuScreen.customModeScreen.confirmButton.show();
                CardCrawlGame.sound.playA(this.c.getCustomModeCharacterButtonSoundKey(), MathUtils.random(-0.2f, 0.2f));
            }
        }
    }

    public void render(SpriteBatch sb) {
        this.renderOptionButton(sb);
        if (this.hb.hovered) {
            TipHelper.renderGenericTip((float)InputHelper.mX + 180.0f * Settings.scale, this.hb.cY + 40.0f * Settings.scale, this.charStrings.NAMES[0], this.charStrings.TEXT[0]);
        }
        this.hb.render(sb);
    }

    private void renderOptionButton(SpriteBatch sb) {
        if (this.selected) {
            this.highlightColor.a = 0.25f + (MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) + 1.25f) / 3.5f;
            sb.setColor(this.highlightColor);
            sb.draw(ImageMaster.FILTER_GLOW_BG, this.hb.cX - 64.0f, this.hb.cY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.drawScale, this.drawScale, 0.0f, 0, 0, 128, 128, false, false);
        }
        if (this.locked) {
            ShaderHelper.setShader(sb, ShaderHelper.Shader.GRAYSCALE);
        } else if (this.selected || this.hb.hovered) {
            sb.setColor(Color.WHITE);
        } else {
            sb.setColor(Color.LIGHT_GRAY);
        }
        sb.draw(this.buttonImg, this.hb.cX - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.drawScale, this.drawScale, 0.0f, 0, 0, 128, 128, false, false);
        if (this.locked) {
            ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
        }
    }
}

