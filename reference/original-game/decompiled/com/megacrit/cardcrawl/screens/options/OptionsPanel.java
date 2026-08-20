/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.DisplayConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.HitboxListener;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.DisplayOption;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.options.AbandonRunButton;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;
import com.megacrit.cardcrawl.screens.options.ExitGameButton;
import com.megacrit.cardcrawl.screens.options.Slider;
import com.megacrit.cardcrawl.screens.options.ToggleButton;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.RestartForChangesEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class OptionsPanel
implements DropdownMenuListener,
HitboxListener {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Options Tip");
    public static final String[] MSG = OptionsPanel.tutorialStrings.TEXT;
    public static final String[] LABEL = OptionsPanel.tutorialStrings.LABEL;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("OptionsPanel");
    public static final String[] TEXT = OptionsPanel.uiStrings.TEXT;
    private static final int RAW_W = 1920;
    private static final int RAW_H = 1080;
    private static final float SCREEN_CENTER_Y = (float)Settings.HEIGHT / 2.0f - 64.0f * Settings.scale;
    private AbandonRunButton abandonBtn;
    private ExitGameButton exitBtn;
    private Hitbox inputSettingsHb;
    public DropdownMenu fpsDropdown;
    public DropdownMenu resoDropdown;
    private Slider masterSlider;
    private Slider bgmSlider;
    private Slider sfxSlider;
    private static final float TOGGLE_X_LEFT = 430.0f * Settings.xScale;
    private static final float TOGGLE_X_LEFT_2 = 660.0f * Settings.xScale;
    private static final float TOGGLE_X_RIGHT = 1030.0f * Settings.xScale;
    public ToggleButton fsToggle;
    public ToggleButton wfsToggle;
    public ToggleButton vSyncToggle;
    private ToggleButton ssToggle;
    private ToggleButton ambienceToggle;
    private ToggleButton muteBgToggle;
    private ToggleButton sumToggle;
    private ToggleButton blockToggle;
    private ToggleButton confirmToggle;
    private ToggleButton effectsToggle;
    private ToggleButton fastToggle;
    private ToggleButton cardKeyOverlayToggle;
    private ToggleButton uploadToggle;
    private ToggleButton longPressToggle;
    private ToggleButton bigTextToggle;
    private ToggleButton playtesterToggle = null;
    private DropdownMenu languageDropdown;
    private String[] languageLabels;
    public ArrayList<AbstractGameEffect> effects = new ArrayList();
    private Hitbox currentHb = null;
    public static final String[] LOCALIZED_LANGUAGE_LABELS = CardCrawlGame.languagePack.getUIString((String)"LanguageDropdown").TEXT;
    private String[] FRAMERATE_LABELS = new String[]{"24", "30", "60", "120", "240"};
    private int[] FRAMERATE_OPTIONS = new int[]{24, 30, 60, 120, 240};
    private static final float LEFT_TOGGLE_X = 670.0f * Settings.xScale;
    private static final float LEFT_TEXT_X = 410.0f * Settings.xScale;
    private static final float LEFT_TOGGLE_TEXT_X = 456.0f * Settings.xScale;
    private static final String HEADER_TEXT = TEXT[1];
    private static final String GRAPHICS_PANEL_HEADER_TEXT = TEXT[2];
    private static final String RESOLUTION_TEXTS = TEXT[3];
    private static final String FULLSCREEN_TEXTS = TEXT[4];
    private static final String SOUND_PANEL_HEADER_TEXT = TEXT[5];
    private static final String VOLUME_TEXTS = TEXT[6];
    private static final String OTHER_SOUND_TEXTS = TEXT[7];
    private static final String PREF_PANEL_HEADER_TEXT = TEXT[8];
    private static final String PREF_TEXTS = TEXT[9];
    private static final String FAST_MODE_TEXT = TEXT[10];
    private static final String MISC_PANEL_HEADER_TEXT = TEXT[12];
    private static final String LANGUAGE_TEXT = TEXT[13];
    private static final String UPLOAD_TEXT = TEXT[14];
    private static final String EXIT_TEXT = TEXT[15];
    private static final String SAVE_TEXT = TEXT[16];
    private static final String VSYNC_TEXT = TEXT[17];
    private static final String PLAYTESTER_ART_TEXT = TEXT[18];
    private static final String SHOW_CARD_QUICK_SELECT_TEXT = TEXT[19];
    private static final String DISABLE_EFFECTS_TEXT = TEXT[21];
    private static final String LONGPRESS_TEXT = TEXT[25];

    public OptionsPanel() {
        this.fsToggle = new ToggleButton(TOGGLE_X_LEFT, 98.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.FULL_SCREEN, true);
        this.wfsToggle = new ToggleButton(TOGGLE_X_LEFT, 64.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.W_FULL_SCREEN, true);
        this.ssToggle = new ToggleButton(TOGGLE_X_LEFT, 30.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SCREEN_SHAKE);
        this.vSyncToggle = new ToggleButton(TOGGLE_X_LEFT_2, 30.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.V_SYNC);
        this.resoDropdown = new DropdownMenu((DropdownMenuListener)this, this.getResolutionLabels(), FontHelper.tipBodyFont, Settings.CREAM_COLOR);
        this.resetResolutionDropdownSelection();
        this.fpsDropdown = new DropdownMenu((DropdownMenuListener)this, this.FRAMERATE_LABELS, FontHelper.tipBodyFont, Settings.CREAM_COLOR);
        this.resetFpsDropdownSelection();
        this.sumToggle = new ToggleButton(TOGGLE_X_LEFT, -122.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SUM_DMG, true);
        this.blockToggle = new ToggleButton(TOGGLE_X_LEFT, -156.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.BLOCK_DMG, true);
        this.confirmToggle = new ToggleButton(TOGGLE_X_LEFT, -190.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.HAND_CONF, true);
        this.effectsToggle = new ToggleButton(TOGGLE_X_LEFT, -224.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.EFFECTS, true);
        this.fastToggle = new ToggleButton(TOGGLE_X_LEFT, -258.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.FAST_MODE, true);
        this.cardKeyOverlayToggle = new ToggleButton(TOGGLE_X_LEFT, -292.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SHOW_CARD_HOTKEYS, true);
        this.ambienceToggle = new ToggleButton(TOGGLE_X_RIGHT, 58.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.AMBIENCE_ON, true);
        this.muteBgToggle = new ToggleButton(TOGGLE_X_RIGHT, 24.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.MUTE_IF_BG, true);
        this.masterSlider = new Slider(SCREEN_CENTER_Y + 186.0f * Settings.scale, Settings.MASTER_VOLUME, Slider.SliderType.MASTER);
        this.bgmSlider = new Slider(SCREEN_CENTER_Y + 142.0f * Settings.scale, Settings.MUSIC_VOLUME, Slider.SliderType.BGM);
        this.sfxSlider = new Slider(SCREEN_CENTER_Y + 98.0f * Settings.scale, Settings.SOUND_VOLUME, Slider.SliderType.SFX);
        if (this.canTogglePlaytesterArt()) {
            this.playtesterToggle = new ToggleButton(TOGGLE_X_RIGHT, -190.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.PLAYTESTER_ART, true);
        }
        this.uploadToggle = new ToggleButton(TOGGLE_X_RIGHT, -224.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.UPLOAD_DATA, true);
        this.longPressToggle = new ToggleButton(TOGGLE_X_RIGHT, -258.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.LONG_PRESS, true);
        this.bigTextToggle = new ToggleButton(TOGGLE_X_RIGHT, -292.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.BIG_TEXT, true);
        this.languageLabels = this.languageLabels();
        this.languageDropdown = new DropdownMenu((DropdownMenuListener)this, this.languageLabels, FontHelper.tipBodyFont, Settings.CREAM_COLOR, 9);
        this.resetLanguageDropdownSelection();
        this.exitBtn = new ExitGameButton();
        this.abandonBtn = new AbandonRunButton();
        this.inputSettingsHb = new Hitbox(360.0f * Settings.scale, 70.0f * Settings.scale);
        this.inputSettingsHb.move(918.0f * Settings.xScale, Settings.OPTION_Y + 382.0f * Settings.scale);
    }

    public void update() {
        this.updateControllerInput();
        if (CardCrawlGame.isInARun()) {
            this.abandonBtn.update();
        }
        this.exitBtn.update();
        if (Settings.isControllerMode && CInputActionSet.pageRightViewExhaust.isJustPressed()) {
            this.clicked(this.inputSettingsHb);
        }
        this.inputSettingsHb.encapsulatedUpdate(this);
        if (this.fpsDropdown.isOpen) {
            this.fpsDropdown.update();
        } else if (this.resoDropdown.isOpen) {
            this.resoDropdown.update();
        } else if (this.languageDropdown.isOpen) {
            this.languageDropdown.update();
        } else {
            this.updateEffects();
            this.updateGraphics();
            this.updateSound();
            this.updatePreferences();
            this.updateMiscellaneous();
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode || this.resoDropdown.isOpen || this.fpsDropdown.isOpen || this.languageDropdown.isOpen) {
            return;
        }
        if (AbstractDungeon.player != null && AbstractDungeon.player.viewingRelics) {
            return;
        }
        if (this.resoDropdown.getHitbox().hovered) {
            if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.masterSlider.bgHb);
                this.currentHb = this.masterSlider.bgHb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.fpsDropdown.getHitbox());
                this.currentHb = this.fpsDropdown.getHitbox();
            }
        } else if (this.fpsDropdown.getHitbox().hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.resoDropdown.getHitbox());
                this.currentHb = this.resoDropdown.getHitbox();
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.fsToggle.hb);
                this.currentHb = this.fsToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.bgmSlider.bgHb);
                this.currentHb = this.bgmSlider.bgHb;
            }
        } else if (this.fsToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.resoDropdown.getHitbox());
                this.currentHb = this.resoDropdown.getHitbox();
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.wfsToggle.hb);
                this.currentHb = this.wfsToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.sfxSlider.bgHb);
                this.currentHb = this.sfxSlider.bgHb;
            }
        } else if (this.wfsToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.fsToggle.hb);
                this.currentHb = this.fsToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.ssToggle.hb);
                this.currentHb = this.ssToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.ambienceToggle.hb);
                this.currentHb = this.ambienceToggle.hb;
            }
        } else if (this.ssToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.wfsToggle.hb);
                this.currentHb = this.wfsToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.sumToggle.hb);
                this.currentHb = this.sumToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                CInputHelper.setCursor(this.vSyncToggle.hb);
                this.currentHb = this.vSyncToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.muteBgToggle.hb);
                this.currentHb = this.muteBgToggle.hb;
            }
        } else if (this.vSyncToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.wfsToggle.hb);
                this.currentHb = this.wfsToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.sumToggle.hb);
                this.currentHb = this.sumToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.ssToggle.hb);
                this.currentHb = this.ssToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                CInputHelper.setCursor(this.muteBgToggle.hb);
                this.currentHb = this.muteBgToggle.hb;
            }
        } else if (this.sumToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.ssToggle.hb);
                this.currentHb = this.ssToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.blockToggle.hb);
                this.currentHb = this.blockToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.languageDropdown.getHitbox());
                this.currentHb = this.languageDropdown.getHitbox();
            }
        } else if (this.blockToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.sumToggle.hb);
                this.currentHb = this.sumToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.confirmToggle.hb);
                this.currentHb = this.confirmToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.languageDropdown.getHitbox());
                this.currentHb = this.languageDropdown.getHitbox();
            }
        } else if (this.confirmToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.blockToggle.hb);
                this.currentHb = this.blockToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.effectsToggle.hb);
                this.currentHb = this.effectsToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                if (this.playtesterToggle != null) {
                    CInputHelper.setCursor(this.playtesterToggle.hb);
                    this.currentHb = this.playtesterToggle.hb;
                } else {
                    CInputHelper.setCursor(this.uploadToggle.hb);
                    this.currentHb = this.uploadToggle.hb;
                }
            }
        } else if (this.effectsToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.confirmToggle.hb);
                this.currentHb = this.confirmToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.fastToggle.hb);
                this.currentHb = this.fastToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.uploadToggle.hb);
                this.currentHb = this.uploadToggle.hb;
            }
        } else if (this.fastToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.effectsToggle.hb);
                this.currentHb = this.effectsToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.cardKeyOverlayToggle.hb);
                this.currentHb = this.cardKeyOverlayToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.longPressToggle.hb);
                this.currentHb = this.longPressToggle.hb;
            }
        } else if (this.cardKeyOverlayToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.fastToggle.hb);
                this.currentHb = this.fastToggle.hb;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.bigTextToggle.hb);
                this.currentHb = this.bigTextToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.masterSlider.bgHb);
                this.currentHb = this.masterSlider.bgHb;
            }
        } else if (this.masterSlider.bgHb.hovered) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.bgmSlider.bgHb);
                this.currentHb = this.bgmSlider.bgHb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.resoDropdown.getHitbox());
                this.currentHb = this.resoDropdown.getHitbox();
            }
        } else if (this.bgmSlider.bgHb.hovered) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.sfxSlider.bgHb);
                this.currentHb = this.sfxSlider.bgHb;
            } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.masterSlider.bgHb);
                this.currentHb = this.masterSlider.bgHb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.fpsDropdown.getHitbox());
                this.currentHb = this.fpsDropdown.getHitbox();
            }
        } else if (this.sfxSlider.bgHb.hovered) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.ambienceToggle.hb);
                this.currentHb = this.ambienceToggle.hb;
            } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.bgmSlider.bgHb);
                this.currentHb = this.bgmSlider.bgHb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.fsToggle.hb);
                this.currentHb = this.fsToggle.hb;
            }
        } else if (this.ambienceToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.sfxSlider.bgHb);
                this.currentHb = this.sfxSlider.bgHb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.muteBgToggle.hb);
                this.currentHb = this.muteBgToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.wfsToggle.hb);
                this.currentHb = this.wfsToggle.hb;
            }
        } else if (this.muteBgToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.ambienceToggle.hb);
                this.currentHb = this.ambienceToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.languageDropdown.getHitbox());
                this.currentHb = this.languageDropdown.getHitbox();
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.vSyncToggle.hb);
                this.currentHb = this.vSyncToggle.hb;
            }
        } else if (this.languageDropdown.getHitbox().hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.muteBgToggle.hb);
                this.currentHb = this.muteBgToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                if (this.canTogglePlaytesterArt()) {
                    CInputHelper.setCursor(this.playtesterToggle.hb);
                    this.currentHb = this.playtesterToggle.hb;
                } else {
                    CInputHelper.setCursor(this.uploadToggle.hb);
                    this.currentHb = this.uploadToggle.hb;
                }
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.sumToggle.hb);
                this.currentHb = this.sumToggle.hb;
            }
        } else if (this.playtesterToggle != null && this.playtesterToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.languageDropdown.getHitbox());
                this.currentHb = this.languageDropdown.getHitbox();
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.uploadToggle.hb);
                this.currentHb = this.uploadToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.confirmToggle.hb);
                this.currentHb = this.confirmToggle.hb;
            }
        } else if (this.uploadToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                if (this.playtesterToggle != null) {
                    CInputHelper.setCursor(this.playtesterToggle.hb);
                    this.currentHb = this.playtesterToggle.hb;
                } else {
                    CInputHelper.setCursor(this.languageDropdown.getHitbox());
                    this.currentHb = this.languageDropdown.getHitbox();
                }
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.effectsToggle.hb);
                this.currentHb = this.effectsToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.longPressToggle.hb);
                this.currentHb = this.longPressToggle.hb;
            }
        } else if (this.longPressToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.uploadToggle.hb);
                this.currentHb = this.uploadToggle.hb;
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                CInputHelper.setCursor(this.bigTextToggle.hb);
                this.currentHb = this.bigTextToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.fastToggle.hb);
                this.currentHb = this.fastToggle.hb;
            }
        } else if (this.bigTextToggle.hb.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                CInputHelper.setCursor(this.longPressToggle.hb);
                this.currentHb = this.longPressToggle.hb;
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                CInputHelper.setCursor(this.cardKeyOverlayToggle.hb);
                this.currentHb = this.cardKeyOverlayToggle.hb;
            }
        } else {
            CInputHelper.setCursor(this.resoDropdown.getHitbox());
            this.currentHb = this.resoDropdown.getHitbox();
        }
    }

    private void updateEffects() {
        Iterator<AbstractGameEffect> c = this.effects.iterator();
        while (c.hasNext()) {
            AbstractGameEffect e = c.next();
            e.update();
            if (!e.isDone) continue;
            c.remove();
        }
    }

    private void updateGraphics() {
        this.fsToggle.update();
        this.wfsToggle.update();
        this.ssToggle.update();
        this.vSyncToggle.update();
        this.resoDropdown.update();
        this.fpsDropdown.update();
        if (this.fsToggle.hb.hovered) {
            TipHelper.renderGenericTip((float)Settings.WIDTH * 0.03f, this.fsToggle.hb.cY + 50.0f * Settings.scale, LABEL[1], MSG[1]);
        } else if (this.wfsToggle.hb.hovered) {
            TipHelper.renderGenericTip((float)Settings.WIDTH * 0.03f, this.wfsToggle.hb.cY + 50.0f * Settings.scale, LABEL[2], MSG[2]);
        } else if (this.fpsDropdown.getHitbox().hovered) {
            TipHelper.renderGenericTip((float)Settings.WIDTH * 0.03f, this.fpsDropdown.getHitbox().cY + 30.0f * Settings.scale, LABEL[3], MSG[3]);
        }
    }

    private void updateSound() {
        this.ambienceToggle.update();
        this.muteBgToggle.update();
        this.masterSlider.update();
        this.bgmSlider.update();
        this.sfxSlider.update();
    }

    private void updatePreferences() {
        this.sumToggle.update();
        this.blockToggle.update();
        this.confirmToggle.update();
        this.effectsToggle.update();
        this.fastToggle.update();
        this.cardKeyOverlayToggle.update();
    }

    private void updateMiscellaneous() {
        this.uploadToggle.update();
        if (this.playtesterToggle != null) {
            this.playtesterToggle.update();
        }
        if (this.uploadToggle.hb.hovered) {
            TipHelper.renderGenericTip((float)Settings.WIDTH * 0.03f, (float)Settings.HEIGHT / 2.0f, LABEL[0], MSG[0]);
        }
        this.longPressToggle.update();
        this.bigTextToggle.update();
        this.languageDropdown.update();
    }

    public void render(SpriteBatch sb) {
        this.renderBg(sb);
        this.renderBanner(sb);
        this.renderGraphics(sb);
        this.renderSound(sb);
        this.renderPreferences(sb);
        this.renderMiscellaneous(sb);
        if (CardCrawlGame.isInARun()) {
            this.abandonBtn.render(sb);
        }
        this.exitBtn.render(sb);
        if (!Settings.isConsoleBuild && !Settings.isMobile) {
            this.languageDropdown.render(sb, 1150.0f * Settings.xScale, SCREEN_CENTER_Y - 115.0f * Settings.scale);
        } else {
            this.languageDropdown.render(sb, 1180.0f * Settings.xScale, SCREEN_CENTER_Y - 100.0f * Settings.scale);
        }
        if (this.resoDropdown.isOpen) {
            this.fpsDropdown.render(sb, LEFT_TOGGLE_X, SCREEN_CENTER_Y + 160.0f * Settings.scale);
            this.resoDropdown.render(sb, LEFT_TOGGLE_X, SCREEN_CENTER_Y + 206.0f * Settings.scale);
        } else {
            this.resoDropdown.render(sb, LEFT_TOGGLE_X, SCREEN_CENTER_Y + 206.0f * Settings.scale);
            this.fpsDropdown.render(sb, LEFT_TOGGLE_X, SCREEN_CENTER_Y + 160.0f * Settings.scale);
        }
        for (AbstractGameEffect e : this.effects) {
            e.render(sb);
        }
        this.renderControllerUi(sb, this.currentHb);
    }

    private void renderControllerUi(SpriteBatch sb, Hitbox hb) {
        if (!Settings.isControllerMode) {
            return;
        }
        if (hb != null) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(0.7f, 0.9f, 1.0f, 0.25f));
            sb.draw(ImageMaster.CONTROLLER_HB_HIGHLIGHT, hb.cX - hb.width / 2.0f, hb.cY - hb.height / 2.0f, hb.width, hb.height);
            sb.setBlendFunction(770, 771);
        }
    }

    private void renderBg(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.INPUT_SETTINGS_EDGES, (float)Settings.WIDTH / 2.0f - 960.0f, Settings.OPTION_Y - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.xScale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
        sb.draw(ImageMaster.SETTINGS_BACKGROUND, (float)Settings.WIDTH / 2.0f - 960.0f, Settings.OPTION_Y - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.xScale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.inputSettingsHb.cX - 32.0f + FontHelper.getSmartWidth(FontHelper.panelNameFont, TEXT[20], 99999.0f, 0.0f) / 2.0f + 42.0f * Settings.scale, Settings.OPTION_Y - 32.0f + 379.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.xScale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }

    private void renderBanner(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, HEADER_TEXT, this.inputSettingsHb.cX - 396.0f * Settings.xScale, this.inputSettingsHb.cY, Settings.GOLD_COLOR);
        Color textColor = this.inputSettingsHb.hovered ? Settings.GOLD_COLOR : Color.LIGHT_GRAY;
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[20], this.inputSettingsHb.cX, this.inputSettingsHb.cY, textColor);
        this.inputSettingsHb.render(sb);
    }

    private void renderGraphics(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, GRAPHICS_PANEL_HEADER_TEXT, 636.0f * Settings.xScale, SCREEN_CENTER_Y + 256.0f * Settings.scale, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, RESOLUTION_TEXTS, LEFT_TEXT_X, SCREEN_CENTER_Y + 196.0f * Settings.scale, 10000.0f, 40.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, FULLSCREEN_TEXTS, LEFT_TOGGLE_TEXT_X, SCREEN_CENTER_Y + 106.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, VSYNC_TEXT, 686.0f * Settings.xScale, SCREEN_CENTER_Y + 106.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        this.fsToggle.render(sb);
        this.wfsToggle.render(sb);
        this.ssToggle.render(sb);
        this.vSyncToggle.render(sb);
    }

    private void renderSound(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, SOUND_PANEL_HEADER_TEXT, 1264.0f * Settings.xScale, SCREEN_CENTER_Y + 256.0f * Settings.scale, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, VOLUME_TEXTS, 1020.0f * Settings.xScale, SCREEN_CENTER_Y + 196.0f * Settings.scale, 10000.0f, 44.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, OTHER_SOUND_TEXTS, 1056.0f * Settings.xScale, SCREEN_CENTER_Y + 66.0f * Settings.scale, 10000.0f, 34.8f * Settings.scale, Settings.CREAM_COLOR);
        this.masterSlider.render(sb);
        this.bgmSlider.render(sb);
        this.sfxSlider.render(sb);
        this.ambienceToggle.render(sb);
        this.muteBgToggle.render(sb);
    }

    private void renderPreferences(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, PREF_PANEL_HEADER_TEXT, 636.0f * Settings.xScale, SCREEN_CENTER_Y - 60.0f * Settings.scale, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, PREF_TEXTS + " NL " + DISABLE_EFFECTS_TEXT + " NL " + FAST_MODE_TEXT + " NL " + SHOW_CARD_QUICK_SELECT_TEXT, 456.0f * Settings.xScale, SCREEN_CENTER_Y - 112.0f * Settings.scale, 10000.0f, 34.8f * Settings.scale, Settings.CREAM_COLOR);
        this.sumToggle.render(sb);
        this.blockToggle.render(sb);
        this.confirmToggle.render(sb);
        this.effectsToggle.render(sb);
        this.fastToggle.render(sb);
        this.cardKeyOverlayToggle.render(sb);
    }

    private void renderMiscellaneous(SpriteBatch sb) {
        FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, MISC_PANEL_HEADER_TEXT, 1264.0f * Settings.xScale, SCREEN_CENTER_Y - 60.0f * Settings.scale, Settings.GOLD_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, LANGUAGE_TEXT, 1020.0f * Settings.xScale, SCREEN_CENTER_Y - 114.0f * Settings.scale, 10000.0f, 44.0f * Settings.scale, Settings.CREAM_COLOR);
        if (this.playtesterToggle != null) {
            FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, PLAYTESTER_ART_TEXT, 1056.0f * Settings.xScale, SCREEN_CENTER_Y - 182.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        }
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, UPLOAD_TEXT, 1056.0f * Settings.xScale, SCREEN_CENTER_Y - 216.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, LONGPRESS_TEXT, 1056.0f * Settings.xScale, SCREEN_CENTER_Y - 250.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, TEXT[26], 1056.0f * Settings.xScale, SCREEN_CENTER_Y - 284.0f * Settings.scale, 10000.0f, 34.0f * Settings.scale, Settings.CREAM_COLOR);
        this.uploadToggle.render(sb);
        if (this.playtesterToggle != null) {
            this.playtesterToggle.render(sb);
        }
        this.longPressToggle.render(sb);
        this.bigTextToggle.render(sb);
    }

    private boolean canTogglePlaytesterArt() {
        return UnlockTracker.isAchievementUnlocked("THE_ENDING") || Settings.isBeta;
    }

    public void refresh() {
        this.currentHb = null;
        this.fsToggle = new ToggleButton(TOGGLE_X_LEFT, 98.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.FULL_SCREEN);
        this.wfsToggle = new ToggleButton(TOGGLE_X_LEFT, 64.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.W_FULL_SCREEN);
        this.ssToggle = new ToggleButton(TOGGLE_X_LEFT, 30.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SCREEN_SHAKE);
        this.vSyncToggle = new ToggleButton(TOGGLE_X_LEFT_2, 30.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.V_SYNC);
        this.sumToggle = new ToggleButton(TOGGLE_X_LEFT, -122.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SUM_DMG);
        this.blockToggle = new ToggleButton(TOGGLE_X_LEFT, -156.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.BLOCK_DMG);
        this.confirmToggle = new ToggleButton(TOGGLE_X_LEFT, -190.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.HAND_CONF);
        this.effectsToggle = new ToggleButton(TOGGLE_X_LEFT, -224.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.EFFECTS);
        this.fastToggle = new ToggleButton(TOGGLE_X_LEFT, -258.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.FAST_MODE);
        this.cardKeyOverlayToggle = new ToggleButton(TOGGLE_X_LEFT, -292.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.SHOW_CARD_HOTKEYS);
        this.ambienceToggle = new ToggleButton(TOGGLE_X_RIGHT, 58.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.AMBIENCE_ON);
        this.muteBgToggle = new ToggleButton(TOGGLE_X_RIGHT, 24.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.MUTE_IF_BG);
        if (this.canTogglePlaytesterArt()) {
            this.playtesterToggle = new ToggleButton(TOGGLE_X_RIGHT, -190.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.PLAYTESTER_ART);
        }
        this.uploadToggle = new ToggleButton(TOGGLE_X_RIGHT, -224.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.UPLOAD_DATA);
        this.longPressToggle = new ToggleButton(TOGGLE_X_RIGHT, -258.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.LONG_PRESS);
        this.bigTextToggle = new ToggleButton(TOGGLE_X_RIGHT, -292.0f, SCREEN_CENTER_Y, ToggleButton.ToggleBtnType.BIG_TEXT);
        this.masterSlider = new Slider(SCREEN_CENTER_Y + 186.0f * Settings.scale, Settings.MASTER_VOLUME, Slider.SliderType.MASTER);
        this.bgmSlider = new Slider(SCREEN_CENTER_Y + 142.0f * Settings.scale, Settings.MUSIC_VOLUME, Slider.SliderType.BGM);
        this.sfxSlider = new Slider(SCREEN_CENTER_Y + 98.0f * Settings.scale, Settings.SOUND_VOLUME, Slider.SliderType.SFX);
        this.resoDropdown = new DropdownMenu((DropdownMenuListener)this, this.getResolutionLabels(), FontHelper.tipBodyFont, Settings.CREAM_COLOR);
        this.resetResolutionDropdownSelection();
        this.fpsDropdown = new DropdownMenu((DropdownMenuListener)this, this.FRAMERATE_LABELS, FontHelper.tipBodyFont, Settings.CREAM_COLOR);
        this.resetFpsDropdownSelection();
        this.languageDropdown = new DropdownMenu((DropdownMenuListener)this, this.languageLabels, FontHelper.tipBodyFont, Settings.CREAM_COLOR);
        this.resetLanguageDropdownSelection();
        this.exitBtn.updateLabel(SAVE_TEXT);
        if (!Gdx.files.local(AbstractDungeon.player.getSaveFilePath()).exists()) {
            this.exitBtn.updateLabel(EXIT_TEXT);
        }
    }

    public void displayRestartRequiredText() {
        if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
            if (CardCrawlGame.mainMenuScreen != null) {
                CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
                CardCrawlGame.mainMenuScreen.optionPanel.effects.add(new RestartForChangesEffect());
            }
        } else {
            AbstractDungeon.topLevelEffects.add(new RestartForChangesEffect());
        }
    }

    @Override
    public void changedSelectionTo(DropdownMenu dropdownMenu, int index, String optionText) {
        if (dropdownMenu == this.languageDropdown) {
            this.changeLanguageToIndex(index);
            if (Settings.isControllerMode) {
                Gdx.input.setCursorPosition((int)this.languageDropdown.getHitbox().cX, Settings.HEIGHT - (int)this.languageDropdown.getHitbox().cY);
                this.currentHb = this.languageDropdown.getHitbox();
            }
        } else if (dropdownMenu == this.resoDropdown) {
            this.changeResolutionToIndex(index);
            if (Settings.isControllerMode) {
                Gdx.input.setCursorPosition((int)this.resoDropdown.getHitbox().cX, Settings.HEIGHT - (int)this.resoDropdown.getHitbox().cY);
                this.currentHb = this.resoDropdown.getHitbox();
            }
        } else if (dropdownMenu == this.fpsDropdown) {
            this.changeFrameRateToIndex(index);
            if (Settings.isControllerMode) {
                Gdx.input.setCursorPosition((int)this.fpsDropdown.getHitbox().cX, Settings.HEIGHT - (int)this.fpsDropdown.getHitbox().cY);
                this.currentHb = this.fpsDropdown.getHitbox();
            }
        }
    }

    private void resetLanguageDropdownSelection() {
        Settings.GameLanguage[] languageOptions = this.LanguageOptions();
        for (int i = 0; i < languageOptions.length; ++i) {
            if (Settings.language != languageOptions[i]) continue;
            this.languageDropdown.setSelectedIndex(i);
            return;
        }
    }

    private void changeLanguageToIndex(int index) {
        if (index >= LOCALIZED_LANGUAGE_LABELS.length) {
            return;
        }
        Settings.GameLanguage[] languageOptions = this.LanguageOptions();
        for (int i = 0; i < languageOptions.length; ++i) {
            if (Settings.language != languageOptions[i] || i != index) continue;
            return;
        }
        Settings.setLanguage(this.LanguageOptions()[index], false);
        Settings.gamePref.flush();
        this.displayRestartRequiredText();
    }

    public String[] languageLabels() {
        if (Settings.isConsoleBuild) {
            return new String[]{LOCALIZED_LANGUAGE_LABELS[3], LOCALIZED_LANGUAGE_LABELS[0], LOCALIZED_LANGUAGE_LABELS[1], LOCALIZED_LANGUAGE_LABELS[2], LOCALIZED_LANGUAGE_LABELS[26], LOCALIZED_LANGUAGE_LABELS[4], LOCALIZED_LANGUAGE_LABELS[5], LOCALIZED_LANGUAGE_LABELS[24], LOCALIZED_LANGUAGE_LABELS[6], LOCALIZED_LANGUAGE_LABELS[7], LOCALIZED_LANGUAGE_LABELS[8], LOCALIZED_LANGUAGE_LABELS[9], LOCALIZED_LANGUAGE_LABELS[10], LOCALIZED_LANGUAGE_LABELS[11], LOCALIZED_LANGUAGE_LABELS[16], LOCALIZED_LANGUAGE_LABELS[13], LOCALIZED_LANGUAGE_LABELS[18], LOCALIZED_LANGUAGE_LABELS[19]};
        }
        if (!Settings.isBeta) {
            return new String[]{LOCALIZED_LANGUAGE_LABELS[3], LOCALIZED_LANGUAGE_LABELS[0], LOCALIZED_LANGUAGE_LABELS[1], LOCALIZED_LANGUAGE_LABELS[2], LOCALIZED_LANGUAGE_LABELS[26], LOCALIZED_LANGUAGE_LABELS[25], LOCALIZED_LANGUAGE_LABELS[27], LOCALIZED_LANGUAGE_LABELS[4], LOCALIZED_LANGUAGE_LABELS[5], LOCALIZED_LANGUAGE_LABELS[24], LOCALIZED_LANGUAGE_LABELS[6], LOCALIZED_LANGUAGE_LABELS[7], LOCALIZED_LANGUAGE_LABELS[8], LOCALIZED_LANGUAGE_LABELS[9], LOCALIZED_LANGUAGE_LABELS[10], LOCALIZED_LANGUAGE_LABELS[17], LOCALIZED_LANGUAGE_LABELS[21], LOCALIZED_LANGUAGE_LABELS[11], LOCALIZED_LANGUAGE_LABELS[16], LOCALIZED_LANGUAGE_LABELS[13], LOCALIZED_LANGUAGE_LABELS[18], LOCALIZED_LANGUAGE_LABELS[19]};
        }
        return new String[]{LOCALIZED_LANGUAGE_LABELS[3], LOCALIZED_LANGUAGE_LABELS[0], LOCALIZED_LANGUAGE_LABELS[1], LOCALIZED_LANGUAGE_LABELS[2], LOCALIZED_LANGUAGE_LABELS[26], LOCALIZED_LANGUAGE_LABELS[25], LOCALIZED_LANGUAGE_LABELS[27], LOCALIZED_LANGUAGE_LABELS[4], LOCALIZED_LANGUAGE_LABELS[5], LOCALIZED_LANGUAGE_LABELS[23], LOCALIZED_LANGUAGE_LABELS[24], LOCALIZED_LANGUAGE_LABELS[6], LOCALIZED_LANGUAGE_LABELS[7], LOCALIZED_LANGUAGE_LABELS[8], LOCALIZED_LANGUAGE_LABELS[15], LOCALIZED_LANGUAGE_LABELS[9], LOCALIZED_LANGUAGE_LABELS[10], LOCALIZED_LANGUAGE_LABELS[17], LOCALIZED_LANGUAGE_LABELS[21], LOCALIZED_LANGUAGE_LABELS[11], LOCALIZED_LANGUAGE_LABELS[16], LOCALIZED_LANGUAGE_LABELS[13], LOCALIZED_LANGUAGE_LABELS[18], LOCALIZED_LANGUAGE_LABELS[19]};
    }

    public Settings.GameLanguage[] LanguageOptions() {
        if (Settings.isConsoleBuild) {
            return new Settings.GameLanguage[]{Settings.GameLanguage.ENG, Settings.GameLanguage.PTB, Settings.GameLanguage.ZHS, Settings.GameLanguage.ZHT, Settings.GameLanguage.DUT, Settings.GameLanguage.FRA, Settings.GameLanguage.DEU, Settings.GameLanguage.ITA, Settings.GameLanguage.ITA, Settings.GameLanguage.JPN, Settings.GameLanguage.KOR, Settings.GameLanguage.POL, Settings.GameLanguage.RUS, Settings.GameLanguage.SPA, Settings.GameLanguage.THA, Settings.GameLanguage.TUR, Settings.GameLanguage.UKR, Settings.GameLanguage.VIE};
        }
        if (!Settings.isBeta) {
            return new Settings.GameLanguage[]{Settings.GameLanguage.ENG, Settings.GameLanguage.PTB, Settings.GameLanguage.ZHS, Settings.GameLanguage.ZHT, Settings.GameLanguage.DUT, Settings.GameLanguage.EPO, Settings.GameLanguage.FIN, Settings.GameLanguage.FRA, Settings.GameLanguage.DEU, Settings.GameLanguage.IND, Settings.GameLanguage.ITA, Settings.GameLanguage.JPN, Settings.GameLanguage.KOR, Settings.GameLanguage.POL, Settings.GameLanguage.RUS, Settings.GameLanguage.SRP, Settings.GameLanguage.SRB, Settings.GameLanguage.SPA, Settings.GameLanguage.THA, Settings.GameLanguage.TUR, Settings.GameLanguage.UKR, Settings.GameLanguage.VIE};
        }
        return new Settings.GameLanguage[]{Settings.GameLanguage.ENG, Settings.GameLanguage.PTB, Settings.GameLanguage.ZHS, Settings.GameLanguage.ZHT, Settings.GameLanguage.DUT, Settings.GameLanguage.EPO, Settings.GameLanguage.FIN, Settings.GameLanguage.FRA, Settings.GameLanguage.DEU, Settings.GameLanguage.GRE, Settings.GameLanguage.IND, Settings.GameLanguage.ITA, Settings.GameLanguage.JPN, Settings.GameLanguage.KOR, Settings.GameLanguage.NOR, Settings.GameLanguage.POL, Settings.GameLanguage.RUS, Settings.GameLanguage.SRP, Settings.GameLanguage.SRB, Settings.GameLanguage.SPA, Settings.GameLanguage.THA, Settings.GameLanguage.TUR, Settings.GameLanguage.UKR, Settings.GameLanguage.VIE};
    }

    private void changeFrameRateToIndex(int index) {
        if (Settings.MAX_FPS != this.FRAMERATE_OPTIONS[index]) {
            this.fpsDropdown.setSelectedIndex(index);
            Settings.MAX_FPS = this.FRAMERATE_OPTIONS[index];
            DisplayConfig.writeDisplayConfigFile(Settings.SAVED_WIDTH, Settings.SAVED_HEIGHT, Settings.MAX_FPS, Settings.IS_FULLSCREEN, Settings.IS_W_FULLSCREEN, Settings.IS_V_SYNC);
            this.displayRestartRequiredText();
        }
    }

    private void resetFpsDropdownSelection() {
        boolean found = false;
        for (int i = 0; i < this.FRAMERATE_OPTIONS.length; ++i) {
            if (Settings.MAX_FPS != this.FRAMERATE_OPTIONS[i]) continue;
            found = true;
            this.changeFrameRateToIndex(i);
            this.fpsDropdown.setSelectedIndex(i);
        }
        if (!found) {
            Settings.MAX_FPS = 60;
            this.changeFrameRateToIndex(2);
            this.fpsDropdown.setSelectedIndex(2);
        }
    }

    private void changeResolutionToIndex(int index) {
        if (Settings.displayIndex == index) {
            return;
        }
        this.resoDropdown.setSelectedIndex(index);
        Settings.displayIndex = index;
        this.displayRestartRequiredText();
        if (index > Settings.displayOptions.size() - 1) {
            index = 0;
        }
        int TMP_WIDTH = Settings.displayOptions.get((int)index).width;
        int TMP_HEIGHT = Settings.displayOptions.get((int)index).height;
        DisplayConfig.writeDisplayConfigFile(TMP_WIDTH, TMP_HEIGHT, Settings.MAX_FPS, Settings.IS_FULLSCREEN, Settings.IS_W_FULLSCREEN, Settings.IS_V_SYNC);
        Settings.SAVED_WIDTH = TMP_WIDTH;
        Settings.SAVED_HEIGHT = TMP_HEIGHT;
    }

    public void resetResolutionDropdownSelection() {
        DisplayConfig dConfig = DisplayConfig.readConfig();
        for (int i = 0; i < Settings.displayOptions.size(); ++i) {
            if (dConfig.getWidth() != Settings.displayOptions.get((int)i).width || dConfig.getHeight() != Settings.displayOptions.get((int)i).height) continue;
            Settings.displayIndex = i;
            this.resoDropdown.setSelectedIndex(i);
            this.resoDropdown.topVisibleRowIndex = 0;
            return;
        }
        this.resoDropdown.setSelectedIndex(Settings.displayIndex);
    }

    private ArrayList<String> getResolutionLabels() {
        this.initalizeDisplayOptionsIfNull();
        ArrayList<String> labels = new ArrayList<String>();
        for (DisplayOption option : Settings.displayOptions) {
            labels.add(option.uiString());
        }
        return labels;
    }

    public ArrayList<String> getResolutionLabels(int mode) {
        switch (mode) {
            case 0: {
                this.setDisplayOptionsToFullscreen();
                break;
            }
            case 1: {
                this.setDisplayOptionsToBfs();
                break;
            }
            default: {
                this.setDisplayOptionsToAllResolutions();
            }
        }
        ArrayList<String> labels = new ArrayList<String>();
        for (DisplayOption option : Settings.displayOptions) {
            labels.add(option.uiString());
        }
        return labels;
    }

    private void initalizeDisplayOptionsIfNull() {
        if (Settings.displayOptions == null) {
            Settings.displayOptions = Settings.IS_FULLSCREEN ? this.getFullScreenOnlyResolutions() : (Settings.IS_W_FULLSCREEN ? this.getBfsOnlyResolutions() : this.getWindowedAndFullscreenResolutions());
        }
    }

    public void setDisplayOptionsToBfs() {
        Settings.displayOptions.clear();
        Settings.displayOptions = null;
        Settings.displayOptions = this.getBfsOnlyResolutions();
    }

    public void setDisplayOptionsToFullscreen() {
        Settings.displayOptions.clear();
        Settings.displayOptions = null;
        Settings.displayOptions = this.getFullScreenOnlyResolutions();
    }

    public void setDisplayOptionsToAllResolutions() {
        Settings.displayOptions.clear();
        Settings.displayOptions = null;
        Settings.displayOptions = this.getWindowedAndFullscreenResolutions();
    }

    private ArrayList<DisplayOption> getBfsOnlyResolutions() {
        ArrayList<DisplayOption> retVal = new ArrayList<DisplayOption>();
        Graphics.DisplayMode[] modes = Gdx.graphics.getDisplayModes(Gdx.graphics.getPrimaryMonitor());
        ArrayList<DisplayOption> modesList = new ArrayList<DisplayOption>();
        for (int i = 0; i < modes.length; ++i) {
            modesList.add(new DisplayOption(modes[i].width, modes[i].height));
        }
        Collections.sort(modesList);
        retVal.add((DisplayOption)modesList.get(modesList.size() - 1));
        return retVal;
    }

    private ArrayList<DisplayOption> getFullScreenOnlyResolutions() {
        Graphics.DisplayMode[] modes;
        ArrayList<DisplayOption> retVal = new ArrayList<DisplayOption>();
        ArrayList<DisplayOption> allowedResolutions = this.getAllowedResolutions();
        for (Graphics.DisplayMode m : modes = Gdx.graphics.getDisplayModes(Gdx.graphics.getPrimaryMonitor())) {
            DisplayOption o = new DisplayOption(m.width, m.height, true);
            if (retVal.contains(o) || !allowedResolutions.contains(o)) continue;
            retVal.add(o);
        }
        Collections.sort(retVal);
        return retVal;
    }

    private ArrayList<DisplayOption> getAllowedResolutions() {
        ArrayList<DisplayOption> retVal = new ArrayList<DisplayOption>();
        retVal.add(new DisplayOption(1680, 720));
        retVal.add(new DisplayOption(2560, 1080));
        retVal.add(new DisplayOption(3440, 1440));
        retVal.add(new DisplayOption(1024, 576));
        retVal.add(new DisplayOption(1280, 720));
        retVal.add(new DisplayOption(1366, 768));
        retVal.add(new DisplayOption(1536, 864));
        retVal.add(new DisplayOption(1600, 900));
        retVal.add(new DisplayOption(1920, 1080));
        retVal.add(new DisplayOption(2560, 1440));
        retVal.add(new DisplayOption(3840, 2160));
        retVal.add(new DisplayOption(1024, 640));
        retVal.add(new DisplayOption(1280, 800));
        retVal.add(new DisplayOption(1680, 1050));
        retVal.add(new DisplayOption(1920, 1200));
        retVal.add(new DisplayOption(2560, 1600));
        retVal.add(new DisplayOption(1024, 768));
        retVal.add(new DisplayOption(1280, 960));
        retVal.add(new DisplayOption(1400, 1050));
        retVal.add(new DisplayOption(1440, 1080));
        retVal.add(new DisplayOption(1600, 1200));
        retVal.add(new DisplayOption(2048, 1536));
        retVal.add(new DisplayOption(2224, 1668));
        retVal.add(new DisplayOption(2732, 2048));
        return retVal;
    }

    private ArrayList<DisplayOption> getWindowedAndFullscreenResolutions() {
        Graphics.DisplayMode[] modes;
        ArrayList<DisplayOption> retVal = new ArrayList<DisplayOption>();
        ArrayList<DisplayOption> availableResos = this.getAllowedResolutions();
        DisplayOption screenRes = new DisplayOption(Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height);
        if (!retVal.contains(screenRes)) {
            availableResos.add(screenRes);
        }
        for (DisplayOption o : availableResos) {
            if (o.width > Gdx.graphics.getDisplayMode().width || o.height > Gdx.graphics.getDisplayMode().height || retVal.contains(o)) continue;
            retVal.add(o);
        }
        for (Graphics.DisplayMode m : modes = Gdx.graphics.getDisplayModes(Gdx.graphics.getPrimaryMonitor())) {
            DisplayOption o = new DisplayOption(m.width, m.height);
            if (retVal.contains(o) || o.width < 1024 || o.height < 576) continue;
            retVal.add(o);
        }
        Collections.sort(retVal);
        return retVal;
    }

    public void setFullscreen(boolean borderless) {
        int TMP_WIDTH = Gdx.graphics.getDisplayMode().width;
        int TMP_HEIGHT = Gdx.graphics.getDisplayMode().height;
        DisplayConfig.writeDisplayConfigFile(TMP_WIDTH, TMP_HEIGHT, Settings.MAX_FPS, !borderless, borderless, Settings.IS_V_SYNC);
        Settings.SAVED_WIDTH = TMP_WIDTH;
        Settings.SAVED_HEIGHT = TMP_HEIGHT;
        for (int i = 0; i < Settings.displayOptions.size(); ++i) {
            if (!Settings.displayOptions.get(i).equals(new DisplayOption(TMP_WIDTH, TMP_HEIGHT))) continue;
            this.changeResolutionToIndex(i);
        }
    }

    @Override
    public void hoverStarted(Hitbox hitbox) {
        CardCrawlGame.sound.play("UI_HOVER");
    }

    @Override
    public void startClicking(Hitbox hitbox) {
        CardCrawlGame.sound.play("UI_CLICK_1");
    }

    @Override
    public void clicked(Hitbox hitbox) {
        if (hitbox == this.inputSettingsHb) {
            if (CardCrawlGame.isInARun()) {
                AbstractDungeon.inputSettingsScreen.open(false);
            } else {
                CardCrawlGame.cancelButton.hideInstantly();
                CardCrawlGame.mainMenuScreen.inputSettingsScreen.open(false);
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.INPUT_SETTINGS;
                CardCrawlGame.mainMenuScreen.isSettingsUp = false;
            }
        }
    }
}

