/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.runHistory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Clipboard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class CopyableTextElement {
    private static final float HITBOX_PADDING = 4.0f * Settings.scale;
    private String text = "";
    private String copyText = "";
    private Hitbox hb;
    private BitmapFont font;

    public CopyableTextElement(BitmapFont font) {
        this.font = font;
        float height = font.getLineHeight() * Settings.scale;
        this.hb = new Hitbox(0.0f, height + 2.0f * HITBOX_PADDING);
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        } else if (this.hb.clicked) {
            this.hb.clicked = false;
            this.copySeedToSystem();
        }
    }

    private void copySeedToSystem() {
        Clipboard clipBoard = Gdx.app.getClipboard();
        clipBoard.setContents(this.copyText);
    }

    public void render(SpriteBatch sb, float x, float y) {
        Color textColor = this.hb.hovered ? Settings.GREEN_TEXT_COLOR : Settings.GOLD_COLOR;
        this.renderSmallText(sb, this.getText(), x, y, textColor);
        this.hb.translate(x - HITBOX_PADDING, y - this.hb.height + HITBOX_PADDING);
        this.hb.render(sb);
    }

    public float approximateWidth() {
        return FontHelper.getSmartWidth(this.font, this.getText(), Float.MAX_VALUE, 36.0f * Settings.scale);
    }

    private void renderSmallText(SpriteBatch sb, String text, float x, float y, Color color) {
        FontHelper.renderSmartText(sb, this.font, text, x, y, Float.MAX_VALUE, 36.0f * Settings.scale, color);
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
        this.hb.width = this.approximateWidth() + 2.0f * HITBOX_PADDING;
    }

    public void setText(String text, String copyText) {
        this.setText(text);
        this.copyText = copyText;
    }
}

