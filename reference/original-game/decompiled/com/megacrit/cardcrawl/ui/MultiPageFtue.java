/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.ui.FtueTip;
import com.megacrit.cardcrawl.vfx.combat.BattleStartEffect;

public class MultiPageFtue
extends FtueTip {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Main Tutorial");
    public static final String[] MSG = MultiPageFtue.tutorialStrings.TEXT;
    public static final String[] LABEL = MultiPageFtue.tutorialStrings.LABEL;
    private static final int W = 760;
    private static final int H = 580;
    private Texture img1;
    private Texture img2;
    private Texture img3;
    private Color screen = Color.valueOf("1c262a00");
    private float x;
    private float x1;
    private float x2;
    private float x3;
    private float targetX;
    private float startX;
    private float scrollTimer = 0.0f;
    private static final float SCROLL_TIME = 0.3f;
    private int currentSlot = 0;
    private static final String msg1 = MSG[0];
    private static final String msg2 = MSG[1];
    private static final String msg3 = MSG[2];

    public MultiPageFtue() {
        this.img1 = ImageMaster.loadImage("images/ui/tip/t1.png");
        this.img2 = ImageMaster.loadImage("images/ui/tip/t2.png");
        this.img3 = ImageMaster.loadImage("images/ui/tip/t3.png");
        AbstractDungeon.player.releaseCard();
        if (AbstractDungeon.isScreenUp) {
            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.FTUE;
        AbstractDungeon.overlayMenu.showBlackScreen();
        this.x = 0.0f;
        this.x1 = 567.0f * Settings.scale;
        this.x2 = this.x1 + (float)Settings.WIDTH;
        this.x3 = this.x2 + (float)Settings.WIDTH;
        AbstractDungeon.overlayMenu.proceedButton.show();
        AbstractDungeon.overlayMenu.proceedButton.setLabel(LABEL[0]);
    }

    @Override
    public void update() {
        if (this.screen.a != 0.8f) {
            this.screen.a += Gdx.graphics.getDeltaTime();
            if (this.screen.a > 0.8f) {
                this.screen.a = 0.8f;
            }
        }
        if (AbstractDungeon.overlayMenu.proceedButton.isHovered && InputHelper.justClickedLeft || CInputActionSet.proceed.isJustPressed()) {
            CInputActionSet.proceed.unpress();
            if (this.currentSlot == -2) {
                CardCrawlGame.sound.play("DECK_CLOSE");
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.overlayMenu.proceedButton.hide();
                AbstractDungeon.effectList.clear();
                AbstractDungeon.topLevelEffects.add(new BattleStartEffect(false));
                return;
            }
            AbstractDungeon.overlayMenu.proceedButton.hideInstantly();
            AbstractDungeon.overlayMenu.proceedButton.show();
            CardCrawlGame.sound.play("DECK_CLOSE");
            --this.currentSlot;
            this.startX = this.x;
            this.targetX = this.currentSlot * Settings.WIDTH;
            this.scrollTimer = 0.3f;
            if (this.currentSlot == -2) {
                AbstractDungeon.overlayMenu.proceedButton.setLabel(LABEL[1]);
            }
        }
        if (this.scrollTimer != 0.0f) {
            this.scrollTimer -= Gdx.graphics.getDeltaTime();
            if (this.scrollTimer < 0.0f) {
                this.scrollTimer = 0.0f;
            }
        }
        this.x = Interpolation.fade.apply(this.targetX, this.startX, this.scrollTimer / 0.3f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.screen);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(Color.WHITE);
        sb.draw(this.img1, this.x + this.x1 - 380.0f, (float)Settings.HEIGHT / 2.0f - 290.0f, 380.0f, 290.0f, 760.0f, 580.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 760, 580, false, false);
        sb.draw(this.img2, this.x + this.x2 - 380.0f, (float)Settings.HEIGHT / 2.0f - 290.0f, 380.0f, 290.0f, 760.0f, 580.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 760, 580, false, false);
        sb.draw(this.img3, this.x + this.x3 - 380.0f, (float)Settings.HEIGHT / 2.0f - 290.0f, 380.0f, 290.0f, 760.0f, 580.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 760, 580, false, false);
        float offsetY = 0.0f;
        if (Settings.BIG_TEXT_MODE) {
            offsetY = 110.0f * Settings.scale;
        }
        FontHelper.renderSmartText(sb, FontHelper.panelNameFont, msg1, this.x + this.x1 + 400.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f - FontHelper.getSmartHeight(FontHelper.panelNameFont, msg1, 700.0f * Settings.scale, 40.0f * Settings.scale) / 2.0f + offsetY, 700.0f * Settings.scale, 40.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.panelNameFont, msg2, this.x + this.x2 + 400.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f - FontHelper.getSmartHeight(FontHelper.panelNameFont, msg2, 700.0f * Settings.scale, 40.0f * Settings.scale) / 2.0f + offsetY, 700.0f * Settings.scale, 40.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.panelNameFont, msg3, this.x + this.x3 + 400.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f - FontHelper.getSmartHeight(FontHelper.panelNameFont, msg3, 700.0f * Settings.scale, 40.0f * Settings.scale) / 2.0f + offsetY, 700.0f * Settings.scale, 40.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderFontCenteredWidth(sb, FontHelper.panelNameFont, LABEL[2], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 360.0f * Settings.scale, Settings.GOLD_COLOR);
        FontHelper.renderFontCenteredWidth(sb, FontHelper.tipBodyFont, LABEL[3] + Integer.toString(Math.abs(this.currentSlot - 1)) + LABEL[4], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 400.0f * Settings.scale, Settings.CREAM_COLOR);
        AbstractDungeon.overlayMenu.proceedButton.render(sb);
    }
}

