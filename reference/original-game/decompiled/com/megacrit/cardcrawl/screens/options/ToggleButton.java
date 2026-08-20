/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.DisplayConfig;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.RestartForChangesEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ToggleButton {
    private static final Logger logger = LogManager.getLogger(ToggleButton.class.getName());
    private static final int W = 32;
    private float x;
    private float y;
    public Hitbox hb;
    public boolean enabled = true;
    private ToggleBtnType type;

    public ToggleButton(float x, float y, float middleY, ToggleBtnType type) {
        this.x = x;
        this.y = middleY + y * Settings.scale;
        this.type = type;
        this.hb = new Hitbox(200.0f * Settings.scale, 32.0f * Settings.scale);
        this.hb.move(x + 74.0f * Settings.scale, this.y);
        this.enabled = this.getPref();
    }

    public ToggleButton(float x, float y, float middleY, ToggleBtnType type, boolean isLarge) {
        this.x = x;
        this.y = middleY + y * Settings.scale;
        this.type = type;
        if (isLarge) {
            this.hb = new Hitbox(480.0f * Settings.scale, 32.0f * Settings.scale);
            this.hb.move(x + 214.0f * Settings.scale, this.y);
        } else {
            this.hb = new Hitbox(240.0f * Settings.scale, 32.0f * Settings.scale);
            this.hb.move(x + 74.0f * Settings.scale, this.y);
        }
        this.enabled = this.getPref();
    }

    private boolean getPref() {
        switch (this.type) {
            case AMBIENCE_ON: {
                return Settings.soundPref.getBoolean("Ambience On", true);
            }
            case MUTE_IF_BG: {
                return Settings.soundPref.getBoolean("Mute in Bg", true);
            }
            case BIG_TEXT: {
                return Settings.gamePref.getBoolean("Bigger Text", false);
            }
            case BLOCK_DMG: {
                return Settings.gamePref.getBoolean("Blocked Damage", false);
            }
            case EFFECTS: {
                return Settings.gamePref.getBoolean("Particle Effects", false);
            }
            case FAST_MODE: {
                return Settings.gamePref.getBoolean("Fast Mode", false);
            }
            case SHOW_CARD_HOTKEYS: {
                return Settings.gamePref.getBoolean("Show Card keys", false);
            }
            case FULL_SCREEN: {
                return Settings.IS_FULLSCREEN;
            }
            case W_FULL_SCREEN: {
                return Settings.IS_W_FULLSCREEN;
            }
            case V_SYNC: {
                return Settings.IS_V_SYNC;
            }
            case HAND_CONF: {
                return Settings.gamePref.getBoolean("Hand Confirmation", false);
            }
            case SCREEN_SHAKE: {
                return Settings.gamePref.getBoolean("Screen Shake", true);
            }
            case SUM_DMG: {
                return Settings.gamePref.getBoolean("Summed Damage", false);
            }
            case UPLOAD_DATA: {
                return Settings.gamePref.getBoolean("Upload Data", true);
            }
            case LONG_PRESS: {
                return Settings.gamePref.getBoolean("Long-press Enabled", Settings.isConsoleBuild);
            }
            case PLAYTESTER_ART: {
                return Settings.gamePref.getBoolean("Playtester Art", false);
            }
        }
        return true;
    }

    public void update() {
        this.hb.update();
        if (this.hb.hovered && (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed())) {
            InputHelper.justClickedLeft = false;
            this.toggle();
        }
    }

    public void toggle() {
        this.enabled = !this.enabled;
        switch (this.type) {
            case AMBIENCE_ON: {
                Settings.soundPref.putBoolean("Ambience On", this.enabled);
                Settings.soundPref.flush();
                Settings.AMBIANCE_ON = this.enabled;
                if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                    CardCrawlGame.mainMenuScreen.updateAmbienceVolume();
                } else {
                    AbstractDungeon.scene.updateAmbienceVolume();
                }
                logger.info("Ambience On: " + this.enabled);
                break;
            }
            case MUTE_IF_BG: {
                Settings.soundPref.putBoolean("Mute in Bg", this.enabled);
                Settings.soundPref.flush();
                CardCrawlGame.MUTE_IF_BG = this.enabled;
                logger.info("Mute while in Background: " + this.enabled);
                break;
            }
            case BIG_TEXT: {
                Settings.gamePref.putBoolean("Bigger Text", this.enabled);
                Settings.gamePref.flush();
                Settings.BIG_TEXT_MODE = this.enabled;
                CardCrawlGame.mainMenuScreen.optionPanel.displayRestartRequiredText();
                logger.info("Bigger Text: " + this.enabled);
                break;
            }
            case BLOCK_DMG: {
                Settings.gamePref.putBoolean("Blocked Damage", this.enabled);
                Settings.gamePref.flush();
                Settings.SHOW_DMG_BLOCK = this.enabled;
                logger.info("Show Blocked Damage: " + this.enabled);
                break;
            }
            case EFFECTS: {
                Settings.gamePref.putBoolean("Particle Effects", this.enabled);
                Settings.gamePref.flush();
                Settings.DISABLE_EFFECTS = this.enabled;
                logger.info("Particle FX Disabled: " + this.enabled);
                break;
            }
            case FAST_MODE: {
                Settings.gamePref.putBoolean("Fast Mode", this.enabled);
                Settings.gamePref.flush();
                Settings.FAST_MODE = this.enabled;
                logger.info("Fast Mode: " + this.enabled);
                break;
            }
            case SHOW_CARD_HOTKEYS: {
                Settings.gamePref.putBoolean("Show Card keys", this.enabled);
                Settings.gamePref.flush();
                Settings.SHOW_CARD_HOTKEYS = this.enabled;
                logger.info("Show Card Hotkeys: " + this.enabled);
                break;
            }
            case FULL_SCREEN: {
                boolean bl = Settings.IS_FULLSCREEN = !Settings.IS_FULLSCREEN;
                if (Settings.IS_FULLSCREEN) {
                    if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                        CardCrawlGame.mainMenuScreen.optionPanel.setFullscreen(false);
                    } else {
                        AbstractDungeon.settingsScreen.panel.setFullscreen(false);
                    }
                    if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                        if (CardCrawlGame.mainMenuScreen.optionPanel.wfsToggle.enabled) {
                            CardCrawlGame.mainMenuScreen.optionPanel.wfsToggle.enabled = false;
                        }
                    } else if (AbstractDungeon.settingsScreen.panel.wfsToggle.enabled) {
                        AbstractDungeon.settingsScreen.panel.wfsToggle.enabled = false;
                    }
                    this.updateResolutionDropdown(0);
                } else {
                    this.updateResolutionDropdown(2);
                }
                Settings.IS_W_FULLSCREEN = false;
                DisplayConfig.writeDisplayConfigFile(Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height, Settings.MAX_FPS, Settings.IS_FULLSCREEN, Settings.IS_W_FULLSCREEN, Settings.IS_V_SYNC);
                logger.info("Fullscreen: " + Settings.IS_FULLSCREEN);
                break;
            }
            case W_FULL_SCREEN: {
                boolean bl = Settings.IS_W_FULLSCREEN = !Settings.IS_W_FULLSCREEN;
                if (Settings.IS_W_FULLSCREEN) {
                    if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                        CardCrawlGame.mainMenuScreen.optionPanel.setFullscreen(true);
                    } else {
                        AbstractDungeon.settingsScreen.panel.setFullscreen(true);
                    }
                    if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                        if (CardCrawlGame.mainMenuScreen.optionPanel.fsToggle.enabled) {
                            CardCrawlGame.mainMenuScreen.optionPanel.fsToggle.enabled = false;
                        }
                    } else if (AbstractDungeon.settingsScreen.panel.fsToggle.enabled) {
                        AbstractDungeon.settingsScreen.panel.fsToggle.enabled = false;
                    }
                    this.updateResolutionDropdown(1);
                } else {
                    this.updateResolutionDropdown(2);
                }
                Settings.IS_FULLSCREEN = false;
                DisplayConfig.writeDisplayConfigFile(Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height, Settings.MAX_FPS, Settings.IS_FULLSCREEN, Settings.IS_W_FULLSCREEN, Settings.IS_V_SYNC);
                logger.info("Borderless Fullscreen: " + Settings.IS_W_FULLSCREEN);
                break;
            }
            case V_SYNC: {
                boolean bl = Settings.IS_V_SYNC = !Settings.IS_V_SYNC;
                if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                    CardCrawlGame.mainMenuScreen.optionPanel.vSyncToggle.enabled = Settings.IS_V_SYNC;
                    CardCrawlGame.mainMenuScreen.optionPanel.effects.clear();
                    CardCrawlGame.mainMenuScreen.optionPanel.effects.add(new RestartForChangesEffect());
                } else {
                    AbstractDungeon.settingsScreen.panel.vSyncToggle.enabled = Settings.IS_V_SYNC;
                    AbstractDungeon.settingsScreen.panel.effects.clear();
                    AbstractDungeon.settingsScreen.panel.effects.add(new RestartForChangesEffect());
                }
                DisplayConfig.writeDisplayConfigFile(Settings.SAVED_WIDTH, Settings.SAVED_HEIGHT, Settings.MAX_FPS, Settings.IS_FULLSCREEN, Settings.IS_W_FULLSCREEN, Settings.IS_V_SYNC);
                logger.info("V Sync: " + Settings.IS_V_SYNC);
                break;
            }
            case HAND_CONF: {
                Settings.gamePref.putBoolean("Hand Confirmation", this.enabled);
                Settings.gamePref.flush();
                Settings.FAST_HAND_CONF = this.enabled;
                logger.info("Hand Confirmation: " + this.enabled);
                break;
            }
            case SCREEN_SHAKE: {
                Settings.gamePref.putBoolean("Screen Shake", this.enabled);
                Settings.gamePref.flush();
                Settings.SCREEN_SHAKE = this.enabled;
                logger.info("Screen Shake: " + this.enabled);
                break;
            }
            case SUM_DMG: {
                Settings.gamePref.putBoolean("Summed Damage", this.enabled);
                Settings.gamePref.flush();
                Settings.SHOW_DMG_SUM = this.enabled;
                logger.info("Display Summed Damage: " + this.enabled);
                break;
            }
            case UPLOAD_DATA: {
                Settings.gamePref.putBoolean("Upload Data", this.enabled);
                Settings.gamePref.flush();
                Settings.UPLOAD_DATA = this.enabled;
                logger.info("Upload Data: " + this.enabled);
                break;
            }
            case PLAYTESTER_ART: {
                Settings.gamePref.putBoolean("Playtester Art", this.enabled);
                Settings.gamePref.flush();
                Settings.PLAYTESTER_ART_MODE = this.enabled;
                logger.info("Playtester Art: " + this.enabled);
                break;
            }
            case LONG_PRESS: {
                Settings.gamePref.putBoolean("Long-press Enabled", this.enabled);
                Settings.gamePref.flush();
                Settings.USE_LONG_PRESS = this.enabled;
                logger.info("Long-press: " + this.enabled);
                break;
            }
        }
    }

    private void updateResolutionDropdown(int mode) {
        CardCrawlGame.mainMenuScreen.optionPanel.resoDropdown.updateResolutionLabels(mode);
        AbstractDungeon.settingsScreen.panel.resoDropdown.updateResolutionLabels(mode);
    }

    public void render(SpriteBatch sb) {
        if (this.enabled) {
            sb.setColor(Color.LIGHT_GRAY);
        } else {
            sb.setColor(Color.WHITE);
        }
        float scale = Settings.scale;
        if (this.hb.hovered) {
            sb.setColor(Color.SKY);
            scale = Settings.scale * 1.25f;
        }
        sb.draw(ImageMaster.OPTION_TOGGLE, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, scale, scale, 0.0f, 0, 0, 32, 32, false, false);
        if (this.enabled) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.OPTION_TOGGLE_ON, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, scale, scale, 0.0f, 0, 0, 32, 32, false, false);
        }
        this.hb.render(sb);
    }

    public static enum ToggleBtnType {
        FULL_SCREEN,
        W_FULL_SCREEN,
        SCREEN_SHAKE,
        AMBIENCE_ON,
        MUTE_IF_BG,
        SUM_DMG,
        BIG_TEXT,
        BLOCK_DMG,
        HAND_CONF,
        FAST_MODE,
        UPLOAD_DATA,
        LONG_PRESS,
        V_SYNC,
        PLAYTESTER_ART,
        SHOW_CARD_HOTKEYS,
        EFFECTS;

    }
}

