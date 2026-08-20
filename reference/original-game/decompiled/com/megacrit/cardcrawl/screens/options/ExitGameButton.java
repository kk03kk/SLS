/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.Gdx;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExitGameButton {
    private static final Logger logger = LogManager.getLogger(ExitGameButton.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ExitGameButton");
    public static final String[] TEXT = ExitGameButton.uiStrings.TEXT;
    private int W = 635;
    private int H = 488;
    private Hitbox hb;
    private float x;
    private float y;
    private String label = TEXT[0];

    public ExitGameButton() {
        this.hb = new Hitbox(280.0f * Settings.scale, 80.0f * Settings.scale);
        this.x = 1490.0f * Settings.xScale;
        this.y = Settings.OPTION_Y - 202.0f * Settings.scale;
        this.hb.move(this.x + 50.0f * Settings.xScale, this.y - 173.0f * Settings.scale);
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.clicked || CInputActionSet.discardPile.isJustPressed()) {
            this.hb.clicked = false;
            AbstractDungeon.settingsScreen.popup(ConfirmPopup.ConfirmType.EXIT);
        }
        if (this.hb.hovered && InputHelper.justClickedLeft || CInputActionSet.discardPile.isJustPressed()) {
            if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                logger.info("Exit Game button clicked!");
                Gdx.app.exit();
            }
            CardCrawlGame.sound.play("UI_CLICK_1");
            this.hb.clickStarted = true;
            return;
        }
    }

    public void updateLabel(String newLabel) {
        this.label = newLabel;
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.OPTION_EXIT, this.x - (float)this.W / 2.0f, this.y - (float)this.H / 2.0f, (float)this.W / 2.0f, (float)this.H / 2.0f, this.W, this.H, Settings.scale, Settings.scale, 0.0f, 0, 0, this.W, this.H, false, false);
        FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, this.label, this.x + 50.0f * Settings.scale, this.y - 170.0f * Settings.scale, Settings.GOLD_COLOR, 1.0f);
        if (this.hb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.2f));
            sb.draw(ImageMaster.OPTION_EXIT, this.x - (float)this.W / 2.0f, this.y - (float)this.H / 2.0f, (float)this.W / 2.0f, (float)this.H / 2.0f, this.W, this.H, Settings.scale, Settings.scale, 0.0f, 0, 0, this.W, this.H, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.discardPile.getKeyImg(), this.x - 32.0f - 32.0f * Settings.scale - FontHelper.getSmartWidth(FontHelper.losePowerFont, this.label, 99999.0f, 0.0f) / 2.0f, this.y - 32.0f - 170.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.hb.render(sb);
    }
}

