/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;

public class EarlyAccessPopup {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("EarlyAccessPopup");
    public static final String[] TEXT = EarlyAccessPopup.uiStrings.TEXT;
    public static boolean isUp = false;
    private boolean darken = false;
    private static Texture img = null;

    public EarlyAccessPopup() {
        if (img == null) {
            img = ImageMaster.loadImage("images/ui/eapopup.png");
        }
    }

    public void update() {
        if (!this.darken) {
            this.darken = true;
            CardCrawlGame.mainMenuScreen.darken();
        }
        if ((InputHelper.justClickedLeft || InputHelper.pressedEscape || CInputActionSet.select.isJustPressed()) && CardCrawlGame.mainMenuScreen.screenColor.a == 0.8f) {
            CardCrawlGame.mainMenuScreen.bg.activated = true;
            isUp = false;
            CardCrawlGame.mainMenuScreen.lighten();
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(img, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        if (!Settings.isBeta) {
            FontHelper.renderFontCenteredTopAligned(sb, FontHelper.damageNumberFont, TEXT[0], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 150.0f * Settings.scale, Settings.GOLD_COLOR);
            FontHelper.renderSmartText(sb, FontHelper.topPanelInfoFont, TEXT[2], 600.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 50.0f * Settings.scale, 800.0f * Settings.scale, 32.0f * Settings.scale, Settings.CREAM_COLOR);
        } else {
            FontHelper.renderFontCenteredTopAligned(sb, FontHelper.damageNumberFont, TEXT[1], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 150.0f * Settings.scale, Settings.GOLD_COLOR);
            FontHelper.renderSmartText(sb, FontHelper.topPanelInfoFont, TEXT[3], 600.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 50.0f * Settings.scale, 800.0f * Settings.scale, 32.0f * Settings.scale, Settings.CREAM_COLOR);
        }
        if (!Settings.isControllerMode) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[4], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.2f, new Color(1.0f, 0.9f, 0.4f, 0.5f + MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) / 5.0f));
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[5], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.2f, new Color(1.0f, 0.9f, 0.4f, 0.5f + MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) / 5.0f));
            sb.draw(CInputActionSet.select.getKeyImg(), (float)Settings.WIDTH / 2.0f - 32.0f - 110.0f * Settings.scale, (float)Settings.HEIGHT * 0.2f - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }
}

