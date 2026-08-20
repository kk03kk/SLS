/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUtilsCallback;
import com.esotericsoftware.spine.SkeletonRendererDebug;
import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.SoundMaster;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardSave;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.CharacterManager;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.BuildSettings;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.ExceptionHandler;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.core.SystemStats;
import com.megacrit.cardcrawl.daily.TimeHelper;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.DrawMaster;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.GameTips;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.TrialHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.DevInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.helpers.steamInput.SteamInputHelper;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.integrations.SteelSeries;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.metrics.MetricData;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.BottledFlame;
import com.megacrit.cardcrawl.relics.BottledLightning;
import com.megacrit.cardcrawl.relics.BottledTornado;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rewards.RewardSave;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.screens.DoorUnlockScreen;
import com.megacrit.cardcrawl.screens.DungeonTransitionScreen;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import com.megacrit.cardcrawl.screens.SingleRelicViewPopup;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.splash.SplashScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.trials.AbstractTrial;
import com.megacrit.cardcrawl.ui.buttons.CancelButton;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import de.robojumper.ststwitch.TwitchConfig;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CardCrawlGame
implements ApplicationListener {
    public static String VERSION_NUM = "[V2.3.4] (12-18-2022)";
    public static String TRUE_VERSION_NUM = "2022-12-18";
    private OrthographicCamera camera;
    public static FitViewport viewport;
    public static PolygonSpriteBatch psb;
    private SpriteBatch sb;
    public static GameCursor cursor;
    public static boolean isPopupOpen;
    public static int popupMX;
    public static int popupMY;
    public static ScreenShake screenShake;
    public static AbstractDungeon dungeon;
    public static MainMenuScreen mainMenuScreen;
    public static SplashScreen splashScreen;
    public static DungeonTransitionScreen dungeonTransitionScreen;
    public static CancelButton cancelButton;
    public static MusicMaster music;
    public static SoundMaster sound;
    public static GameTips tips;
    public static SingleCardViewPopup cardPopup;
    public static SingleRelicViewPopup relicPopup;
    private FPSLogger fpsLogger = new FPSLogger();
    public boolean prevDebugKeyDown = false;
    public static String nextDungeon;
    public static GameMode mode;
    public static boolean startOver;
    private static boolean queueCredits;
    public static boolean playCreditsBgm;
    public static boolean MUTE_IF_BG;
    public static AbstractPlayer.PlayerClass chosenCharacter;
    public static boolean loadingSave;
    public static SaveFile saveFile;
    public static Prefs saveSlotPref;
    public static Prefs playerPref;
    public static int saveSlot;
    public static String playerName;
    public static String alias;
    public static CharacterManager characterManager;
    public static int monstersSlain;
    public static int elites1Slain;
    public static int elites2Slain;
    public static int elites3Slain;
    public static int elitesModdedSlain;
    public static int champion;
    public static int perfect;
    public static boolean overkill;
    public static boolean combo;
    public static boolean cheater;
    public static int goldGained;
    public static int cardsPurged;
    public static int potionsBought;
    public static int mysteryMachine;
    public static float playtime;
    public static boolean stopClock;
    public static SkeletonRendererDebug debugRenderer;
    public static AbstractTrial trial;
    public static Scanner sControllerScanner;
    SteamInputHelper steamInputHelper = null;
    public static SteamUtils clientUtils;
    public static Thread sInputDetectThread;
    private static Color screenColor;
    public static float screenTimer;
    public static float screenTime;
    private static boolean fadeIn;
    public static MetricData metricData;
    public static PublisherIntegration publisherIntegration;
    public static SteelSeries steelSeries;
    public static LocalizedStrings languagePack;
    private boolean displayCursor = true;
    public static boolean displayVersion;
    public static String preferenceDir;
    private static final Logger logger;
    private SteamUtilsCallback clUtilsCallback = new SteamUtilsCallback(){

        @Override
        public void onSteamShutdown() {
            logger.error("Steam client requested to shut down!");
        }
    };

    public CardCrawlGame(String prefDir) {
        preferenceDir = prefDir;
    }

    @Override
    public void create() {
        if (Settings.isAlpha) {
            TRUE_VERSION_NUM = TRUE_VERSION_NUM + " ALPHA";
            VERSION_NUM = VERSION_NUM + " ALPHA";
        } else if (Settings.isBeta) {
            VERSION_NUM = VERSION_NUM + " BETA";
        }
        try {
            TwitchConfig.createConfig();
            BuildSettings buildSettings = new BuildSettings(Gdx.files.internal("build.properties").reader());
            logger.info("DistributorPlatform=" + buildSettings.getDistributor());
            logger.info("isModded=" + Settings.isModded);
            logger.info("isBeta=" + Settings.isBeta);
            publisherIntegration = DistributorFactory.getEnabledDistributor(buildSettings.getDistributor());
            this.saveMigration();
            saveSlotPref = SaveHelper.getPrefs("STSSaveSlots");
            saveSlot = saveSlotPref.getInteger("DEFAULT_SLOT", 0);
            playerPref = SaveHelper.getPrefs("STSPlayer");
            playerName = saveSlotPref.getString(SaveHelper.slotName("PROFILE_NAME", saveSlot), "");
            if (playerName.equals("")) {
                playerName = playerPref.getString("name", "");
            }
            if ((alias = playerPref.getString("alias", "")).equals("")) {
                alias = CardCrawlGame.generateRandomAlias();
                playerPref.putString("alias", alias);
                playerPref.flush();
            }
            Settings.initialize(false);
            this.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            if (Settings.VERT_LETTERBOX_AMT != 0 || Settings.HORIZ_LETTERBOX_AMT != 0) {
                this.camera.position.set((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, 0.0f);
                this.camera.update();
                viewport = new FitViewport(Settings.WIDTH, (float)(Settings.M_H - Settings.HEIGHT / 2), this.camera);
            } else {
                this.camera.position.set(this.camera.viewportWidth / 2.0f, this.camera.viewportHeight / 2.0f, 0.0f);
                this.camera.update();
                viewport = new FitViewport(Settings.WIDTH, (float)Settings.HEIGHT, this.camera);
                viewport.apply();
            }
            languagePack = new LocalizedStrings();
            cardPopup = new SingleCardViewPopup();
            relicPopup = new SingleRelicViewPopup();
            if (Settings.IS_FULLSCREEN) {
                this.resize(Settings.M_W, Settings.M_H);
            }
            Gdx.graphics.setCursor(Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("images/blank.png")), 0, 0));
            this.sb = new SpriteBatch();
            psb = new PolygonSpriteBatch();
            music = new MusicMaster();
            sound = new SoundMaster();
            AbstractCreature.initialize();
            AbstractCard.initialize();
            GameDictionary.initialize();
            ImageMaster.initialize();
            AbstractPower.initialize();
            FontHelper.initialize();
            AbstractCard.initializeDynamicFrameWidths();
            UnlockTracker.initialize();
            CardLibrary.initialize();
            RelicLibrary.initialize();
            InputHelper.initialize();
            TipTracker.initialize();
            ModHelper.initialize();
            ShaderHelper.initializeShaders();
            UnlockTracker.retroactiveUnlock();
            CInputHelper.loadSettings();
            clientUtils = new SteamUtils(this.clUtilsCallback);
            this.steamInputHelper = new SteamInputHelper();
            steelSeries = new SteelSeries();
            cursor = new GameCursor();
            metricData = new MetricData();
            cancelButton = new CancelButton();
            tips = new GameTips();
            characterManager = new CharacterManager();
            splashScreen = new SplashScreen();
            mode = GameMode.SPLASH;
        }
        catch (Exception e) {
            logger.info("Exception occurred in CardCrawlGame create method!");
            ExceptionHandler.handleException(e, logger);
            Gdx.app.exit();
        }
    }

    public static void reloadPrefs() {
        playerPref = SaveHelper.getPrefs("STSPlayer");
        alias = playerPref.getString("alias", "");
        if (alias.equals("")) {
            alias = CardCrawlGame.generateRandomAlias();
            playerPref.putString("alias", alias);
        }
        music.fadeOutBGM();
        mainMenuScreen.fadeOutMusic();
        InputActionSet.prefs = SaveHelper.getPrefs("STSInputSettings");
        InputActionSet.load();
        CInputActionSet.prefs = SaveHelper.getPrefs("STSInputSettings_Controller");
        CInputActionSet.load();
        if (SteamInputHelper.numControllers == 1) {
            SteamInputHelper.initActions(SteamInputHelper.controllerHandles[0]);
        }
        characterManager = new CharacterManager();
        Settings.initialize(true);
        UnlockTracker.initialize();
        CardLibrary.resetForReload();
        CardLibrary.initialize();
        RelicLibrary.resetForReload();
        RelicLibrary.initialize();
        TipTracker.initialize();
        logger.info("TEXTURE COUNT: " + Texture.getNumManagedTextures());
        CardCrawlGame.screenColor.a = 0.0f;
        screenTime = 0.01f;
        screenTimer = 0.01f;
        fadeIn = false;
        startOver = true;
    }

    public void saveMigration() {
        if (!SaveHelper.saveExists()) {
            Preferences p = Gdx.app.getPreferences("STSPlayer");
            if (p.getString("name", "").equals("") && Gdx.app.getPreferences("STSDataVagabond").getLong("PLAYTIME") == 0L) {
                logger.info("New player, no migration.");
                return;
            }
            logger.info("Migrating Save...");
            this.migrateHelper("STSPlayer");
            this.migrateHelper("STSUnlocks");
            this.migrateHelper("STSUnlockProgress");
            this.migrateHelper("STSTips");
            this.migrateHelper("STSSound");
            this.migrateHelper("STSSeenRelics");
            this.migrateHelper("STSSeenCards");
            this.migrateHelper("STSSeenBosses");
            this.migrateHelper("STSGameplaySettings");
            this.migrateHelper("STSDataVagabond");
            this.migrateHelper("STSDataTheSilent");
            this.migrateHelper("STSAchievements");
            if (MathUtils.randomBoolean(0.5f)) {
                logger.warn("Save Migration");
            }
        } else {
            logger.info("No migration");
        }
    }

    public void migrateHelper(String file) {
        Preferences p = Gdx.app.getPreferences(file);
        Prefs p2 = SaveHelper.getPrefs(file);
        Map<String, ?> map = p.get();
        for (Map.Entry<String, ?> c : map.entrySet()) {
            p2.putString(c.getKey(), p.getString(c.getKey()));
        }
        p2.flush();
    }

    @Override
    public void render() {
        try {
            TimeHelper.update();
            if (Gdx.graphics.getRawDeltaTime() > 0.1f) {
                return;
            }
            if (!SteamInputHelper.alive) {
                CInputHelper.initializeIfAble();
            }
            this.update();
            this.sb.setProjectionMatrix(this.camera.combined);
            psb.setProjectionMatrix(this.camera.combined);
            Gdx.gl.glClear(16384);
            this.sb.begin();
            this.sb.setColor(Color.WHITE);
            switch (mode) {
                case SPLASH: {
                    splashScreen.render(this.sb);
                    break;
                }
                case CHAR_SELECT: {
                    mainMenuScreen.render(this.sb);
                    break;
                }
                case GAMEPLAY: {
                    if (dungeonTransitionScreen != null) {
                        dungeonTransitionScreen.render(this.sb);
                        break;
                    }
                    if (dungeon == null) break;
                    dungeon.render(this.sb);
                    break;
                }
                case DUNGEON_TRANSITION: {
                    break;
                }
                default: {
                    logger.info("Unknown Game Mode: " + mode.name());
                }
            }
            DrawMaster.draw(this.sb);
            if (CardCrawlGame.cardPopup.isOpen) {
                cardPopup.render(this.sb);
            }
            if (CardCrawlGame.relicPopup.isOpen) {
                relicPopup.render(this.sb);
            }
            TipHelper.render(this.sb);
            if (mode != GameMode.SPLASH) {
                this.renderBlackFadeScreen(this.sb);
                if (this.displayCursor) {
                    if (isPopupOpen) {
                        InputHelper.mX = popupMX;
                        InputHelper.mY = popupMY;
                    }
                    cursor.render(this.sb);
                }
            }
            if (Settings.HORIZ_LETTERBOX_AMT != 0) {
                this.sb.setColor(Color.BLACK);
                this.sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)(-Settings.HORIZ_LETTERBOX_AMT));
                this.sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, (float)Settings.HEIGHT, (float)Settings.WIDTH, (float)Settings.HORIZ_LETTERBOX_AMT);
            } else if (Settings.VERT_LETTERBOX_AMT != 0) {
                this.sb.setColor(Color.BLACK);
                this.sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)(-Settings.VERT_LETTERBOX_AMT), (float)Settings.HEIGHT);
                this.sb.draw(ImageMaster.WHITE_SQUARE_IMG, (float)Settings.WIDTH, 0.0f, (float)Settings.VERT_LETTERBOX_AMT, (float)Settings.HEIGHT);
            }
            this.sb.end();
        }
        catch (Exception e) {
            logger.info("Exception occurred in CardCrawlGame render method!");
            ExceptionHandler.handleException(e, logger);
            Gdx.app.exit();
        }
    }

    private void renderBlackFadeScreen(SpriteBatch sb) {
        sb.setColor(screenColor);
        if (CardCrawlGame.screenColor.a < 0.55f && !CardCrawlGame.mainMenuScreen.bg.activated) {
            CardCrawlGame.mainMenuScreen.bg.activated = true;
        }
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    public void updateFade() {
        if (screenTimer != 0.0f) {
            if ((screenTimer -= Gdx.graphics.getDeltaTime()) < 0.0f) {
                screenTimer = 0.0f;
            }
            if (fadeIn) {
                CardCrawlGame.screenColor.a = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - screenTimer / screenTime);
            } else {
                CardCrawlGame.screenColor.a = Interpolation.fade.apply(0.0f, 1.0f, 1.0f - screenTimer / screenTime);
                if (startOver && screenTimer == 0.0f) {
                    if (AbstractDungeon.scene != null) {
                        AbstractDungeon.scene.fadeOutAmbiance();
                    }
                    long startTime = System.currentTimeMillis();
                    AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
                    AbstractDungeon.reset();
                    FontHelper.cardTitleFont.getData().setScale(1.0f);
                    AbstractRelic.relicPage = 0;
                    SeedPanel.textField = "";
                    ModHelper.setModsFalse();
                    SeedHelper.cachedSeed = null;
                    Settings.seed = null;
                    Settings.seedSet = false;
                    Settings.specialSeed = null;
                    Settings.isTrial = false;
                    Settings.isDailyRun = false;
                    Settings.isEndless = false;
                    Settings.isFinalActAvailable = false;
                    Settings.hasRubyKey = false;
                    Settings.hasEmeraldKey = false;
                    Settings.hasSapphireKey = false;
                    CustomModeScreen.finalActAvailable = false;
                    trial = null;
                    logger.info("Dungeon Reset: " + (System.currentTimeMillis() - startTime) + "ms");
                    startTime = System.currentTimeMillis();
                    ShopScreen.resetPurgeCost();
                    tips.initialize();
                    metricData.clearData();
                    logger.info("Shop Screen Rest, Tips Initialize, Metric Data Clear: " + (System.currentTimeMillis() - startTime) + "ms");
                    startTime = System.currentTimeMillis();
                    UnlockTracker.refresh();
                    logger.info("Unlock Tracker Refresh:  " + (System.currentTimeMillis() - startTime) + "ms");
                    startTime = System.currentTimeMillis();
                    mainMenuScreen = new MainMenuScreen();
                    CardCrawlGame.mainMenuScreen.bg.slideDownInstantly();
                    saveSlotPref.putFloat(SaveHelper.slotName("COMPLETION", saveSlot), UnlockTracker.getCompletionPercentage());
                    saveSlotPref.putLong(SaveHelper.slotName("PLAYTIME", saveSlot), UnlockTracker.getTotalPlaytime());
                    saveSlotPref.flush();
                    logger.info("New Main Menu Screen: " + (System.currentTimeMillis() - startTime) + "ms");
                    startTime = System.currentTimeMillis();
                    CardHelper.clear();
                    mode = GameMode.CHAR_SELECT;
                    nextDungeon = "Exordium";
                    dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");
                    TipTracker.refresh();
                    logger.info("[GC] BEFORE: " + String.valueOf(SystemStats.getUsedMemory()));
                    System.gc();
                    logger.info("[GC] AFTER: " + String.valueOf(SystemStats.getUsedMemory()));
                    logger.info("New Transition Screen, Tip Tracker Refresh: " + (System.currentTimeMillis() - startTime) + "ms");
                    startTime = System.currentTimeMillis();
                    CardCrawlGame.fadeIn(2.0f);
                    if (queueCredits) {
                        queueCredits = false;
                        CardCrawlGame.mainMenuScreen.creditsScreen.open(playCreditsBgm);
                        mainMenuScreen.hideMenuButtons();
                    }
                }
            }
        }
    }

    public static void fadeIn(float duration) {
        CardCrawlGame.screenColor.a = 1.0f;
        screenTime = duration;
        screenTimer = duration;
        fadeIn = true;
    }

    public static void fadeToBlack(float duration) {
        CardCrawlGame.screenColor.a = 0.0f;
        screenTime = duration;
        screenTimer = duration;
        fadeIn = false;
    }

    public static void startOver() {
        startOver = true;
        CardCrawlGame.fadeToBlack(2.0f);
    }

    public static void startOverButShowCredits() {
        startOver = true;
        queueCredits = true;
        CardCrawlGame.doorUnlockScreenCheck();
        CardCrawlGame.fadeToBlack(2.0f);
    }

    private static void doorUnlockScreenCheck() {
        DoorUnlockScreen.show = false;
        if (!Settings.isStandardRun()) {
            logger.info("[INFO] Non-Standard Run, no check for door.");
            return;
        }
        switch (AbstractDungeon.player.chosenClass) {
            case IRONCLAD: {
                if (!playerPref.getBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", false)) {
                    logger.info("[INFO] Ironclad Victory: Show Door.");
                    playerPref.putBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", true);
                    DoorUnlockScreen.show = true;
                    break;
                }
                logger.info("[INFO] Ironclad Already Won: No Door.");
                break;
            }
            case THE_SILENT: {
                if (!playerPref.getBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", false)) {
                    logger.info("[INFO] Silent Victory: Show Door.");
                    playerPref.putBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", true);
                    DoorUnlockScreen.show = true;
                    break;
                }
                logger.info("[INFO] Silent Already Won: No Door.");
                break;
            }
            case DEFECT: {
                if (!playerPref.getBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", false)) {
                    logger.info("[INFO] Defect Victory: Show Door.");
                    playerPref.putBoolean(AbstractDungeon.player.chosenClass.name() + "_WIN", true);
                    DoorUnlockScreen.show = true;
                    break;
                }
                logger.info("[INFO] Defect Already Won: No Door.");
                break;
            }
        }
        if (DoorUnlockScreen.show) {
            playerPref.flush();
        }
    }

    public static void resetScoreVars() {
        monstersSlain = 0;
        elites1Slain = 0;
        elites2Slain = 0;
        elites3Slain = 0;
        if (dungeon != null) {
            AbstractDungeon.bossCount = 0;
        }
        champion = 0;
        perfect = 0;
        overkill = false;
        combo = false;
        goldGained = 0;
        cardsPurged = 0;
        potionsBought = 0;
        mysteryMachine = 0;
        playtime = 0.0f;
        stopClock = false;
    }

    public void update() {
        cursor.update();
        screenShake.update(viewport);
        if (mode != GameMode.SPLASH) {
            this.updateFade();
        }
        music.update();
        sound.update();
        if (CardCrawlGame.steelSeries.isEnabled.booleanValue()) {
            steelSeries.update();
        }
        if (Settings.isDebug) {
            if (DevInputActionSet.toggleCursor.isJustPressed()) {
                this.displayCursor = !this.displayCursor;
            } else if (DevInputActionSet.toggleVersion.isJustPressed()) {
                boolean bl = displayVersion = !displayVersion;
            }
        }
        if (SteamInputHelper.numControllers == 1) {
            SteamInputHelper.updateFirst();
        } else if (SteamInputHelper.numControllers == 999 && CInputHelper.controllers == null) {
            CInputHelper.initializeIfAble();
        }
        InputHelper.updateFirst();
        if (CardCrawlGame.cardPopup.isOpen) {
            cardPopup.update();
        }
        if (CardCrawlGame.relicPopup.isOpen) {
            relicPopup.update();
        }
        if (isPopupOpen) {
            popupMX = InputHelper.mX;
            popupMY = InputHelper.mY;
            InputHelper.mX = -9999;
            InputHelper.mY = -9999;
        }
        switch (mode) {
            case SPLASH: {
                splashScreen.update();
                if (!CardCrawlGame.splashScreen.isDone) break;
                mode = GameMode.CHAR_SELECT;
                splashScreen = null;
                mainMenuScreen = new MainMenuScreen();
                break;
            }
            case CHAR_SELECT: {
                mainMenuScreen.update();
                if (!CardCrawlGame.mainMenuScreen.fadedOut) break;
                AbstractDungeon.pathX = new ArrayList();
                AbstractDungeon.pathY = new ArrayList();
                if (trial == null && Settings.specialSeed != null) {
                    trial = TrialHelper.getTrialForSeed(SeedHelper.getString(Settings.specialSeed));
                }
                if (loadingSave) {
                    ModHelper.setModsFalse();
                    AbstractDungeon.player = CardCrawlGame.createCharacter(chosenCharacter);
                    this.loadPlayerSave(AbstractDungeon.player);
                } else {
                    Settings.setFinalActAvailability();
                    logger.info("FINAL ACT AVAILABLE: " + Settings.isFinalActAvailable);
                    if (trial == null) {
                        if (Settings.isDailyRun) {
                            AbstractDungeon.ascensionLevel = 0;
                            AbstractDungeon.isAscensionMode = false;
                        }
                        AbstractDungeon.player = CardCrawlGame.createCharacter(chosenCharacter);
                        for (AbstractRelic r : AbstractDungeon.player.relics) {
                            r.updateDescription(AbstractDungeon.player.chosenClass);
                            r.onEquip();
                        }
                        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                            if (c.rarity == AbstractCard.CardRarity.BASIC) continue;
                            CardHelper.obtain(c.cardID, c.rarity, c.color);
                        }
                    } else {
                        Settings.isTrial = true;
                        Settings.isDailyRun = false;
                        this.setupTrialMods(trial, chosenCharacter);
                        this.setupTrialPlayer(trial);
                    }
                }
                mode = GameMode.GAMEPLAY;
                nextDungeon = "Exordium";
                dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");
                if (loadingSave) {
                    CardCrawlGame.dungeonTransitionScreen.isComplete = true;
                    break;
                }
                monstersSlain = 0;
                elites1Slain = 0;
                elites2Slain = 0;
                elites3Slain = 0;
                break;
            }
            case GAMEPLAY: {
                if (dungeonTransitionScreen != null) {
                    dungeonTransitionScreen.update();
                    if (CardCrawlGame.dungeonTransitionScreen.isComplete) {
                        dungeonTransitionScreen = null;
                        if (loadingSave) {
                            this.getDungeon(CardCrawlGame.saveFile.level_name, AbstractDungeon.player, saveFile);
                            this.loadPostCombat(saveFile);
                            if (!CardCrawlGame.saveFile.post_combat) {
                                loadingSave = false;
                            }
                        } else {
                            this.getDungeon(nextDungeon, AbstractDungeon.player);
                            if (!nextDungeon.equals("Exordium") || Settings.isShowBuild || !TipTracker.tips.get("NEOW_SKIP").booleanValue()) {
                                AbstractDungeon.dungeonMapScreen.open(true);
                                TipTracker.neverShowAgain("NEOW_SKIP");
                            }
                        }
                    }
                } else if (dungeon != null) {
                    dungeon.update();
                } else {
                    logger.info("Eh-?");
                }
                if (dungeon == null || !AbstractDungeon.isDungeonBeaten || AbstractDungeon.fadeColor.a != 1.0f) break;
                dungeon = null;
                AbstractDungeon.scene.fadeOutAmbiance();
                dungeonTransitionScreen = new DungeonTransitionScreen(nextDungeon);
                break;
            }
            case DUNGEON_TRANSITION: {
                break;
            }
            default: {
                logger.info("Unknown Game Mode: " + mode.name());
            }
        }
        this.updateDebugSwitch();
        InputHelper.updateLast();
        if (CInputHelper.controller != null) {
            CInputHelper.updateLast();
        }
        if (Settings.isInfo) {
            this.fpsLogger.log();
        }
    }

    private void setupTrialMods(AbstractTrial trial, AbstractPlayer.PlayerClass chosenClass) {
        if (trial.useRandomDailyMods()) {
            long sourceTime = System.nanoTime();
            Random rng = new Random(sourceTime);
            Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
            ModHelper.setTodaysMods(Settings.seed, chosenClass);
        } else if (trial.dailyModIDs() != null) {
            ModHelper.setMods(trial.dailyModIDs());
            ModHelper.clearNulls();
        }
    }

    private void setupTrialPlayer(AbstractTrial trial) {
        AbstractDungeon.player = trial.setupPlayer(CardCrawlGame.createCharacter(chosenCharacter));
        if (!trial.keepStarterRelic()) {
            AbstractDungeon.player.relics.clear();
        }
        for (String relicID : trial.extraStartingRelicIDs()) {
            AbstractRelic relic = RelicLibrary.getRelic(relicID);
            relic.instantObtain(AbstractDungeon.player, AbstractDungeon.player.relics.size(), false);
            AbstractDungeon.relicsToRemoveOnStart.add(relic.relicId);
        }
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.updateDescription(AbstractDungeon.player.chosenClass);
            r.onEquip();
        }
        if (!trial.keepsStarterCards()) {
            AbstractDungeon.player.masterDeck.clear();
        }
        for (String cardID : trial.extraStartingCardIDs()) {
            AbstractDungeon.player.masterDeck.addToTop(CardLibrary.getCard(cardID).makeCopy());
        }
    }

    private void loadPostCombat(SaveFile saveFile) {
        if (saveFile.post_combat) {
            AbstractDungeon.getCurrRoom().isBattleOver = true;
            AbstractDungeon.overlayMenu.hideCombatPanels();
            AbstractDungeon.loading_post_combat = true;
            AbstractDungeon.getCurrRoom().smoked = saveFile.smoked;
            AbstractDungeon.getCurrRoom().mugged = saveFile.mugged;
            if (AbstractDungeon.getCurrRoom().event != null) {
                AbstractDungeon.getCurrRoom().event.postCombatLoad();
            }
            if (AbstractDungeon.getCurrRoom().monsters != null) {
                AbstractDungeon.getCurrRoom().monsters.monsters.clear();
                AbstractDungeon.actionManager.actions.clear();
            }
            if (!saveFile.smoked) {
                block18: for (RewardSave i : saveFile.combat_rewards) {
                    switch (i.type) {
                        case "CARD": {
                            continue block18;
                        }
                        case "GOLD": {
                            AbstractDungeon.getCurrRoom().addGoldToRewards(i.amount);
                            continue block18;
                        }
                        case "RELIC": {
                            AbstractDungeon.getCurrRoom().addRelicToRewards(RelicLibrary.getRelic(i.id).makeCopy());
                            continue block18;
                        }
                        case "POTION": {
                            AbstractDungeon.getCurrRoom().addPotionToRewards(PotionHelper.getPotion(i.id));
                            continue block18;
                        }
                        case "STOLEN_GOLD": {
                            AbstractDungeon.getCurrRoom().addStolenGoldToRewards(i.amount);
                            continue block18;
                        }
                        case "SAPPHIRE_KEY": {
                            AbstractDungeon.getCurrRoom().addSapphireKey(AbstractDungeon.getCurrRoom().rewards.get(AbstractDungeon.getCurrRoom().rewards.size() - 1));
                            continue block18;
                        }
                        case "EMERALD_KEY": {
                            AbstractDungeon.getCurrRoom().rewards.add(new RewardItem(AbstractDungeon.getCurrRoom().rewards.get(AbstractDungeon.getCurrRoom().rewards.size() - 1), RewardItem.RewardType.EMERALD_KEY));
                            continue block18;
                        }
                    }
                    logger.info("Loading unknown type: " + i.type);
                }
            }
            if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
                AbstractDungeon.scene.fadeInAmbiance();
                music.silenceTempBgmInstantly();
                music.silenceBGMInstantly();
                AbstractMonster.playBossStinger();
            } else if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite) {
                AbstractDungeon.scene.fadeInAmbiance();
                music.fadeOutTempBGM();
            }
            saveFile.post_combat = false;
        }
    }

    /*
     * WARNING - void declaration
     */
    private void loadPlayerSave(AbstractPlayer p) {
        saveFile = SaveAndContinue.loadSaveFile(p.chosenClass);
        AbstractDungeon.loading_post_combat = false;
        Settings.seed = CardCrawlGame.saveFile.seed;
        Settings.isFinalActAvailable = CardCrawlGame.saveFile.is_final_act_on;
        Settings.hasRubyKey = CardCrawlGame.saveFile.has_ruby_key;
        Settings.hasEmeraldKey = CardCrawlGame.saveFile.has_emerald_key;
        Settings.hasSapphireKey = CardCrawlGame.saveFile.has_sapphire_key;
        Settings.isDailyRun = CardCrawlGame.saveFile.is_daily;
        if (Settings.isDailyRun) {
            Settings.dailyDate = CardCrawlGame.saveFile.daily_date;
        }
        Settings.specialSeed = CardCrawlGame.saveFile.special_seed;
        Settings.seedSet = CardCrawlGame.saveFile.seed_set;
        Settings.isTrial = CardCrawlGame.saveFile.is_trial;
        if (Settings.isTrial) {
            ModHelper.setTodaysMods(Settings.seed, AbstractDungeon.player.chosenClass);
            AbstractPlayer.customMods = CardCrawlGame.saveFile.custom_mods;
        } else if (Settings.isDailyRun) {
            ModHelper.setTodaysMods(Settings.specialSeed, AbstractDungeon.player.chosenClass);
        }
        AbstractPlayer.customMods = CardCrawlGame.saveFile.custom_mods;
        if (AbstractPlayer.customMods == null) {
            AbstractPlayer.customMods = new ArrayList();
        }
        p.currentHealth = CardCrawlGame.saveFile.current_health;
        p.maxHealth = CardCrawlGame.saveFile.max_health;
        p.displayGold = p.gold = CardCrawlGame.saveFile.gold;
        p.masterHandSize = CardCrawlGame.saveFile.hand_size;
        p.potionSlots = CardCrawlGame.saveFile.potion_slots;
        if (p.potionSlots == 0) {
            p.potionSlots = 3;
        }
        p.potions.clear();
        for (int i = 0; i < p.potionSlots; ++i) {
            p.potions.add(new PotionSlot(i));
        }
        p.masterMaxOrbs = CardCrawlGame.saveFile.max_orbs;
        p.energy = new EnergyManager(CardCrawlGame.saveFile.red + CardCrawlGame.saveFile.green + CardCrawlGame.saveFile.blue);
        monstersSlain = CardCrawlGame.saveFile.monsters_killed;
        elites1Slain = CardCrawlGame.saveFile.elites1_killed;
        elites2Slain = CardCrawlGame.saveFile.elites2_killed;
        elites3Slain = CardCrawlGame.saveFile.elites3_killed;
        goldGained = CardCrawlGame.saveFile.gold_gained;
        champion = CardCrawlGame.saveFile.champions;
        perfect = CardCrawlGame.saveFile.perfect;
        combo = CardCrawlGame.saveFile.combo;
        overkill = CardCrawlGame.saveFile.overkill;
        mysteryMachine = CardCrawlGame.saveFile.mystery_machine;
        playtime = CardCrawlGame.saveFile.play_time;
        AbstractDungeon.ascensionLevel = CardCrawlGame.saveFile.ascension_level;
        AbstractDungeon.isAscensionMode = CardCrawlGame.saveFile.is_ascension_mode;
        p.masterDeck.clear();
        for (CardSave cardSave : CardCrawlGame.saveFile.cards) {
            logger.info(cardSave.id + ", " + cardSave.upgrades);
            p.masterDeck.addToTop(CardLibrary.getCopy(cardSave.id, cardSave.upgrades, cardSave.misc));
        }
        Settings.isEndless = CardCrawlGame.saveFile.is_endless_mode;
        int index = 0;
        p.blights.clear();
        if (CardCrawlGame.saveFile.blights != null) {
            for (String b : CardCrawlGame.saveFile.blights) {
                AbstractBlight blight = BlightHelper.getBlight(b);
                if (blight != null) {
                    int incrementAmount = CardCrawlGame.saveFile.endless_increments.get(index);
                    for (int i = 0; i < incrementAmount; ++i) {
                        blight.incrementUp();
                    }
                    blight.setIncrement(incrementAmount);
                    blight.instantObtain(AbstractDungeon.player, index, false);
                }
                ++index;
            }
            if (CardCrawlGame.saveFile.blight_counters != null) {
                index = 0;
                for (Integer i : CardCrawlGame.saveFile.blight_counters) {
                    p.blights.get(index).setCounter(i);
                    p.blights.get(index).updateDescription(p.chosenClass);
                    ++index;
                }
            }
        }
        p.relics.clear();
        index = 0;
        for (String s : CardCrawlGame.saveFile.relics) {
            AbstractRelic r = RelicLibrary.getRelic(s).makeCopy();
            r.instantObtain(p, index, false);
            if (index < CardCrawlGame.saveFile.relic_counters.size()) {
                r.setCounter(CardCrawlGame.saveFile.relic_counters.get(index));
            }
            r.updateDescription(p.chosenClass);
            ++index;
        }
        index = 0;
        for (String s : CardCrawlGame.saveFile.potions) {
            AbstractPotion potion = PotionHelper.getPotion(s);
            if (potion != null) {
                AbstractDungeon.player.obtainPotion(index, potion);
            }
            ++index;
        }
        Object var3_11 = null;
        if (CardCrawlGame.saveFile.bottled_flame != null) {
            void var3_12;
            for (AbstractCard i : AbstractDungeon.player.masterDeck.group) {
                if (!i.cardID.equals(CardCrawlGame.saveFile.bottled_flame)) continue;
                AbstractCard abstractCard = i;
                if (i.timesUpgraded != CardCrawlGame.saveFile.bottled_flame_upgrade || i.misc != CardCrawlGame.saveFile.bottled_flame_misc) continue;
                break;
            }
            if (var3_12 != null) {
                var3_12.inBottleFlame = true;
                ((BottledFlame)AbstractDungeon.player.getRelic((String)"Bottled Flame")).card = var3_12;
                ((BottledFlame)AbstractDungeon.player.getRelic("Bottled Flame")).setDescriptionAfterLoading();
            }
        }
        Object var3_14 = null;
        if (CardCrawlGame.saveFile.bottled_lightning != null) {
            void var3_15;
            for (AbstractCard i : AbstractDungeon.player.masterDeck.group) {
                if (!i.cardID.equals(CardCrawlGame.saveFile.bottled_lightning)) continue;
                AbstractCard abstractCard = i;
                if (i.timesUpgraded != CardCrawlGame.saveFile.bottled_lightning_upgrade || i.misc != CardCrawlGame.saveFile.bottled_lightning_misc) continue;
                break;
            }
            if (var3_15 != null) {
                var3_15.inBottleLightning = true;
                ((BottledLightning)AbstractDungeon.player.getRelic((String)"Bottled Lightning")).card = var3_15;
                ((BottledLightning)AbstractDungeon.player.getRelic("Bottled Lightning")).setDescriptionAfterLoading();
            }
        }
        Object var3_17 = null;
        if (CardCrawlGame.saveFile.bottled_tornado != null) {
            void var3_18;
            for (AbstractCard i : AbstractDungeon.player.masterDeck.group) {
                if (!i.cardID.equals(CardCrawlGame.saveFile.bottled_tornado)) continue;
                AbstractCard abstractCard = i;
                if (i.timesUpgraded != CardCrawlGame.saveFile.bottled_tornado_upgrade || i.misc != CardCrawlGame.saveFile.bottled_tornado_misc) continue;
                break;
            }
            if (var3_18 != null) {
                var3_18.inBottleTornado = true;
                ((BottledTornado)AbstractDungeon.player.getRelic((String)"Bottled Tornado")).card = var3_18;
                ((BottledTornado)AbstractDungeon.player.getRelic("Bottled Tornado")).setDescriptionAfterLoading();
            }
        }
        if (CardCrawlGame.saveFile.daily_mods != null && CardCrawlGame.saveFile.daily_mods.size() > 0) {
            ModHelper.setMods(CardCrawlGame.saveFile.daily_mods);
        }
        metricData.clearData();
        CardCrawlGame.metricData.campfire_rested = CardCrawlGame.saveFile.metric_campfire_rested;
        CardCrawlGame.metricData.campfire_upgraded = CardCrawlGame.saveFile.metric_campfire_upgraded;
        CardCrawlGame.metricData.purchased_purges = CardCrawlGame.saveFile.metric_purchased_purges;
        CardCrawlGame.metricData.potions_floor_spawned = CardCrawlGame.saveFile.metric_potions_floor_spawned;
        CardCrawlGame.metricData.current_hp_per_floor = CardCrawlGame.saveFile.metric_current_hp_per_floor;
        CardCrawlGame.metricData.max_hp_per_floor = CardCrawlGame.saveFile.metric_max_hp_per_floor;
        CardCrawlGame.metricData.gold_per_floor = CardCrawlGame.saveFile.metric_gold_per_floor;
        CardCrawlGame.metricData.path_per_floor = CardCrawlGame.saveFile.metric_path_per_floor;
        CardCrawlGame.metricData.path_taken = CardCrawlGame.saveFile.metric_path_taken;
        CardCrawlGame.metricData.items_purchased = CardCrawlGame.saveFile.metric_items_purchased;
        CardCrawlGame.metricData.items_purged = CardCrawlGame.saveFile.metric_items_purged;
        CardCrawlGame.metricData.card_choices = CardCrawlGame.saveFile.metric_card_choices;
        CardCrawlGame.metricData.event_choices = CardCrawlGame.saveFile.metric_event_choices;
        CardCrawlGame.metricData.damage_taken = CardCrawlGame.saveFile.metric_damage_taken;
        CardCrawlGame.metricData.boss_relics = CardCrawlGame.saveFile.metric_boss_relics;
        if (CardCrawlGame.saveFile.metric_potions_obtained != null) {
            CardCrawlGame.metricData.potions_obtained = CardCrawlGame.saveFile.metric_potions_obtained;
        }
        if (CardCrawlGame.saveFile.metric_relics_obtained != null) {
            CardCrawlGame.metricData.relics_obtained = CardCrawlGame.saveFile.metric_relics_obtained;
        }
        if (CardCrawlGame.saveFile.metric_campfire_choices != null) {
            CardCrawlGame.metricData.campfire_choices = CardCrawlGame.saveFile.metric_campfire_choices;
        }
        if (CardCrawlGame.saveFile.metric_item_purchase_floors != null) {
            CardCrawlGame.metricData.item_purchase_floors = CardCrawlGame.saveFile.metric_item_purchase_floors;
        }
        if (CardCrawlGame.saveFile.metric_items_purged_floors != null) {
            CardCrawlGame.metricData.items_purged_floors = CardCrawlGame.saveFile.metric_items_purged_floors;
        }
        if (CardCrawlGame.saveFile.neow_bonus != null) {
            CardCrawlGame.metricData.neowBonus = CardCrawlGame.saveFile.neow_bonus;
        }
        if (CardCrawlGame.saveFile.neow_cost != null) {
            CardCrawlGame.metricData.neowCost = CardCrawlGame.saveFile.neow_cost;
        }
    }

    private static AbstractPlayer createCharacter(AbstractPlayer.PlayerClass selection) {
        AbstractPlayer p = characterManager.recreateCharacter(selection);
        for (AbstractCard c : p.masterDeck.group) {
            UnlockTracker.markCardAsSeen(c.cardID);
        }
        return p;
    }

    private void updateDebugSwitch() {
        if (!Settings.isDev) {
            return;
        }
        if (DevInputActionSet.toggleDebug.isJustPressed()) {
            Settings.isDebug = !Settings.isDebug;
            return;
        }
        if (DevInputActionSet.toggleInfo.isJustPressed()) {
            Settings.isInfo = !Settings.isInfo;
            return;
        }
        if (Settings.isDebug && DevInputActionSet.uploadData.isJustPressed()) {
            RelicLibrary.uploadRelicData();
            CardLibrary.uploadCardData();
            MonsterHelper.uploadEnemyData();
            PotionHelper.uploadPotionData();
            ModHelper.uploadModData();
            BlightHelper.uploadBlightData();
            BotDataUploader.uploadKeywordData();
            return;
        }
        if (!Settings.isDebug) {
            return;
        }
        if (DevInputActionSet.hideTopBar.isJustPressed()) {
            Settings.hideTopBar = !Settings.hideTopBar;
            return;
        }
        if (DevInputActionSet.hidePopUps.isJustPressed()) {
            Settings.hidePopupDetails = !Settings.hidePopupDetails;
            return;
        }
        if (DevInputActionSet.hideRelics.isJustPressed()) {
            Settings.hideRelics = !Settings.hideRelics;
            return;
        }
        if (DevInputActionSet.hideCombatLowUI.isJustPressed()) {
            Settings.hideLowerElements = !Settings.hideLowerElements;
            return;
        }
        if (DevInputActionSet.hideCards.isJustPressed()) {
            Settings.hideCards = !Settings.hideCards;
            return;
        }
        if (DevInputActionSet.hideEndTurnButton.isJustPressed()) {
            boolean bl = Settings.hideEndTurn = !Settings.hideEndTurn;
            if (AbstractDungeon.getMonsters() == null) {
                return;
            }
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                m.damage(new DamageInfo(AbstractDungeon.player, m.currentHealth, DamageInfo.DamageType.HP_LOSS));
            }
            return;
        }
        if (DevInputActionSet.hideCombatInfo.isJustPressed()) {
            Settings.hideCombatElements = !Settings.hideCombatElements;
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    public AbstractDungeon getDungeon(String key, AbstractPlayer p) {
        switch (key) {
            case "Exordium": {
                ArrayList<String> emptyList = new ArrayList<String>();
                return new Exordium(p, emptyList);
            }
            case "TheCity": {
                return new TheCity(p, AbstractDungeon.specialOneTimeEventList);
            }
            case "TheBeyond": {
                return new TheBeyond(p, AbstractDungeon.specialOneTimeEventList);
            }
            case "TheEnding": {
                return new TheEnding(p, AbstractDungeon.specialOneTimeEventList);
            }
        }
        return null;
    }

    public AbstractDungeon getDungeon(String key, AbstractPlayer p, SaveFile saveFile) {
        switch (key) {
            case "Exordium": {
                return new Exordium(p, saveFile);
            }
            case "TheCity": {
                return new TheCity(p, saveFile);
            }
            case "TheBeyond": {
                return new TheBeyond(p, saveFile);
            }
            case "TheEnding": {
                return new TheEnding(p, saveFile);
            }
        }
        return null;
    }

    @Override
    public void pause() {
        logger.info("PAUSE()");
        Settings.isControllerMode = false;
        if (MUTE_IF_BG && mainMenuScreen != null) {
            Settings.isBackgrounded = true;
            if (mode == GameMode.CHAR_SELECT) {
                mainMenuScreen.muteAmbienceVolume();
            } else if (AbstractDungeon.scene != null) {
                AbstractDungeon.scene.muteAmbienceVolume();
            }
        }
    }

    @Override
    public void resume() {
        logger.info("RESUME()");
        if (MUTE_IF_BG && mainMenuScreen != null) {
            Settings.isBackgrounded = false;
            if (mode == GameMode.CHAR_SELECT) {
                mainMenuScreen.updateAmbienceVolume();
            } else if (AbstractDungeon.scene != null) {
                AbstractDungeon.scene.updateAmbienceVolume();
            }
        }
    }

    @Override
    public void dispose() {
        logger.info("Game shutting down...");
        logger.info("Flushing saves to disk...");
        AsyncSaver.shutdownSaveThread();
        if (SteamInputHelper.alive) {
            logger.info("Shutting down controller handler...");
            SteamInputHelper.alive = false;
            SteamInputHelper.controller.shutdown();
            if (clientUtils != null) {
                clientUtils.dispose();
            }
        }
        if (sInputDetectThread != null) {
            logger.info("Steam input detection was running! Shutting down...");
            sInputDetectThread.interrupt();
        }
        logger.info("Shutting down publisher integrations...");
        publisherIntegration.dispose();
        logger.info("Flushing logs to disk. Clean shutdown successful.");
        LogManager.shutdown();
    }

    public static String generateRandomAlias() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder retVal = new StringBuilder();
        for (int i = 0; i < 16; ++i) {
            retVal.append(alphabet.charAt(MathUtils.random(0, alphabet.length() - 1)));
        }
        return retVal.toString();
    }

    public static boolean isInARun() {
        return mode == GameMode.GAMEPLAY && AbstractDungeon.player != null && !AbstractDungeon.player.isDead;
    }

    public static Texture getSaveSlotImg() {
        switch (saveSlot) {
            case 0: {
                return ImageMaster.PROFILE_A;
            }
            case 1: {
                return ImageMaster.PROFILE_B;
            }
            case 2: {
                return ImageMaster.PROFILE_C;
            }
        }
        return ImageMaster.PROFILE_A;
    }

    static {
        isPopupOpen = false;
        screenShake = new ScreenShake();
        mode = GameMode.CHAR_SELECT;
        startOver = false;
        queueCredits = false;
        playCreditsBgm = false;
        MUTE_IF_BG = false;
        chosenCharacter = null;
        saveFile = null;
        saveSlot = 0;
        monstersSlain = 0;
        elites1Slain = 0;
        elites2Slain = 0;
        elites3Slain = 0;
        elitesModdedSlain = 0;
        champion = 0;
        perfect = 0;
        overkill = false;
        combo = false;
        cheater = false;
        goldGained = 0;
        cardsPurged = 0;
        potionsBought = 0;
        mysteryMachine = 0;
        playtime = 0.0f;
        stopClock = false;
        trial = null;
        sInputDetectThread = null;
        screenColor = Color.BLACK.cpy();
        screenTimer = 2.0f;
        screenTime = 2.0f;
        fadeIn = true;
        displayVersion = true;
        preferenceDir = null;
        logger = LogManager.getLogger(CardCrawlGame.class.getName());
    }

    public static enum GameMode {
        CHAR_SELECT,
        GAMEPLAY,
        DUNGEON_TRANSITION,
        SPLASH;

    }
}

