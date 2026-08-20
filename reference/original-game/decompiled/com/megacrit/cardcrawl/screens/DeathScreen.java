/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.screens.GameOverStat;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.ui.buttons.DynamicBanner;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.unlock.misc.DefectUnlock;
import com.megacrit.cardcrawl.unlock.misc.TheSilentUnlock;
import com.megacrit.cardcrawl.unlock.misc.WatcherUnlock;
import com.megacrit.cardcrawl.vfx.AscensionLevelUpTextEffect;
import com.megacrit.cardcrawl.vfx.AscensionUnlockedTextEffect;
import com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeathScreen
extends GameOverScreen {
    private static final Logger logger = LogManager.getLogger(DeathScreen.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("DeathScreen");
    public static final String[] TEXT = DeathScreen.uiStrings.TEXT;
    private MonsterGroup monsters;
    private String deathText;
    private ArrayList<DeathScreenFloatyEffect> particles = new ArrayList();
    private static final float NUM_PARTICLES = 50.0f;
    private float deathAnimWaitTimer = 1.0f;
    private static final float DEATH_TEXT_TIME = 5.0f;
    private float deathTextTimer = 5.0f;
    private Color defeatTextColor = Color.WHITE.cpy();
    private Color deathTextColor = Settings.BLUE_TEXT_COLOR.cpy();
    private static final float DEATH_TEXT_Y = (float)Settings.HEIGHT - 360.0f * Settings.scale;
    private boolean defectUnlockedThisRun = false;

    public DeathScreen(MonsterGroup m) {
        this.playtime = (long)CardCrawlGame.playtime;
        if (this.playtime < 0L) {
            this.playtime = 0L;
        }
        AbstractDungeon.getCurrRoom().clearEvent();
        DeathScreen.resetScoreChecks();
        AbstractDungeon.is_victory = false;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            c.unhover();
        }
        if (AbstractDungeon.player.stance != null) {
            AbstractDungeon.player.stance.stopIdleSfx();
        }
        AbstractDungeon.dungeonMapScreen.closeInstantly();
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.DEATH;
        AbstractDungeon.overlayMenu.showBlackScreen(1.0f);
        AbstractDungeon.previousScreen = null;
        AbstractDungeon.overlayMenu.cancelButton.hideInstantly();
        AbstractDungeon.isScreenUp = true;
        this.deathText = this.getDeathText();
        this.monsters = m;
        logger.info("PLAYTIME: " + this.playtime);
        if (AbstractDungeon.getCurrRoom() instanceof RestRoom) {
            ((RestRoom)AbstractDungeon.getCurrRoom()).cutFireSound();
        }
        if (!(isVictory = AbstractDungeon.getCurrRoom() instanceof VictoryRoom)) {
            PublisherIntegration pubInteg = CardCrawlGame.publisherIntegration;
            String winStreakStatId = Settings.isBeta ? AbstractDungeon.player.getWinStreakKey() + "_BETA" : AbstractDungeon.player.getWinStreakKey();
            pubInteg.setStat(winStreakStatId, 0);
            logger.info("WIN STREAK  " + pubInteg.getStat(winStreakStatId));
        }
        this.showingStats = false;
        this.returnButton = new ReturnToMenuButton();
        this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[0]);
        if (AbstractDungeon.getCurrRoom() instanceof VictoryRoom) {
            AbstractDungeon.dynamicBanner.appear(TEXT[1]);
        } else {
            AbstractDungeon.dynamicBanner.appear(this.getDeathBannerText());
        }
        if (Settings.isStandardRun()) {
            if (AbstractDungeon.floorNum >= 16) {
                logger.info("Neow available");
                CardCrawlGame.playerPref.putInteger(AbstractDungeon.player.chosenClass.name() + "_SPIRITS", 1);
            } else {
                logger.info("No Neow for you");
                CardCrawlGame.playerPref.putInteger(AbstractDungeon.player.chosenClass.name() + "_SPIRITS", 0);
                AbstractDungeon.bossCount = 0;
            }
        }
        CardCrawlGame.music.dispose();
        CardCrawlGame.sound.play("DEATH_STINGER", true);
        String bgmKey = null;
        switch (MathUtils.random(0, 3)) {
            case 0: {
                bgmKey = "STS_DeathStinger_1_v3_MUSIC.ogg";
                break;
            }
            case 1: {
                bgmKey = "STS_DeathStinger_2_v3_MUSIC.ogg";
                break;
            }
            case 2: {
                bgmKey = "STS_DeathStinger_3_v3_MUSIC.ogg";
                break;
            }
            case 3: {
                bgmKey = "STS_DeathStinger_4_v3_MUSIC.ogg";
                break;
            }
        }
        CardCrawlGame.music.playTempBgmInstantly(bgmKey, false);
        if (isVictory) {
            UnlockTracker.unlockAchievement(AbstractDungeon.player.getAchievementKey());
            this.submitVictoryMetrics();
            if (this.playtime != 0L) {
                StatsScreen.updateVictoryTime(this.playtime);
            }
            StatsScreen.incrementVictory(AbstractDungeon.player.getCharStat());
            if (AbstractDungeon.ascensionLevel == 10 && !Settings.isTrial) {
                UnlockTracker.unlockAchievement("ASCEND_10");
            } else if (AbstractDungeon.ascensionLevel == 20 && !Settings.isTrial) {
                UnlockTracker.unlockAchievement("ASCEND_20");
            }
        } else {
            if (AbstractDungeon.ascensionLevel == 20 && AbstractDungeon.actNum == 4) {
                UnlockTracker.unlockAchievement("ASCEND_20");
            }
            this.submitDefeatMetrics(m);
            StatsScreen.incrementDeath(AbstractDungeon.player.getCharStat());
        }
        if (Settings.isStandardRun() && AbstractDungeon.actNum > 3) {
            StatsScreen.incrementVictoryIfZero(AbstractDungeon.player.getCharStat());
        }
        this.defeatTextColor.a = 0.0f;
        this.deathTextColor.a = 0.0f;
        if (this.playtime != 0L) {
            StatsScreen.incrementPlayTime(this.playtime);
        }
        if (Settings.isStandardRun()) {
            StatsScreen.updateFurthestAscent(AbstractDungeon.floorNum);
        } else if (Settings.isDailyRun) {
            StatsScreen.updateHighestDailyScore(AbstractDungeon.floorNum);
        }
        if (SaveHelper.shouldDeleteSave()) {
            SaveAndContinue.deleteSave(AbstractDungeon.player);
        }
        this.calculateUnlockProgress();
        if (!Settings.isEndless) {
            this.uploadToSteamLeaderboards();
        }
        this.createGameOverStats();
        CardCrawlGame.playerPref.flush();
    }

    private void createGameOverStats() {
        this.stats.clear();
        this.stats.add(new GameOverStat(TEXT[2] + " (" + AbstractDungeon.floorNum + ")", null, Integer.toString(floorPoints)));
        this.stats.add(new GameOverStat(TEXT[43] + " (" + CardCrawlGame.monstersSlain + ")", null, Integer.toString(monsterPoints)));
        this.stats.add(new GameOverStat(DeathScreen.EXORDIUM_ELITE.NAME + " (" + CardCrawlGame.elites1Slain + ")", null, Integer.toString(elite1Points)));
        if (Settings.isEndless) {
            if (CardCrawlGame.elites2Slain > 0) {
                this.stats.add(new GameOverStat(DeathScreen.CITY_ELITE.NAME + " (" + CardCrawlGame.elites2Slain + ")", null, Integer.toString(elite2Points)));
            }
        } else if (CardCrawlGame.dungeon instanceof TheCity || CardCrawlGame.dungeon instanceof TheBeyond || CardCrawlGame.dungeon instanceof TheEnding) {
            this.stats.add(new GameOverStat(DeathScreen.CITY_ELITE.NAME + " (" + CardCrawlGame.elites2Slain + ")", null, Integer.toString(elite2Points)));
        }
        if (Settings.isEndless) {
            if (CardCrawlGame.elites3Slain > 0) {
                this.stats.add(new GameOverStat(DeathScreen.BEYOND_ELITE.NAME + " (" + CardCrawlGame.elites3Slain + ")", null, Integer.toString(elite3Points)));
            }
        } else if (CardCrawlGame.dungeon instanceof TheBeyond || CardCrawlGame.dungeon instanceof TheEnding) {
            this.stats.add(new GameOverStat(DeathScreen.BEYOND_ELITE.NAME + " (" + CardCrawlGame.elites3Slain + ")", null, Integer.toString(elite3Points)));
        }
        this.stats.add(new GameOverStat(DeathScreen.BOSSES_SLAIN.NAME + " (" + AbstractDungeon.bossCount + ")", null, Integer.toString(bossPoints)));
        if (IS_POOPY) {
            this.stats.add(new GameOverStat(DeathScreen.POOPY.NAME, DeathScreen.POOPY.DESCRIPTIONS[0], Integer.toString(-1)));
        }
        if (IS_SPEEDSTER) {
            this.stats.add(new GameOverStat(DeathScreen.SPEEDSTER.NAME, DeathScreen.SPEEDSTER.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (IS_LIGHT_SPEED) {
            this.stats.add(new GameOverStat(DeathScreen.LIGHT_SPEED.NAME, DeathScreen.LIGHT_SPEED.DESCRIPTIONS[0], Integer.toString(50)));
        }
        if (IS_HIGHLANDER) {
            this.stats.add(new GameOverStat(DeathScreen.HIGHLANDER.NAME, DeathScreen.HIGHLANDER.DESCRIPTIONS[0], Integer.toString(100)));
        }
        if (IS_SHINY) {
            this.stats.add(new GameOverStat(DeathScreen.SHINY.NAME, DeathScreen.SHINY.DESCRIPTIONS[0], Integer.toString(50)));
        }
        if (IS_I_LIKE_GOLD) {
            this.stats.add(new GameOverStat(DeathScreen.I_LIKE_GOLD.NAME + " (" + CardCrawlGame.goldGained + ")", DeathScreen.I_LIKE_GOLD.DESCRIPTIONS[0], Integer.toString(75)));
        } else if (IS_RAINING_MONEY) {
            this.stats.add(new GameOverStat(DeathScreen.RAINING_MONEY.NAME + " (" + CardCrawlGame.goldGained + ")", DeathScreen.RAINING_MONEY.DESCRIPTIONS[0], Integer.toString(50)));
        } else if (IS_MONEY_MONEY) {
            this.stats.add(new GameOverStat(DeathScreen.MONEY_MONEY.NAME + " (" + CardCrawlGame.goldGained + ")", DeathScreen.MONEY_MONEY.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (IS_MYSTERY_MACHINE) {
            this.stats.add(new GameOverStat(DeathScreen.MYSTERY_MACHINE.NAME + " (" + CardCrawlGame.mysteryMachine + ")", DeathScreen.MYSTERY_MACHINE.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (IS_FULL_SET > 0) {
            this.stats.add(new GameOverStat(DeathScreen.COLLECTOR.NAME + " (" + IS_FULL_SET + ")", DeathScreen.COLLECTOR.DESCRIPTIONS[0], Integer.toString(25 * IS_FULL_SET)));
        }
        if (IS_PAUPER) {
            this.stats.add(new GameOverStat(DeathScreen.PAUPER.NAME, DeathScreen.PAUPER.DESCRIPTIONS[0], Integer.toString(50)));
        }
        if (IS_LIBRARY) {
            this.stats.add(new GameOverStat(DeathScreen.LIBRARIAN.NAME, DeathScreen.LIBRARIAN.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (IS_ENCYCLOPEDIA) {
            this.stats.add(new GameOverStat(DeathScreen.ENCYCLOPEDIAN.NAME, DeathScreen.ENCYCLOPEDIAN.DESCRIPTIONS[0], Integer.toString(50)));
        }
        if (IS_STUFFED) {
            this.stats.add(new GameOverStat(DeathScreen.STUFFED.NAME, DeathScreen.STUFFED.DESCRIPTIONS[0], Integer.toString(50)));
        } else if (IS_WELL_FED) {
            this.stats.add(new GameOverStat(DeathScreen.WELL_FED.NAME, DeathScreen.WELL_FED.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (IS_CURSES) {
            this.stats.add(new GameOverStat(DeathScreen.CURSES.NAME, DeathScreen.CURSES.DESCRIPTIONS[0], Integer.toString(100)));
        }
        if (IS_ON_MY_OWN) {
            this.stats.add(new GameOverStat(DeathScreen.ON_MY_OWN_TERMS.NAME, DeathScreen.ON_MY_OWN_TERMS.DESCRIPTIONS[0], Integer.toString(50)));
        }
        if (CardCrawlGame.champion > 0) {
            this.stats.add(new GameOverStat(DeathScreen.CHAMPION.NAME + " (" + CardCrawlGame.champion + ")", DeathScreen.CHAMPION.DESCRIPTIONS[0], Integer.toString(25 * CardCrawlGame.champion)));
        }
        if (CardCrawlGame.perfect >= 3) {
            this.stats.add(new GameOverStat(DeathScreen.BEYOND_PERFECT.NAME, DeathScreen.BEYOND_PERFECT.DESCRIPTIONS[0], Integer.toString(200)));
        } else if (CardCrawlGame.perfect > 0) {
            this.stats.add(new GameOverStat(DeathScreen.PERFECT.NAME + " (" + CardCrawlGame.perfect + ")", DeathScreen.PERFECT.DESCRIPTIONS[0], Integer.toString(50 * CardCrawlGame.perfect)));
        }
        if (CardCrawlGame.overkill) {
            this.stats.add(new GameOverStat(DeathScreen.OVERKILL.NAME, DeathScreen.OVERKILL.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (CardCrawlGame.combo) {
            this.stats.add(new GameOverStat(DeathScreen.COMBO.NAME, DeathScreen.COMBO.DESCRIPTIONS[0], Integer.toString(25)));
        }
        if (AbstractDungeon.isAscensionMode) {
            this.stats.add(new GameOverStat(DeathScreen.ASCENSION.NAME + " (" + AbstractDungeon.ascensionLevel + ")", DeathScreen.ASCENSION.DESCRIPTIONS[0], Integer.toString(ascensionPoints)));
        }
        this.stats.add(new GameOverStat());
        this.stats.add(new GameOverStat(TEXT[6], null, Integer.toString(this.score)));
    }

    private void submitDefeatMetrics(MonsterGroup m) {
        if (m != null && !m.areMonstersDead() && !m.areMonstersBasicallyDead()) {
            CardCrawlGame.metricData.addEncounterData();
        }
        Metrics metrics = new Metrics();
        metrics.gatherAllDataAndSave(true, false, m);
        if (DeathScreen.shouldUploadMetricData()) {
            metrics.setValues(true, false, m, Metrics.MetricRequestType.UPLOAD_METRICS);
            Thread t = new Thread(metrics);
            t.setName("Metrics");
            t.start();
        }
    }

    @Override
    protected void submitVictoryMetrics() {
        Metrics metrics = new Metrics();
        metrics.gatherAllDataAndSave(false, false, null);
        if (DeathScreen.shouldUploadMetricData()) {
            metrics.setValues(false, false, null, Metrics.MetricRequestType.UPLOAD_METRICS);
            Thread t = new Thread(metrics);
            t.start();
        }
        if (Settings.isStandardRun()) {
            StatsScreen.updateFurthestAscent(AbstractDungeon.floorNum);
        }
        if (SaveHelper.shouldDeleteSave()) {
            SaveAndContinue.deleteSave(AbstractDungeon.player);
        }
    }

    private String getDeathBannerText() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(TEXT[7]);
        list.add(TEXT[8]);
        list.add(TEXT[9]);
        list.add(TEXT[10]);
        list.add(TEXT[11]);
        list.add(TEXT[12]);
        list.add(TEXT[13]);
        list.add(TEXT[14]);
        return (String)list.get(MathUtils.random(list.size() - 1));
    }

    private String getDeathText() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(TEXT[15]);
        list.add(TEXT[16]);
        list.add(TEXT[17]);
        list.add(TEXT[18]);
        list.add(TEXT[19]);
        list.add(TEXT[20]);
        list.add(TEXT[21]);
        list.add(TEXT[22]);
        list.add(TEXT[23]);
        list.add(TEXT[24]);
        list.add(TEXT[25]);
        list.add(TEXT[26]);
        list.add(TEXT[27]);
        list.add(TEXT[28]);
        list.add(TEXT[29]);
        if (AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
            list.add("...");
        }
        return (String)list.get(MathUtils.random(list.size() - 1));
    }

    public void hide() {
        this.returnButton.hide();
        AbstractDungeon.dynamicBanner.hide();
    }

    public void reopen() {
        this.reopen(false);
    }

    public void reopen(boolean fromVictoryUnlock) {
        AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.DEATH;
        this.statsTimer = 0.5f;
        if (isVictory) {
            AbstractDungeon.dynamicBanner.appearInstantly(TEXT[1]);
        } else {
            AbstractDungeon.dynamicBanner.appearInstantly(TEXT[30]);
        }
        AbstractDungeon.overlayMenu.showBlackScreen(1.0f);
        if (fromVictoryUnlock) {
            this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[31]);
        } else if (!this.showingStats) {
            this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[32]);
        } else if (this.unlockBundle == null) {
            if (!isVictory) {
                if (UnlockTracker.isCharacterLocked("The Silent") || UnlockTracker.isCharacterLocked("Defect") && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT || this.willWatcherUnlock()) {
                    this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[40]);
                } else {
                    this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[34]);
                }
            } else {
                this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[35]);
            }
        } else {
            this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[36]);
        }
    }

    private boolean willWatcherUnlock() {
        if (this.defectUnlockedThisRun || !UnlockTracker.isCharacterLocked("Watcher")) {
            return false;
        }
        return !UnlockTracker.isCharacterLocked("Defect") && (UnlockTracker.isAchievementUnlocked("RUBY") || UnlockTracker.isAchievementUnlocked("EMERALD") || UnlockTracker.isAchievementUnlocked("SAPPHIRE"));
    }

    public void update() {
        if (Settings.isDebug && InputHelper.justClickedRight) {
            UnlockTracker.resetUnlockProgress(AbstractDungeon.player.chosenClass);
        }
        this.updateControllerInput();
        this.returnButton.update();
        if (this.returnButton.hb.clicked || this.returnButton.show && CInputActionSet.select.isJustPressed()) {
            CInputActionSet.topPanel.unpress();
            if (Settings.isControllerMode) {
                Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
            }
            this.returnButton.hb.clicked = false;
            if (!this.showingStats) {
                this.showingStats = true;
                this.statsTimer = 0.5f;
                logger.info("Clicked");
                this.returnButton = new ReturnToMenuButton();
                this.updateAscensionProgress();
                if (this.unlockBundle == null) {
                    if (!isVictory) {
                        if (UnlockTracker.isCharacterLocked("The Silent") || UnlockTracker.isCharacterLocked("Defect") && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT || this.willWatcherUnlock()) {
                            this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[40]);
                        } else {
                            this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[37]);
                        }
                    } else {
                        this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[39]);
                    }
                } else {
                    this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[40]);
                }
            } else if (this.unlockBundle != null) {
                AbstractDungeon.gUnlockScreen.open(this.unlockBundle, false);
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.DEATH;
                AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NEOW_UNLOCK;
                this.unlockBundle = null;
                if (UnlockTracker.isCharacterLocked("The Silent")) {
                    this.returnButton.appear((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT * 0.15f, TEXT[40]);
                } else {
                    this.returnButton.label = TEXT[37];
                }
            } else if (isVictory) {
                this.returnButton.hide();
                if (AbstractDungeon.unlocks.isEmpty() || Settings.isDemo) {
                    if (Settings.isDemo || Settings.isDailyRun) {
                        CardCrawlGame.startOver();
                    } else if (UnlockTracker.isCharacterLocked("The Silent")) {
                        AbstractDungeon.unlocks.add(new TheSilentUnlock());
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else if (UnlockTracker.isCharacterLocked("Defect") && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
                        AbstractDungeon.unlocks.add(new DefectUnlock());
                        this.defectUnlockedThisRun = true;
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else if (this.willWatcherUnlock()) {
                        AbstractDungeon.unlocks.add(new WatcherUnlock());
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else {
                        CardCrawlGame.playCreditsBgm = false;
                        CardCrawlGame.startOverButShowCredits();
                    }
                } else {
                    AbstractDungeon.unlocks.clear();
                    Settings.isTrial = false;
                    Settings.isDailyRun = false;
                    Settings.isEndless = false;
                    CardCrawlGame.trial = null;
                    if (Settings.isDailyRun) {
                        CardCrawlGame.startOver();
                    } else {
                        CardCrawlGame.playCreditsBgm = false;
                        CardCrawlGame.startOverButShowCredits();
                    }
                }
            } else {
                this.returnButton.hide();
                if (AbstractDungeon.unlocks.isEmpty() || Settings.isDemo || Settings.isDailyRun || Settings.isTrial) {
                    if (UnlockTracker.isCharacterLocked("The Silent")) {
                        AbstractDungeon.unlocks.add(new TheSilentUnlock());
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else if (UnlockTracker.isCharacterLocked("Defect") && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
                        AbstractDungeon.unlocks.add(new DefectUnlock());
                        this.defectUnlockedThisRun = true;
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else if (this.willWatcherUnlock()) {
                        AbstractDungeon.unlocks.add(new WatcherUnlock());
                        AbstractDungeon.unlockScreen.open(AbstractDungeon.unlocks.remove(0));
                    } else {
                        Settings.isTrial = false;
                        Settings.isDailyRun = false;
                        Settings.isEndless = false;
                        CardCrawlGame.trial = null;
                        CardCrawlGame.startOver();
                    }
                } else {
                    AbstractDungeon.unlocks.clear();
                    Settings.isTrial = false;
                    Settings.isDailyRun = false;
                    Settings.isEndless = false;
                    CardCrawlGame.trial = null;
                    CardCrawlGame.playCreditsBgm = false;
                    CardCrawlGame.startOverButShowCredits();
                }
            }
        }
        this.updateStatsScreen();
        if (this.deathAnimWaitTimer != 0.0f) {
            this.deathAnimWaitTimer -= Gdx.graphics.getDeltaTime();
            if (this.deathAnimWaitTimer < 0.0f) {
                this.deathAnimWaitTimer = 0.0f;
                AbstractDungeon.player.playDeathAnimation();
            }
        } else {
            this.deathTextTimer -= Gdx.graphics.getDeltaTime();
            if (this.deathTextTimer < 0.0f) {
                this.deathTextTimer = 0.0f;
            }
            this.deathTextColor.a = Interpolation.fade.apply(0.0f, 1.0f, 1.0f - this.deathTextTimer / 5.0f);
            this.defeatTextColor.a = Interpolation.fade.apply(0.0f, 1.0f, 1.0f - this.deathTextTimer / 5.0f);
        }
        if (this.monsters != null) {
            this.monsters.update();
            this.monsters.updateAnimations();
        }
        if ((float)this.particles.size() < 50.0f) {
            this.particles.add(new DeathScreenFloatyEffect());
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode || AbstractDungeon.topPanel.selectPotionMode || !AbstractDungeon.topPanel.potionUi.isHidden || AbstractDungeon.player.viewingRelics) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        if (this.stats != null) {
            for (GameOverStat s : this.stats) {
                if (s.hb.hovered) {
                    anyHovered = true;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            index = -1;
        }
        if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
            --index;
            if (this.stats.size() > 10) {
                int numItemsInRightColumn = (this.stats.size() - 2) / 2;
                if (this.stats.size() % 2 == 0) {
                    --numItemsInRightColumn;
                }
                if (index == numItemsInRightColumn) {
                    index = this.stats.size() - 1;
                } else if (index < 0) {
                    index = this.stats.size() - 1;
                } else if (index == this.stats.size() - 2) {
                    --index;
                }
            } else if (index < 0) {
                index = this.stats.size() - 1;
            } else if (index == this.stats.size() - 2) {
                --index;
            }
            CInputHelper.setCursor(((GameOverStat)this.stats.get((int)index)).hb);
        } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
            if (index == -1) {
                index = 0;
                CInputHelper.setCursor(((GameOverStat)this.stats.get((int)index)).hb);
                return;
            }
            ++index;
            if (this.stats.size() > 10) {
                int numItemsInLeftColumn = (this.stats.size() - 2) / 2;
                if (this.stats.size() % 2 != 0) {
                    ++numItemsInLeftColumn;
                }
                if (index == numItemsInLeftColumn) {
                    index = this.stats.size() - 1;
                }
            } else {
                if (index > this.stats.size() - 1) {
                    index = 0;
                }
                if (index == this.stats.size() - 2) {
                    ++index;
                }
            }
            if (index > this.stats.size() - 3) {
                index = this.stats.size() - 1;
            }
            CInputHelper.setCursor(((GameOverStat)this.stats.get((int)index)).hb);
        } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            if (this.stats.size() > 10) {
                int numItemsInLeftColumn = (this.stats.size() - 2) / 2;
                if (this.stats.size() % 2 != 0) {
                    ++numItemsInLeftColumn;
                }
                if (index < numItemsInLeftColumn - 1) {
                    index += numItemsInLeftColumn;
                } else if (index == numItemsInLeftColumn - 1) {
                    index = this.stats.size() % 2 != 0 ? (index += numItemsInLeftColumn - 1) : (index += numItemsInLeftColumn);
                } else if (index >= numItemsInLeftColumn && index < this.stats.size() - 2) {
                    index -= numItemsInLeftColumn;
                }
            }
            if (index > this.stats.size() - 1) {
                index = this.stats.size() - 1;
            }
            if (index != -1) {
                CInputHelper.setCursor(((GameOverStat)this.stats.get((int)index)).hb);
            }
        }
    }

    private void updateAscensionProgress() {
        if ((isVictory || AbstractDungeon.actNum >= 4) && AbstractDungeon.isAscensionMode && Settings.isStandardRun() && AbstractDungeon.ascensionLevel < 20 && StatsScreen.isPlayingHighestAscension(AbstractDungeon.player.getPrefs())) {
            StatsScreen.incrementAscension(AbstractDungeon.player.getCharStat());
            AbstractDungeon.topLevelEffects.add(new AscensionLevelUpTextEffect());
        } else if (!AbstractDungeon.ascensionCheck && UnlockTracker.isAscensionUnlocked(AbstractDungeon.player) && !Settings.seedSet) {
            AbstractDungeon.topLevelEffects.add(new AscensionUnlockedTextEffect());
        }
    }

    private void updateStatsScreen() {
        if (this.showingStats) {
            this.progressBarAlpha = MathHelper.slowColorLerpSnap(this.progressBarAlpha, 1.0f);
            this.statsTimer -= Gdx.graphics.getDeltaTime();
            if (this.statsTimer < 0.0f) {
                this.statsTimer = 0.0f;
            }
            this.returnButton.y = Interpolation.pow3In.apply((float)Settings.HEIGHT * 0.1f, (float)Settings.HEIGHT * 0.15f, this.statsTimer * 1.0f / 0.5f);
            AbstractDungeon.dynamicBanner.y = Interpolation.pow3In.apply((float)Settings.HEIGHT / 2.0f + 320.0f * Settings.scale, DynamicBanner.Y, this.statsTimer * 1.0f / 0.5f);
            for (GameOverStat i : this.stats) {
                i.update();
            }
            if (this.statAnimateTimer < 0.0f) {
                boolean allStatsShown = true;
                for (GameOverStat i : this.stats) {
                    if (!i.hidden) continue;
                    i.hidden = false;
                    this.statAnimateTimer = 0.1f;
                    allStatsShown = false;
                    break;
                }
                if (allStatsShown) {
                    this.animateProgressBar();
                }
            } else {
                this.statAnimateTimer -= Gdx.graphics.getDeltaTime();
            }
        }
    }

    public void render(SpriteBatch sb) {
        Iterator<DeathScreenFloatyEffect> i = this.particles.iterator();
        while (i.hasNext()) {
            DeathScreenFloatyEffect e = i.next();
            if (e.renderBehind) {
                e.render(sb);
            }
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
        AbstractDungeon.player.render(sb);
        if (this.monsters != null) {
            this.monsters.render(sb);
        }
        sb.setBlendFunction(770, 1);
        for (DeathScreenFloatyEffect e : this.particles) {
            if (e.renderBehind) continue;
            e.render(sb);
        }
        sb.setBlendFunction(770, 771);
        this.renderStatsScreen(sb);
        if (!this.showingStats && !isVictory) {
            FontHelper.renderFontCentered(sb, FontHelper.topPanelInfoFont, this.deathText, (float)Settings.WIDTH / 2.0f, DEATH_TEXT_Y, this.deathTextColor);
        }
        this.returnButton.render(sb);
    }
}

