/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;

public class AbandonRunButton {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("AbandonRunButton");
    public static final String[] TEXT = AbandonRunButton.uiStrings.TEXT;
    private int W = 440;
    private int H = 100;
    private Hitbox hb = new Hitbox(340.0f * Settings.scale, 70.0f * Settings.scale);
    private float x = 1430.0f * Settings.xScale;
    private float y = Settings.OPTION_Y + 340.0f * Settings.scale;

    public AbandonRunButton() {
        this.hb.move(this.x, this.y);
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (InputHelper.justClickedLeft && this.hb.hovered) {
            CardCrawlGame.sound.play("UI_CLICK_1");
            this.hb.clickStarted = true;
        }
        if (this.hb.clicked || CInputActionSet.proceed.isJustPressed()) {
            CInputActionSet.proceed.unpress();
            this.hb.clicked = false;
            AbstractDungeon.settingsScreen.popup(ConfirmPopup.ConfirmType.ABANDON_MID_RUN);
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.OPTION_ABANDON, this.x - (float)this.W / 2.0f, this.y - (float)this.H / 2.0f, (float)this.W / 2.0f, (float)this.H / 2.0f, this.W, this.H, Settings.scale, Settings.scale, 0.0f, 0, 0, this.W, this.H, false, false);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[0], this.x + 15.0f * Settings.scale, this.y + 5.0f * Settings.scale, Settings.GOLD_COLOR);
        if (this.hb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.2f));
            sb.draw(ImageMaster.OPTION_ABANDON, this.x - (float)this.W / 2.0f, this.y - (float)this.H / 2.0f, (float)this.W / 2.0f, (float)this.H / 2.0f, this.W, this.H, Settings.scale, Settings.scale, 0.0f, 0, 0, this.W, this.H, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.proceed.getKeyImg(), this.x - 32.0f - 32.0f * Settings.scale - FontHelper.getSmartWidth(FontHelper.buttonLabelFont, TEXT[0], 99999.0f, 0.0f) / 2.0f, this.y - 32.0f + 5.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.hb.render(sb);
    }
}

