/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.compendium.CardLibSortHeader;
import com.megacrit.cardcrawl.screens.mainMenu.SortHeaderButtonListener;

public class SortHeaderButton {
    public Hitbox hb = new Hitbox(210.0f * Settings.xScale, 48.0f * Settings.scale);
    private boolean isAscending = false;
    private boolean isActive = false;
    private String text;
    public SortHeaderButtonListener delegate;
    private final float ARROW_SIZE = 32.0f;
    public float textWidth;

    public SortHeaderButton(String text, float cx, float cy) {
        this.hb.move(cx, cy);
        this.text = text;
        this.textWidth = FontHelper.getSmartWidth(FontHelper.topPanelInfoFont, text, Float.MAX_VALUE, 0.0f);
    }

    public SortHeaderButton(String text, float cx, float cy, SortHeaderButtonListener delegate) {
        this(text, cx, cy);
        this.delegate = delegate;
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
        }
        if (this.hb.clicked || this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            this.hb.clicked = false;
            this.isAscending = !this.isAscending;
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.2f);
            if (this.delegate instanceof CardLibSortHeader) {
                ((CardLibSortHeader)this.delegate).clearActiveButtons();
            }
            this.delegate.didChangeOrder(this, this.isAscending);
        }
    }

    public void updateScrollPosition(float newY) {
        this.hb.move(this.hb.cX, newY);
    }

    public void render(SpriteBatch sb) {
        Color color = this.hb.hovered || this.isActive ? Settings.GOLD_COLOR : Settings.CREAM_COLOR;
        FontHelper.renderFontCentered(sb, FontHelper.topPanelInfoFont, this.text, this.hb.cX, this.hb.cY, color);
        sb.setColor(color);
        float arrowCenterOffset = 16.0f;
        sb.draw(ImageMaster.FILTER_ARROW, this.hb.cX - 16.0f + (this.textWidth / 2.0f + 16.0f * Settings.xScale), this.hb.cY - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 32, 32, false, !this.isAscending);
        this.hb.render(sb);
    }

    public void reset() {
        this.isAscending = false;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}

