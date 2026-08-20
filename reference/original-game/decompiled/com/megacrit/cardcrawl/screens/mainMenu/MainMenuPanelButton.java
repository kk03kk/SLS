/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.DoorUnlockScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuPanelScreen;

public class MainMenuPanelButton {
    public Hitbox hb = new Hitbox(400.0f * Settings.scale, 700.0f * Settings.scale);
    private PanelClickResult result;
    public PanelColor pColor;
    private Color gColor = Settings.GOLD_COLOR.cpy();
    private Color cColor = Settings.CREAM_COLOR.cpy();
    private Color wColor = Color.WHITE.cpy();
    private Color grColor = Color.GRAY.cpy();
    private Texture portraitImg = null;
    private Texture panelImg = ImageMaster.MENU_PANEL_BG_BLUE;
    private static final int W = 512;
    private static final int H = 800;
    private static final int P_W = 317;
    private static final int P_H = 206;
    private String header = null;
    private String description = null;
    private float yMod;
    private float animTimer;
    private float animTime;
    private static final float START_Y = -100.0f * Settings.scale;
    private float uiScale = 1.0f;

    public MainMenuPanelButton(PanelClickResult setResult, PanelColor setColor, float x, float y) {
        this.result = setResult;
        this.pColor = setColor;
        this.hb.move(x, y);
        this.setLabel();
        this.animTimer = this.animTime = MathUtils.random(0.2f, 0.35f);
    }

    private void setLabel() {
        this.panelImg = ImageMaster.MENU_PANEL_BG_BEIGE;
        switch (this.result) {
            case PLAY_CUSTOM: {
                this.header = MenuPanelScreen.TEXT[39];
                if (this.pColor == PanelColor.GRAY) {
                    this.description = MenuPanelScreen.TEXT[37];
                    this.panelImg = ImageMaster.MENU_PANEL_BG_GRAY;
                } else {
                    this.description = MenuPanelScreen.TEXT[40];
                    this.panelImg = ImageMaster.MENU_PANEL_BG_RED;
                }
                this.portraitImg = ImageMaster.P_LOOP;
                break;
            }
            case PLAY_DAILY: {
                this.header = MenuPanelScreen.TEXT[3];
                this.description = MenuPanelScreen.TEXT[5];
                this.portraitImg = ImageMaster.P_DAILY;
                if (this.pColor == PanelColor.GRAY) {
                    this.panelImg = ImageMaster.MENU_PANEL_BG_GRAY;
                    break;
                }
                this.panelImg = ImageMaster.MENU_PANEL_BG_BLUE;
                break;
            }
            case PLAY_NORMAL: {
                this.header = MenuPanelScreen.TEXT[0];
                this.description = MenuPanelScreen.TEXT[2];
                this.portraitImg = ImageMaster.P_STANDARD;
                break;
            }
            case INFO_CARD: {
                this.header = MenuPanelScreen.TEXT[9];
                this.description = MenuPanelScreen.TEXT[11];
                this.portraitImg = ImageMaster.P_INFO_CARD;
                break;
            }
            case INFO_RELIC: {
                this.header = MenuPanelScreen.TEXT[12];
                this.description = MenuPanelScreen.TEXT[14];
                this.portraitImg = ImageMaster.P_INFO_RELIC;
                this.panelImg = ImageMaster.MENU_PANEL_BG_BLUE;
                break;
            }
            case INFO_POTION: {
                this.header = MenuPanelScreen.TEXT[43];
                this.description = MenuPanelScreen.TEXT[44];
                this.portraitImg = ImageMaster.P_INFO_POTION;
                this.panelImg = ImageMaster.MENU_PANEL_BG_RED;
                break;
            }
            case STAT_CHAR: {
                this.header = MenuPanelScreen.TEXT[18];
                this.description = MenuPanelScreen.TEXT[20];
                this.portraitImg = ImageMaster.P_STAT_CHAR;
                break;
            }
            case STAT_HISTORY: {
                this.header = MenuPanelScreen.TEXT[24];
                this.description = MenuPanelScreen.TEXT[26];
                this.portraitImg = ImageMaster.P_STAT_HISTORY;
                this.panelImg = ImageMaster.MENU_PANEL_BG_RED;
                break;
            }
            case STAT_LEADERBOARDS: {
                this.header = MenuPanelScreen.TEXT[21];
                this.description = MenuPanelScreen.TEXT[23];
                this.portraitImg = ImageMaster.P_STAT_LEADERBOARD;
                this.panelImg = ImageMaster.MENU_PANEL_BG_BLUE;
                break;
            }
            case SETTINGS_CREDITS: {
                this.header = MenuPanelScreen.TEXT[33];
                this.description = MenuPanelScreen.TEXT[35];
                this.portraitImg = ImageMaster.P_SETTING_CREDITS;
                this.panelImg = ImageMaster.MENU_PANEL_BG_RED;
                break;
            }
            case SETTINGS_GAME: {
                this.header = MenuPanelScreen.TEXT[27];
                this.description = !Settings.isConsoleBuild ? MenuPanelScreen.TEXT[29] : MenuPanelScreen.TEXT[42];
                this.portraitImg = ImageMaster.P_SETTING_GAME;
                break;
            }
            case SETTINGS_INPUT: {
                this.header = MenuPanelScreen.TEXT[30];
                this.description = !Settings.isConsoleBuild ? MenuPanelScreen.TEXT[32] : MenuPanelScreen.TEXT[41];
                this.portraitImg = ImageMaster.P_SETTING_INPUT;
                this.panelImg = ImageMaster.MENU_PANEL_BG_BLUE;
                break;
            }
        }
    }

