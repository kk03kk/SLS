/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.DisplayConfig;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.screens.DisplayOption;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {
    private static final Logger logger = LogManager.getLogger(Settings.class.getName());
    public static boolean isDev = false;
    public static boolean isBeta = false;
    public static boolean isAlpha = false;
    public static boolean isModded = false;
    public static boolean isControllerMode = false;
    public static boolean isMobile = false;
    public static boolean testFonts = false;
    public static boolean isDebug = false;
    public static boolean isInfo = false;
    public static boolean isTestingNeow = false;
    public static boolean usesTrophies = false;
    public static boolean isConsoleBuild = false;
    public static boolean usesProfileSaves = false;
    public static boolean isTouchScreen = false;
    public static boolean isDemo = false;
    public static boolean isShowBuild = false;
    public static boolean isPublisherBuild = false;
    public static GameLanguage language;
    public static boolean lineBreakViaCharacter;
    public static boolean usesOrdinal;
    public static boolean leftAlignCards;
    public static boolean manualLineBreak;
    public static boolean removeAtoZSort;
    public static boolean manualAndAutoLineBreak;
    public static Prefs soundPref;
    public static Prefs dailyPref;
    public static Prefs gamePref;
    public static boolean isDailyRun;
    public static boolean hasDoneDailyToday;
    public static long dailyDate;
    public static long totalPlayTime;
    public static boolean isFinalActAvailable;
    public static boolean hasRubyKey;
    public static boolean hasEmeraldKey;
    public static boolean hasSapphireKey;
    public static boolean isEndless;
    public static boolean isTrial;
    public static Long specialSeed;
    public static String trialName;
    public static boolean IS_FULLSCREEN;
    public static boolean IS_W_FULLSCREEN;
    public static boolean IS_V_SYNC;
    public static int MAX_FPS;
    public static int M_W;
    public static int M_H;
    public static int SAVED_WIDTH;
    public static int SAVED_HEIGHT;
    public static int WIDTH;
    public static int HEIGHT;
    public static boolean isSixteenByTen;
    public static boolean isFourByThree;
    public static boolean isTwoSixteen;
    public static boolean isLetterbox;
    public static int HORIZ_LETTERBOX_AMT;
    public static int VERT_LETTERBOX_AMT;
    public static ArrayList<DisplayOption> displayOptions;
    public static int displayIndex;
    public static float scale;
    public static float renderScale;
    public static float xScale;
    public static float yScale;
    public static float FOUR_BY_THREE_OFFSET_Y;
    public static float LETTERBOX_OFFSET_Y;
    public static Long seed;
    public static boolean seedSet;
    public static long seedSourceTimestamp;
    public static boolean isBackgrounded;
    public static float bgVolume;
    public static final String MASTER_VOLUME_PREF = "Master Volume";
    public static final String MUSIC_VOLUME_PREF = "Music Volume";
    public static final String SOUND_VOLUME_PREF = "Sound Volume";
    public static final String AMBIENCE_ON_PREF = "Ambience On";
    public static final String MUTE_IF_BG_PREF = "Mute in Bg";
    public static final float DEFAULT_MASTER_VOLUME = 0.5f;
    public static final float DEFAULT_MUSIC_VOLUME = 0.5f;
    public static final float DEFAULT_SOUND_VOLUME = 0.5f;
    public static float MASTER_VOLUME;
    public static float MUSIC_VOLUME;
    public static float SOUND_VOLUME;
    public static boolean AMBIANCE_ON;
    public static final String SCREEN_SHAKE_PREF = "Screen Shake";
    public static final String SUM_DMG_PREF = "Summed Damage";
    public static final String BLOCKED_DMG_PREF = "Blocked Damage";
    public static final String HAND_CONF_PREF = "Hand Confirmation";
    public static final String EFFECTS_PREF = "Particle Effects";
    public static final String FAST_MODE_PREF = "Fast Mode";
    public static final String UPLOAD_PREF = "Upload Data";
    public static final String PLAYTESTER_ART = "Playtester Art";
    public static final String SHOW_CARD_HOTKEYS_PREF = "Show Card keys";
    public static final String BIG_TEXT_PREF = "Bigger Text";
    public static final String LONG_PRESS_PREF = "Long-press Enabled";
    public static final String CONTROLLER_ENABLED_PREF = "Controller Enabled";
    public static final String TOUCHSCREEN_ENABLED_PREF = "Touchscreen Enabled";
    public static final String LAST_DAILY = "LAST_DAILY";
    public static boolean SHOW_DMG_SUM;
    public static boolean SHOW_DMG_BLOCK;
    public static boolean FAST_HAND_CONF;
    public static boolean FAST_MODE;
    public static boolean CONTROLLER_ENABLED;
    public static boolean TOUCHSCREEN_ENABLED;
    public static boolean DISABLE_EFFECTS;
    public static boolean UPLOAD_DATA;
    public static boolean SCREEN_SHAKE;
    public static boolean PLAYTESTER_ART_MODE;
    public static boolean SHOW_CARD_HOTKEYS;
    public static boolean USE_LONG_PRESS;
    public static boolean BIG_TEXT_MODE;
    public static final Color CREAM_COLOR;
    public static final Color LIGHT_YELLOW_COLOR;
    public static final Color RED_TEXT_COLOR;
    public static final Color GREEN_TEXT_COLOR;
    public static final Color BLUE_TEXT_COLOR;
    public static final Color GOLD_COLOR;
    public static final Color PURPLE_COLOR;
    public static final Color TOP_PANEL_SHADOW_COLOR;
    public static final Color HALF_TRANSPARENT_WHITE_COLOR;
    public static final Color QUARTER_TRANSPARENT_WHITE_COLOR;
    public static final Color TWO_THIRDS_TRANSPARENT_BLACK_COLOR;
    public static final Color HALF_TRANSPARENT_BLACK_COLOR;
    public static final Color QUARTER_TRANSPARENT_BLACK_COLOR;
    public static final Color RED_RELIC_COLOR;
    public static final Color GREEN_RELIC_COLOR;
    public static final Color BLUE_RELIC_COLOR;
    public static final Color PURPLE_RELIC_COLOR;
    public static final float POST_ATTACK_WAIT_DUR = 0.1f;
    public static final float WAIT_BEFORE_BATTLE_TIME = 1.0f;
    public static float ACTION_DUR_XFAST;
    public static float ACTION_DUR_FASTER;
    public static float ACTION_DUR_FAST;
    public static float ACTION_DUR_MED;
    public static float ACTION_DUR_LONG;
    public static float ACTION_DUR_XLONG;
    public static float CARD_DROP_END_Y;
    public static float SCROLL_SPEED;
    public static float MAP_SCROLL_SPEED;
    public static final float SCROLL_LERP_SPEED = 12.0f;
    public static final float SCROLL_SNAP_BACK_SPEED = 10.0f;
    public static float DEFAULT_SCROLL_LIMIT;
    public static float MAP_DST_Y;
    public static final float CLICK_SPEED_THRESHOLD = 0.4f;
    public static float CLICK_DIST_THRESHOLD;
    public static float POTION_W;
    public static float POTION_Y;
    public static final Color DISCARD_COLOR;
    public static final Color DISCARD_GLOW_COLOR;
    public static final Color SHADOW_COLOR;
    public static final float CARD_SOUL_SCALE = 0.12f;
    public static final float CARD_LERP_SPEED = 6.0f;
    public static float CARD_SNAP_THRESHOLD;
    public static float UI_SNAP_THRESHOLD;
    public static final float CARD_SCALE_LERP_SPEED = 7.5f;
    public static final float CARD_SCALE_SNAP_THRESHOLD = 0.003f;
    public static final float UI_LERP_SPEED = 9.0f;
    public static final float ORB_LERP_SPEED = 6.0f;
    public static final float MOUSE_LERP_SPEED = 20.0f;
    public static final float POP_LERP_SPEED = 8.0f;
    public static final float FADE_LERP_SPEED = 12.0f;
    public static final float SLOW_COLOR_LERP_SPEED = 3.0f;
    public static final float FADE_SNAP_THRESHOLD = 0.01f;
    public static final float ROTATE_LERP_SPEED = 12.0f;
    public static final float SCALE_SNAP_THRESHOLD = 0.003f;
    public static float HOVER_BUTTON_RISE_AMOUNT;
    public static final float CARD_VIEW_SCALE = 0.75f;
    public static float CARD_VIEW_PAD_X;
    public static float CARD_VIEW_PAD_Y;
    public static float OPTION_Y;
    public static float EVENT_Y;
    public static final int MAX_ASCENSION_LEVEL = 20;
    public static final float POST_COMBAT_WAIT_TIME = 0.25f;
    public static final int MAX_HAND_SIZE = 10;
    public static final int NUM_POTIONS = 3;
    public static final int NORMAL_POTION_DROP_RATE = 40;
    public static final int ELITE_POTION_DROP_RATE = 40;
    public static final int BOSS_GOLD_AMT = 100;
    public static final int BOSS_GOLD_JITTER = 5;
    public static final int ACHIEVEMENT_COUNT = 46;
    public static final int NORMAL_RARE_DROP_RATE = 3;
    public static final int NORMAL_UNCOMMON_DROP_RATE = 40;
    public static final int ELITE_RARE_DROP_RATE = 10;
    public static final int ELITE_UNCOMMON_DROP_RATE = 50;
    public static final int UNLOCK_PER_CHAR_COUNT = 5;
    public static boolean hideTopBar;
    public static boolean hidePopupDetails;
    public static boolean hideRelics;
    public static boolean hideLowerElements;
    public static boolean hideCards;
    public static boolean hideEndTurn;
    public static boolean hideCombatElements;
    public static final String SENDTODEVS = "sendToDevs";

    public static void initialize(boolean reloaded) {
        if (!reloaded) {
            Settings.initializeDisplay();
        }
        Settings.initializeSoundPref();
        Settings.initializeGamePref(reloaded);
    }

    private static void initializeDisplay() {
        logger.info("Initializing display settings...");
        DisplayConfig displayConf = DisplayConfig.readConfig();
        M_W = Gdx.graphics.getWidth();
        M_H = Gdx.graphics.getHeight();
        WIDTH = displayConf.getWidth();
        HEIGHT = displayConf.getHeight();
        MAX_FPS = displayConf.getMaxFPS();
        SAVED_WIDTH = WIDTH;
        SAVED_HEIGHT = HEIGHT;
        IS_FULLSCREEN = displayConf.getIsFullscreen();
        IS_W_FULLSCREEN = displayConf.getWFS();
        IS_V_SYNC = displayConf.getIsVsync();
        float aspectRatio = (float)WIDTH / (float)HEIGHT;
        boolean isUltrawide = false;
        boolean bl = isLetterbox = aspectRatio > 2.34f || aspectRatio < 1.3332f;
        if (aspectRatio > 1.32f && aspectRatio < 1.34f) {
            isFourByThree = true;
        } else if (aspectRatio > 1.59f && aspectRatio < 1.61f) {
            isSixteenByTen = true;
        } else if (!(aspectRatio < 1.78f) && aspectRatio > 1.78f) {
            isUltrawide = true;
        }
        if (isLetterbox) {
            if (aspectRatio < 1.333f) {
                HEIGHT = MathUtils.round((float)WIDTH * 0.75f);
                HORIZ_LETTERBOX_AMT = (M_H - HEIGHT) / 2;
                HORIZ_LETTERBOX_AMT += 2;
                xScale = scale = (float)WIDTH / 1920.0f;
                renderScale = scale;
                yScale = (float)HEIGHT / 1080.0f;
                isFourByThree = true;
            } else if (aspectRatio > 2.34f) {
                WIDTH = MathUtils.round((float)HEIGHT * 2.3333f);
                VERT_LETTERBOX_AMT = (M_W - WIDTH) / 2;
                ++VERT_LETTERBOX_AMT;
                scale = (float)((int)((float)HEIGHT * 1.77778f)) / 1920.0f;
                renderScale = xScale = (float)WIDTH / 1920.0f;
                yScale = scale;
                Settings.setXOffset();
            }
        } else if (isFourByThree) {
            xScale = scale = (float)WIDTH / 1920.0f;
            renderScale = yScale = (float)HEIGHT / 1080.0f;
        } else if (isUltrawide) {
            scale = (float)((int)((float)HEIGHT * 1.7777778f)) / 1920.0f;
            renderScale = xScale = (float)WIDTH / 1920.0f;
            yScale = scale;
            Settings.setXOffset();
            isLetterbox = true;
        } else {
            xScale = scale = (float)WIDTH / 1920.0f;
            yScale = scale;
            renderScale = scale;
        }
        SCROLL_SPEED = 75.0f * scale;
        MAP_SCROLL_SPEED = 75.0f * scale;
        DEFAULT_SCROLL_LIMIT = 50.0f * yScale;
        MAP_DST_Y = 150.0f * scale;
        CLICK_DIST_THRESHOLD = 30.0f * scale;
        CARD_DROP_END_Y = (float)HEIGHT * 0.81f;
        POTION_W = isMobile ? 64.0f * scale : 56.0f * scale;
        POTION_Y = isMobile ? (float)HEIGHT - 42.0f * scale : (float)HEIGHT - 30.0f * scale;
        OPTION_Y = (float)HEIGHT / 2.0f - 32.0f * yScale;
        EVENT_Y = (float)HEIGHT / 2.0f - 128.0f * scale;
        CARD_VIEW_PAD_X = 40.0f * scale;
        CARD_VIEW_PAD_Y = 40.0f * scale;
        HOVER_BUTTON_RISE_AMOUNT = 8.0f * scale;
        CARD_SNAP_THRESHOLD = 1.0f * scale;
        UI_SNAP_THRESHOLD = 1.0f * scale;
        FOUR_BY_THREE_OFFSET_Y = 140.0f * yScale;
    }

    private static void setXOffset() {
        if (scale == 1.0f) {
            LETTERBOX_OFFSET_Y = 0.0f;
            return;
        }
        float offsetScale = xScale - 1.0f;
        if (offsetScale < 0.0f) {
            LETTERBOX_OFFSET_Y = 0.0f;
            return;
        }
        LETTERBOX_OFFSET_Y = (float)(WIDTH - 1920) * offsetScale;
    }

    private static void initializeSoundPref() {
        logger.info("Initializing sound settings...");
        soundPref = SaveHelper.getPrefs("STSSound");
        try {
            soundPref.getBoolean(AMBIENCE_ON_PREF);
            soundPref.getBoolean(MUTE_IF_BG_PREF);
        }
        catch (Exception e) {
            soundPref.putBoolean(AMBIENCE_ON_PREF, soundPref.getBoolean(AMBIENCE_ON_PREF, true));
            soundPref.putBoolean(MUTE_IF_BG_PREF, soundPref.getBoolean(MUTE_IF_BG_PREF, true));
            soundPref.flush();
        }
        AMBIANCE_ON = soundPref.getBoolean(AMBIENCE_ON_PREF, true);
        CardCrawlGame.MUTE_IF_BG = soundPref.getBoolean(MUTE_IF_BG_PREF, true);
    }

    private static void initializeGamePref(boolean reloaded) {
        logger.info("Initializing game settings...");
        gamePref = SaveHelper.getPrefs("STSGameplaySettings");
        dailyPref = SaveHelper.getPrefs("STSDaily");
        try {
            gamePref.getBoolean(SUM_DMG_PREF);
            gamePref.getBoolean(BLOCKED_DMG_PREF);
            gamePref.getBoolean(HAND_CONF_PREF);
            gamePref.getBoolean(UPLOAD_PREF);
            gamePref.getBoolean(EFFECTS_PREF);
            gamePref.getBoolean(FAST_MODE_PREF);
            gamePref.getBoolean(SHOW_CARD_HOTKEYS_PREF);
            gamePref.getBoolean(BIG_TEXT_PREF);
            gamePref.getBoolean(LONG_PRESS_PREF);
            gamePref.getBoolean(SCREEN_SHAKE_PREF);
            gamePref.getBoolean(PLAYTESTER_ART);
            gamePref.getBoolean(CONTROLLER_ENABLED_PREF);
            gamePref.getBoolean(TOUCHSCREEN_ENABLED_PREF);
        }
        catch (Exception e) {
            gamePref.putBoolean(SUM_DMG_PREF, gamePref.getBoolean(SUM_DMG_PREF, false));
            gamePref.putBoolean(BLOCKED_DMG_PREF, gamePref.getBoolean(BLOCKED_DMG_PREF, false));
            gamePref.putBoolean(HAND_CONF_PREF, gamePref.getBoolean(HAND_CONF_PREF, false));
            gamePref.putBoolean(UPLOAD_PREF, gamePref.getBoolean(UPLOAD_PREF, true));
            gamePref.putBoolean(EFFECTS_PREF, gamePref.getBoolean(EFFECTS_PREF, false));
            gamePref.putBoolean(FAST_MODE_PREF, gamePref.getBoolean(FAST_MODE_PREF, false));
            gamePref.putBoolean(SHOW_CARD_HOTKEYS_PREF, gamePref.getBoolean(SHOW_CARD_HOTKEYS_PREF, false));
            gamePref.putBoolean(BIG_TEXT_PREF, gamePref.getBoolean(BIG_TEXT_PREF, false));
            gamePref.putBoolean(LONG_PRESS_PREF, gamePref.getBoolean(LONG_PRESS_PREF, false));
            gamePref.putBoolean(SCREEN_SHAKE_PREF, gamePref.getBoolean(SCREEN_SHAKE_PREF, true));
            gamePref.putBoolean(PLAYTESTER_ART, gamePref.getBoolean(PLAYTESTER_ART, false));
            gamePref.putBoolean(CONTROLLER_ENABLED_PREF, gamePref.getBoolean(CONTROLLER_ENABLED_PREF, true));
            gamePref.putBoolean(TOUCHSCREEN_ENABLED_PREF, gamePref.getBoolean(TOUCHSCREEN_ENABLED_PREF, false));
            if (!reloaded) {
                Settings.setLanguage(gamePref.getString("LANGUAGE", GameLanguage.ENG.name()), true);
            }
            gamePref.flush();
        }
        SHOW_DMG_SUM = gamePref.getBoolean(SUM_DMG_PREF, false);
        SHOW_DMG_BLOCK = gamePref.getBoolean(BLOCKED_DMG_PREF, false);
        FAST_HAND_CONF = gamePref.getBoolean(HAND_CONF_PREF, false);
        UPLOAD_DATA = gamePref.getBoolean(UPLOAD_PREF, true);
        DISABLE_EFFECTS = gamePref.getBoolean(EFFECTS_PREF, false);
        FAST_MODE = gamePref.getBoolean(FAST_MODE_PREF, false);
        SHOW_CARD_HOTKEYS = gamePref.getBoolean(SHOW_CARD_HOTKEYS_PREF, false);
        BIG_TEXT_MODE = gamePref.getBoolean(BIG_TEXT_PREF, false);
        USE_LONG_PRESS = gamePref.getBoolean(LONG_PRESS_PREF, false);
        SCREEN_SHAKE = gamePref.getBoolean(SCREEN_SHAKE_PREF, true);
        PLAYTESTER_ART_MODE = gamePref.getBoolean(PLAYTESTER_ART, false);
        CONTROLLER_ENABLED = gamePref.getBoolean(CONTROLLER_ENABLED_PREF, true);
        TOUCHSCREEN_ENABLED = gamePref.getBoolean(TOUCHSCREEN_ENABLED_PREF, false);
        if (TOUCHSCREEN_ENABLED || isConsoleBuild) {
            isTouchScreen = true;
        }
        if (!reloaded) {
            Settings.setLanguage(gamePref.getString("LANGUAGE", GameLanguage.ENG.name()), true);
        }
    }

    public static void setLanguage(GameLanguage key, boolean initial) {
        language = key;
        if (initial) {
            switch (language) {
                case ZHS: 
                case ZHT: {
                    manualAndAutoLineBreak = true;
                    lineBreakViaCharacter = true;
                    usesOrdinal = false;
                    removeAtoZSort = true;
                    break;
                }
                case JPN: {
                    lineBreakViaCharacter = true;
                    usesOrdinal = false;
                    if (isConsoleBuild) {
                        manualLineBreak = true;
                        leftAlignCards = true;
                    } else {
                        manualAndAutoLineBreak = true;
                        manualLineBreak = false;
                        leftAlignCards = false;
                    }
                    removeAtoZSort = true;
                    break;
                }
                case ENG: {
                    lineBreakViaCharacter = false;
                    usesOrdinal = true;
                    break;
                }
                case DUT: 
                case EPO: 
                case PTB: 
                case FIN: 
                case FRA: 
                case DEU: 
                case GRE: 
                case IND: 
                case ITA: 
                case KOR: 
                case NOR: 
                case POL: 
                case RUS: 
                case SPA: 
                case SRP: 
                case SRB: 
                case THA: 
                case UKR: 
                case TUR: 
                case VIE: {
                    lineBreakViaCharacter = false;
                    usesOrdinal = false;
                    break;
                }
                default: {
                    logger.info("[ERROR] Unspecified language: " + key.toString());
                    lineBreakViaCharacter = false;
                    usesOrdinal = true;
                }
            }
        }
        gamePref.putString("LANGUAGE", key.name());
    }

    public static void setLanguage(String langStr, boolean initial) {
        try {
            GameLanguage langKey = GameLanguage.valueOf(langStr);
            Settings.setLanguage(langKey, initial);
        }
        catch (IllegalArgumentException ex) {
            Settings.setLanguageLegacy(langStr, initial);
        }
    }

    public static void setLanguageLegacy(String key, boolean initial) {
        switch (key) {
            case "English": {
                language = GameLanguage.ENG;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = true;
                break;
            }
            case "Brazilian Portuguese": {
                language = GameLanguage.PTB;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Chinese (Simplified)": {
                language = GameLanguage.ZHS;
                if (!initial) break;
                lineBreakViaCharacter = true;
                usesOrdinal = false;
                break;
            }
            case "Chinese (Traditional)": {
                language = GameLanguage.ZHT;
                if (!initial) break;
                lineBreakViaCharacter = true;
                usesOrdinal = false;
                break;
            }
            case "Finnish": {
                language = GameLanguage.FIN;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "French": {
                language = GameLanguage.FRA;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "German": {
                language = GameLanguage.DEU;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Greek": {
                language = GameLanguage.GRE;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Italian": {
                language = GameLanguage.ITA;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Indonesian": {
                language = GameLanguage.IND;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Japanese": {
                language = GameLanguage.JPN;
                if (!initial) break;
                lineBreakViaCharacter = true;
                usesOrdinal = false;
                break;
            }
            case "Korean": {
                language = GameLanguage.KOR;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Norwegian": {
                language = GameLanguage.NOR;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Polish": {
                language = GameLanguage.POL;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Russian": {
                language = GameLanguage.RUS;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Spanish": {
                language = GameLanguage.SPA;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Serbian-Cyrillic": {
                language = GameLanguage.SRP;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Serbian-Latin": {
                language = GameLanguage.SRB;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Thai": {
                language = GameLanguage.THA;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Turkish": {
                language = GameLanguage.TUR;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Ukrainian": {
                language = GameLanguage.UKR;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = false;
                break;
            }
            case "Vietnamese": {
                language = GameLanguage.VIE;
                if (initial) {
                    lineBreakViaCharacter = false;
                    usesOrdinal = false;
                }
            }
            default: {
                language = GameLanguage.ENG;
                if (!initial) break;
                lineBreakViaCharacter = false;
                usesOrdinal = true;
            }
        }
        gamePref.putString("LANGUAGE", key);
    }

    public static boolean isStandardRun() {
        return !isDailyRun && !isTrial && !seedSet;
    }

    public static boolean treatEverythingAsUnlocked() {
        return isDailyRun || isTrial;
    }

    public static void setFinalActAvailability() {
        isFinalActAvailable = CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.IRONCLAD.name() + "_WIN", false) && CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.THE_SILENT.name() + "_WIN", false) && CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.DEFECT.name() + "_WIN", false) && !isDailyRun && !isTrial || CustomModeScreen.finalActAvailable;
    }

    static {
        lineBreakViaCharacter = false;
        usesOrdinal = true;
        leftAlignCards = false;
        manualLineBreak = false;
        removeAtoZSort = false;
        manualAndAutoLineBreak = false;
        dailyDate = 0L;
        isSixteenByTen = false;
        isFourByThree = false;
        isTwoSixteen = false;
        isLetterbox = false;
        HORIZ_LETTERBOX_AMT = 0;
        VERT_LETTERBOX_AMT = 0;
        displayOptions = null;
        displayIndex = 0;
        seedSet = false;
        isBackgrounded = false;
        bgVolume = 0.0f;
        USE_LONG_PRESS = false;
        BIG_TEXT_MODE = false;
        CREAM_COLOR = new Color(-597249);
        LIGHT_YELLOW_COLOR = new Color(-1202177);
        RED_TEXT_COLOR = new Color(-10132481);
        GREEN_TEXT_COLOR = new Color(0x7FFF00FF);
        BLUE_TEXT_COLOR = new Color(-2016482305);
        GOLD_COLOR = new Color(-272084481);
        PURPLE_COLOR = new Color(-293409025);
        TOP_PANEL_SHADOW_COLOR = new Color(64);
        HALF_TRANSPARENT_WHITE_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);
        QUARTER_TRANSPARENT_WHITE_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.25f);
        TWO_THIRDS_TRANSPARENT_BLACK_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.66f);
        HALF_TRANSPARENT_BLACK_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
        QUARTER_TRANSPARENT_BLACK_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.25f);
        RED_RELIC_COLOR = new Color(-10132545);
        GREEN_RELIC_COLOR = new Color(2147418303);
        BLUE_RELIC_COLOR = new Color(-2016482369);
        PURPLE_RELIC_COLOR = new Color(-935526465);
        ACTION_DUR_XFAST = 0.1f;
        ACTION_DUR_FASTER = 0.2f;
        ACTION_DUR_FAST = 0.25f;
        ACTION_DUR_MED = 0.5f;
        ACTION_DUR_LONG = 1.0f;
        ACTION_DUR_XLONG = 1.5f;
        DISCARD_COLOR = Color.valueOf("8a769bff");
        DISCARD_GLOW_COLOR = Color.valueOf("553a66ff");
        SHADOW_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
        hideTopBar = false;
        hidePopupDetails = false;
        hideRelics = false;
        hideLowerElements = false;
        hideCards = false;
        hideEndTurn = false;
        hideCombatElements = false;
    }

    public static enum GameLanguage {
        ENG,
        DUT,
        EPO,
        PTB,
        ZHS,
        ZHT,
        FIN,
        FRA,
        DEU,
        GRE,
        IND,
        ITA,
        JPN,
        KOR,
        NOR,
        POL,
        RUS,
        SPA,
        SRP,
        SRB,
        THA,
        TUR,
        UKR,
        VIE,
        WWW;

    }
}

