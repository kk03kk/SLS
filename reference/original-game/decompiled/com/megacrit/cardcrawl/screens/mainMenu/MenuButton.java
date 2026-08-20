/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuPanelScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MenuButton {
    private static final Logger logger = LogManager.getLogger(MenuButton.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("MenuButton");
    public static final String[] TEXT = MenuButton.uiStrings.TEXT;
    public ClickResult result;
    private String label;
    public Hitbox hb;
    private Color tint = Color.WHITE.cpy();
    private Color highlightColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private int index;
    private boolean hidden = false;
    private float x = 0.0f;
    private float targetX = 0.0f;
    public static final float FONT_X = 120.0f * Settings.scale;
    public static final float START_Y = 120.0f * Settings.scale;
    public static final float SPACE_Y = 50.0f * Settings.scale;
    public static final float FONT_OFFSET_Y = 10.0f * Settings.scale;
    private boolean confirmation = false;
    private static Texture highlightImg = null;

    public MenuButton(ClickResult r, int index) {
        if (highlightImg == null) {
            highlightImg = ImageMaster.loadImage("images/ui/mainMenu/menu_option_highlight.png");
        }
        this.result = r;
        this.index = index;
        this.setLabel();
        if (Settings.isTouchScreen || Settings.isMobile) {
            this.hb = new Hitbox(FontHelper.getSmartWidth(FontHelper.losePowerFont, this.label, 9999.0f, 1.0f) * 1.25f + 100.0f * Settings.scale, SPACE_Y * 2.0f);
            this.hb.move(this.hb.width / 2.0f + 75.0f * Settings.scale, START_Y + (float)index * (SPACE_Y * 2.0f));
        } else {
            this.hb = new Hitbox(FontHelper.getSmartWidth(FontHelper.buttonLabelFont, this.label, 9999.0f, 1.0f) + 100.0f * Settings.scale, SPACE_Y);
            this.hb.move(this.hb.width / 2.0f + 75.0f * Settings.scale, START_Y + (float)index * SPACE_Y);
        }
    }

    private void setLabel() {
        switch (this.result) {
            case PLAY: {
                this.label = TEXT[1];
                break;
            }
            case RESUME_GAME: {
                this.label = TEXT[4];
                break;
            }
            case ABANDON_RUN: {
                this.label = TEXT[10];
                break;
            }
            case INFO: {
                this.label = TEXT[14];
                break;
            }
            case STAT: {
                this.label = TEXT[6];
                break;
            }
            case SETTINGS: {
                this.label = TEXT[12];
                break;
            }
            case QUIT: {
                this.label = TEXT[8];
                break;
            }
            case PATCH_NOTES: {
                this.label = TEXT[9];
                break;
            }
            default: {
                this.label = "ERROR";
            }
        }
    }

    public void update() {
        if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.MAIN_MENU && CardCrawlGame.mainMenuScreen.bg.slider < 0.5f) {
            this.hb.update();
        }
        this.x = MathHelper.uiLerpSnap(this.x, this.targetX);
        if (this.hb.justHovered && !this.hidden) {
            CardCrawlGame.sound.playV("UI_HOVER", 0.75f);
        }
        if (this.hb.hovered) {
            this.highlightColor.a = 0.9f;
            this.targetX = 25.0f * Settings.scale;
            if (InputHelper.justClickedLeft) {
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
                this.hb.clickStarted = true;
            }
            this.tint = Color.WHITE.cpy();
        } else if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.MAIN_MENU) {
            this.highlightColor.a = MathHelper.fadeLerpSnap(this.highlightColor.a, 0.0f);
            this.targetX = 0.0f;
            this.hidden = false;
            this.tint.g = this.tint.r = MathHelper.fadeLerpSnap(this.tint.r, 0.3f);
            this.tint.b = this.tint.r;
        }
        if (this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            CInputActionSet.select.unpress();
            this.hb.clicked = true;
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            this.buttonEffect();
            CardCrawlGame.mainMenuScreen.hideMenuButtons();
        }
    }

    public void hide() {
        this.hb.hovered = false;
        this.targetX = -1000.0f * Settings.scale + 30.0f * Settings.scale * (float)this.index;
        this.hidden = true;
    }

    public void buttonEffect() {
        switch (this.result) {
            case PLAY: {
                CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.PLAY);
                break;
            }
            case RESUME_GAME: {
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.NONE;
                CardCrawlGame.mainMenuScreen.hideMenuButtons();
                CardCrawlGame.mainMenuScreen.darken();
                this.resumeGame();
                break;
            }
            case ABANDON_RUN: {
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.ABANDON_CONFIRM;
                CardCrawlGame.mainMenuScreen.abandonPopup.show();
                break;
            }
            case INFO: {
                CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.COMPENDIUM);
                break;
            }
            case STAT: {
                CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.STATS);
                break;
            }
            case SETTINGS: {
                CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.SETTINGS);
                break;
            }
            case PATCH_NOTES: {
                CardCrawlGame.mainMenuScreen.patchNotesScreen.open();
                break;
            }
            case QUIT: {
                logger.info("Quit Game button clicked!");
                Gdx.app.exit();
                break;
            }
        }
    }

    private void resumeGame() {
        CardCrawlGame.loadingSave = true;
        CardCrawlGame.chosenCharacter = CardCrawlGame.characterManager.loadChosenCharacter().chosenClass;
        CardCrawlGame.mainMenuScreen.isFadingOut = true;
        CardCrawlGame.mainMenuScreen.fadeOutMusic();
        Settings.isDailyRun = false;
        Settings.isTrial = false;
        ModHelper.setModsFalse();
        if (CardCrawlGame.steelSeries.isEnabled.booleanValue()) {
            CardCrawlGame.steelSeries.event_character_chosen(CardCrawlGame.chosenCharacter);
        }
    }

    public void render(SpriteBatch sb) {
        float lerper = Interpolation.circleIn.apply(CardCrawlGame.mainMenuScreen.bg.slider);
        float sliderX = -1000.0f * Settings.scale * lerper;
        sliderX -= (float)this.index * 250.0f * Settings.scale * lerper;
        if (this.result == ClickResult.ABANDON_RUN) {
            this.label = this.confirmation ? TEXT[11] : TEXT[10];
        }
        sb.setBlendFunction(770, 1);
        sb.setColor(this.highlightColor);
        if (Settings.isTouchScreen || Settings.isMobile) {
            sb.draw(highlightImg, this.x + FONT_X + sliderX - 179.0f + 120.0f * Settings.scale, this.hb.cY - 56.0f, 179.0f, 52.0f, 358.0f, 104.0f, Settings.scale, Settings.scale * 1.2f, 0.0f, 0, 0, 358, 104, false, false);
        } else {
            sb.draw(highlightImg, this.x + FONT_X + sliderX - 179.0f + 120.0f * Settings.scale, this.hb.cY - 52.0f, 179.0f, 52.0f, 358.0f, 104.0f, Settings.scale, Settings.scale * 0.8f, 0.0f, 0, 0, 358, 104, false, false);
        }
        sb.setBlendFunction(770, 771);
        if (Settings.isTouchScreen || Settings.isMobile) {
            FontHelper.renderSmartText(sb, FontHelper.losePowerFont, this.label, this.x + FONT_X + sliderX, this.hb.cY + FONT_OFFSET_Y, 9999.0f, 1.0f, Settings.CREAM_COLOR, 1.25f);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.buttonLabelFont, this.label, this.x + FONT_X + sliderX, this.hb.cY + FONT_OFFSET_Y, 9999.0f, 1.0f, Settings.CREAM_COLOR);
        }
        this.hb.render(sb);
    }

    public static enum ClickResult {
        PLAY,
        RESUME_GAME,
        ABANDON_RUN,
        INFO,
        STAT,
        SETTINGS,
        PATCH_NOTES,
        QUIT;

    }
}