    public void update() {
        if (this.pColor != PanelColor.GRAY) {
            this.hb.update();
        }
        if (this.hb.justHovered) {
            CardCrawlGame.sound.playV("UI_HOVER", 0.5f);
        }
        if (this.hb.hovered) {
            this.uiScale = MathHelper.fadeLerpSnap(this.uiScale, 1.025f);
            if (InputHelper.justClickedLeft) {
                this.hb.clickStarted = true;
            }
        } else {
            this.uiScale = MathHelper.cardScaleLerpSnap(this.uiScale, 1.0f);
        }
        if (this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            this.hb.clicked = true;
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            CardCrawlGame.sound.play("DECK_OPEN");
            CardCrawlGame.mainMenuScreen.panelScreen.hide();
            this.buttonEffect();
        }
        this.animatePanelIn();
    }

    private void animatePanelIn() {
        this.animTimer -= Gdx.graphics.getDeltaTime();
        if (this.animTimer < 0.0f) {
            this.animTimer = 0.0f;
        }
        this.yMod = Interpolation.swingIn.apply(0.0f, START_Y, this.animTimer / this.animTime);
        this.cColor.a = this.wColor.a = 1.0f - this.animTimer / this.animTime;
        this.gColor.a = this.wColor.a;
        this.grColor.a = this.wColor.a;
    }

    private void buttonEffect() {
        switch (this.result) {
            case INFO_CARD: {
                CardCrawlGame.mainMenuScreen.cardLibraryScreen.open();
                break;
            }
            case INFO_RELIC: {
                CardCrawlGame.mainMenuScreen.relicScreen.open();
                break;
            }
            case INFO_POTION: {
                CardCrawlGame.mainMenuScreen.potionScreen.open();
                break;
            }
            case PLAY_CUSTOM: {
                CardCrawlGame.mainMenuScreen.customModeScreen.open();
                break;
            }
            case PLAY_DAILY: {
                CardCrawlGame.mainMenuScreen.dailyScreen.open();
                break;
            }
            case PLAY_NORMAL: {
                CardCrawlGame.mainMenuScreen.charSelectScreen.open(false);
                break;
            }
            case SETTINGS_CREDITS: {
                DoorUnlockScreen.show = false;
                CardCrawlGame.mainMenuScreen.creditsScreen.open(false);
                break;
            }
            case SETTINGS_GAME: {
                CardCrawlGame.sound.play("END_TURN");
                CardCrawlGame.mainMenuScreen.isSettingsUp = true;
                InputHelper.pressedEscape = false;
                CardCrawlGame.mainMenuScreen.statsScreen.hide();
                CardCrawlGame.mainMenuScreen.cancelButton.hide();
                CardCrawlGame.cancelButton.show(MainMenuScreen.TEXT[2]);
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.SETTINGS;
                break;
            }
            case SETTINGS_INPUT: {
                CardCrawlGame.mainMenuScreen.inputSettingsScreen.open();
                break;
            }
            case STAT_CHAR: {
                CardCrawlGame.mainMenuScreen.statsScreen.open();
                break;
            }
            case STAT_HISTORY: {
                CardCrawlGame.mainMenuScreen.runHistoryScreen.open();
                break;
            }
            case STAT_LEADERBOARDS: {
                CardCrawlGame.mainMenuScreen.leaderboardsScreen.open();
                break;
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.wColor);
        sb.draw(this.panelImg, this.hb.cX - 256.0f, this.hb.cY + this.yMod - 400.0f, 256.0f, 400.0f, 512.0f, 800.0f, this.uiScale * Settings.scale, this.uiScale * Settings.scale, 0.0f, 0, 0, 512, 800, false, false);
        if (this.hb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, (this.uiScale - 1.0f) * 16.0f));
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.MENU_PANEL_BG_BLUE, this.hb.cX - 256.0f, this.hb.cY + this.yMod - 400.0f, 256.0f, 400.0f, 512.0f, 800.0f, this.uiScale * Settings.scale, this.uiScale * Settings.scale, 0.0f, 0, 0, 512, 800, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (this.pColor == PanelColor.GRAY) {
            sb.setColor(this.grColor);
        } else {
            sb.setColor(this.wColor);
        }
        sb.draw(this.portraitImg, this.hb.cX - 158.5f, this.hb.cY + this.yMod - 103.0f + 140.0f * Settings.scale, 158.5f, 103.0f, 317.0f, 206.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 317, 206, false, false);
        if (this.pColor == PanelColor.GRAY) {
            sb.setColor(this.wColor);
            sb.draw(ImageMaster.P_LOCK, this.hb.cX - 158.5f, this.hb.cY + this.yMod - 103.0f + 140.0f * Settings.scale, 158.5f, 103.0f, 317.0f, 206.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 317, 206, false, false);
        }
        sb.draw(ImageMaster.MENU_PANEL_FRAME, this.hb.cX - 256.0f, this.hb.cY + this.yMod - 400.0f, 256.0f, 400.0f, 512.0f, 800.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 800, false, false);
        if (FontHelper.getWidth(FontHelper.damageNumberFont, this.header, 0.8f) > 310.0f * Settings.scale) {
            FontHelper.renderFontCenteredHeight(sb, FontHelper.damageNumberFont, this.header, this.hb.cX - 138.0f * Settings.scale, this.hb.cY + this.yMod + 294.0f * Settings.scale, 280.0f * Settings.scale, this.gColor, 0.7f);
        } else {
            FontHelper.renderFontCenteredHeight(sb, FontHelper.damageNumberFont, this.header, this.hb.cX - 153.0f * Settings.scale, this.hb.cY + this.yMod + 294.0f * Settings.scale, 310.0f * Settings.scale, this.gColor, 0.8f);
        }
        FontHelper.renderFontCenteredHeight(sb, FontHelper.charDescFont, this.description, this.hb.cX - 153.0f * Settings.scale, this.hb.cY + this.yMod - 130.0f * Settings.scale, 330.0f * Settings.scale, this.cColor);
        this.hb.render(sb);
    }

    public static enum PanelColor {
        RED,
        BLUE,
        BEIGE,
        GRAY;

    }

    public static enum PanelClickResult {
        PLAY_NORMAL,
        PLAY_DAILY,
        PLAY_CUSTOM,
        INFO_CARD,
        INFO_RELIC,
        INFO_POTION,
        STAT_CHAR,
        STAT_LEADERBOARDS,
        STAT_HISTORY,
        SETTINGS_GAME,
        SETTINGS_INPUT,
        SETTINGS_CREDITS;

    }
}

