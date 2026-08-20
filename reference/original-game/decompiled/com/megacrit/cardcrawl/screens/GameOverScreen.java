/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.ScoreBonusStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.GameOverStat;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.ui.buttons.ReturnToMenuButton;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.UnlockTextEffect;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class GameOverScreen {
    private static final Logger logger = LogManager.getLogger(GameOverScreen.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("DeathScreen");
    private static final String[] TEXT = GameOverScreen.uiStrings.TEXT;
    protected ReturnToMenuButton returnButton;
    public static boolean isVictory;
    protected ArrayList<AbstractUnlock> unlockBundle = null;
    protected ArrayList<GameOverStat> stats = new ArrayList();
    protected Color fadeBgColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    protected Color whiteUiColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    protected Color creamUiColor = Settings.CREAM_COLOR.cpy();
    private float progressBarX = 576.0f * Settings.xScale;
    private float progressBarWidth = 768.0f * Settings.xScale;
    protected boolean showingStats;
    protected float statsTimer = 0.0f;
    protected float statAnimateTimer = 0.0f;
    protected float progressBarTimer = 2.0f;
    protected float progressBarAlpha = 0.0f;
    protected static final float STAT_OFFSET_Y;
    protected static final float STAT_START_Y;
    protected boolean maxLevel = false;
    protected float progressPercent;
    protected float unlockTargetProgress;
    protected float unlockTargetStart;
    protected float unlockProgress;
    protected long playtime;
    protected int score = 0;
    protected int unlockCost;
    protected int nextUnlockCost;
    protected int unlockLevel = 0;
    protected static final float STATS_TRANSITION_TIME = 0.5f;
    protected static final float STAT_ANIM_INTERVAL = 0.1f;
    protected static final float PROGRESS_BAR_ANIM_TIME = 2.0f;
    protected static final ScoreBonusStrings EXORDIUM_ELITE;
    protected static final ScoreBonusStrings CITY_ELITE;
    protected static final ScoreBonusStrings BEYOND_ELITE;
    protected static final ScoreBonusStrings BOSSES_SLAIN;
    protected static final ScoreBonusStrings ASCENSION;
    protected static final ScoreBonusStrings CHAMPION;
    protected static final ScoreBonusStrings PERFECT;
    protected static final ScoreBonusStrings BEYOND_PERFECT;
    protected static final ScoreBonusStrings OVERKILL;
    protected static final ScoreBonusStrings COMBO;
    protected static final ScoreBonusStrings POOPY;
    protected static final ScoreBonusStrings SPEEDSTER;
    protected static final ScoreBonusStrings LIGHT_SPEED;
    protected static final ScoreBonusStrings MONEY_MONEY;
    protected static final ScoreBonusStrings RAINING_MONEY;
    protected static final ScoreBonusStrings I_LIKE_GOLD;
    protected static final ScoreBonusStrings HIGHLANDER;
    protected static final ScoreBonusStrings SHINY;
    protected static final ScoreBonusStrings COLLECTOR;
    protected static final ScoreBonusStrings PAUPER;
    protected static final ScoreBonusStrings LIBRARIAN;
    protected static final ScoreBonusStrings ENCYCLOPEDIAN;
    protected static final ScoreBonusStrings WELL_FED;
    protected static final ScoreBonusStrings STUFFED;
    protected static final ScoreBonusStrings CURSES;
    protected static final ScoreBonusStrings MYSTERY_MACHINE;
    protected static final ScoreBonusStrings ON_MY_OWN_TERMS;
    protected static final ScoreBonusStrings HEARTBREAKER;
    protected static boolean IS_POOPY;
    protected static boolean IS_SPEEDSTER;
    protected static boolean IS_LIGHT_SPEED;
    protected static boolean IS_HIGHLANDER;
    protected static int IS_FULL_SET;
    protected static boolean IS_SHINY;
    protected static boolean IS_PAUPER;
    protected static boolean IS_LIBRARY;
    protected static boolean IS_ENCYCLOPEDIA;
    protected static boolean IS_WELL_FED;
    protected static boolean IS_STUFFED;
    protected static boolean IS_CURSES;
    protected static boolean IS_ON_MY_OWN;
    protected static boolean IS_MONEY_MONEY;
    protected static boolean IS_RAINING_MONEY;
    protected static boolean IS_I_LIKE_GOLD;
    protected static boolean IS_MYSTERY_MACHINE;
    protected static final int POOPY_SCORE = -1;
    protected static final int SPEEDER_SCORE = 25;
    protected static final int LIGHT_SPEED_SCORE = 50;
    protected static final int HIGHLANDER_SCORE = 100;
    protected static final int FULL_SET_SCORE = 25;
    protected static final int SHINY_SCORE = 50;
    protected static final int PAUPER_SCORE = 50;
    protected static final int LIBRARY_SCORE = 25;
    protected static final int ENCYCLOPEDIA_SCORE = 50;
    protected static final int WELL_FED_SCORE = 25;
    protected static final int STUFFED_SCORE = 50;
    protected static final int CURSES_SCORE = 100;
    protected static final int ON_MY_OWN_SCORE = 50;
    protected static final int MONEY_MONEY_SCORE = 25;
    protected static final int RAINING_MONEY_SCORE = 50;
    protected static final int I_LIKE_GOLD_SCORE = 75;
    protected static final int CHAMPION_SCORE = 25;
    protected static final int PERFECT_SCORE = 50;
    protected static final int BEYOND_PERFECT_SCORE = 200;
    protected static final int OVERKILL_SCORE = 25;
    protected static final int COMBO_SCORE = 25;
    protected static final int MYSTERY_MACHINE_SCORE = 25;
    protected static final int HEARTBREAKER_SCORE = 250;
    protected static int floorPoints;
    protected static int monsterPoints;
    protected static int elite1Points;
    protected static int elite2Points;
    protected static int elite3Points;
    protected static int bossPoints;
    protected static int ascensionPoints;
    protected static final int FLOOR_MULTIPLIER = 5;
    protected static final int ENEMY_MULTIPLIER = 2;
    protected static final int ELITE_MULTIPLIER_1 = 10;
    protected static final int ELITE_MULTIPLIER_2 = 20;
    protected static final int ELITE_MULTIPLIER_3 = 30;
    protected static final int BOSS_MULTIPLIER = 50;
    protected static final float ASCENSION_MULTIPLIER = 0.05f;
    protected boolean playedWhir = false;
    protected long whirId;

    public static void resetScoreChecks() {
        IS_POOPY = false;
        IS_SPEEDSTER = false;
        IS_LIGHT_SPEED = false;
        IS_HIGHLANDER = false;
        IS_FULL_SET = 0;
        IS_SHINY = false;
        IS_PAUPER = false;
        IS_LIBRARY = false;
        IS_ENCYCLOPEDIA = false;
        IS_WELL_FED = false;
        IS_STUFFED = false;
        IS_CURSES = false;
        IS_ON_MY_OWN = false;
        IS_MONEY_MONEY = false;
        IS_RAINING_MONEY = false;
        IS_I_LIKE_GOLD = false;
        IS_MYSTERY_MACHINE = false;
    }

    protected void animateProgressBar() {
        if (this.maxLevel) {
            return;
        }
        this.progressBarTimer -= Gdx.graphics.getDeltaTime();
        if (this.progressBarTimer < 0.0f) {
            this.progressBarTimer = 0.0f;
        }
        if (this.progressBarTimer > 2.0f) {
            return;
        }
        if (!this.playedWhir) {
            this.playedWhir = true;
            this.whirId = CardCrawlGame.sound.play("UNLOCK_WHIR");
        }
        this.unlockProgress = Interpolation.pow2In.apply(this.unlockTargetProgress, this.unlockTargetStart, this.progressBarTimer / 2.0f);
        if (this.unlockProgress >= (float)this.unlockCost && this.unlockLevel != 5) {
            if (this.unlockLevel == 4) {
                this.unlockProgress = this.unlockCost;
                ++this.unlockLevel;
                AbstractDungeon.topLevelEffects.add(new UnlockTextEffect());
            } else {
                this.unlockTargetProgress = (float)this.score - ((float)this.unlockCost - this.unlockTargetStart);
                this.progressBarTimer = 3.0f;
                AbstractDungeon.topLevelEffects.add(new UnlockTextEffect());
                CardCrawlGame.sound.stop("UNLOCK_WHIR", this.whirId);
                this.playedWhir = false;
                this.unlockProgress = 0.0f;
                this.unlockTargetStart = 0.0f;
                if (this.unlockTargetProgress > (float)(this.nextUnlockCost - 1)) {
                    this.unlockTargetProgress = this.nextUnlockCost - 1;
                }
                this.unlockCost = this.nextUnlockCost;
                ++this.unlockLevel;
            }
        }
        this.progressPercent = this.unlockProgress / (float)this.unlockCost;
    }

    protected void calculateUnlockProgress() {
        this.score = GameOverScreen.calcScore(isVictory);
        this.unlockLevel = UnlockTracker.getUnlockLevel(AbstractDungeon.player.chosenClass);
        if (this.unlockLevel >= 5) {
            this.maxLevel = true;
            return;
        }
        if (this.score == 0) {
            this.playedWhir = true;
        }
        this.unlockTargetStart = this.unlockProgress = (float)UnlockTracker.getCurrentProgress(AbstractDungeon.player.chosenClass);
        this.unlockCost = UnlockTracker.getCurrentScoreCost(AbstractDungeon.player.chosenClass);
        this.unlockTargetProgress = this.unlockProgress + (float)this.score;
        this.nextUnlockCost = UnlockTracker.incrementUnlockRamp(this.unlockCost);
        if (this.unlockTargetProgress >= (float)this.unlockCost) {
            this.unlockBundle = UnlockTracker.getUnlockBundle(AbstractDungeon.player.chosenClass, this.unlockLevel);
            if (this.unlockLevel == 4) {
                this.unlockTargetProgress = this.unlockCost;
            } else if (this.unlockTargetProgress > (float)this.unlockCost - this.unlockProgress + (float)this.nextUnlockCost - 1.0f) {
                this.unlockTargetProgress = (float)this.unlockCost - this.unlockProgress + (float)this.nextUnlockCost - 1.0f;
            }
        }
        logger.info("SCOR: " + this.score);
        logger.info("PROG: " + this.unlockProgress);
        logger.info("STRT: " + this.unlockTargetStart);
        logger.info("TRGT: " + this.unlockTargetProgress);
        logger.info("COST: " + this.unlockCost);
        UnlockTracker.addScore(AbstractDungeon.player.chosenClass, this.score);
        this.progressPercent = this.unlockTargetStart / (float)this.unlockCost;
    }

    public static boolean shouldUploadMetricData() {
        return Settings.UPLOAD_DATA && CardCrawlGame.publisherIntegration.isInitialized() && Settings.isStandardRun();
    }

    protected void submitVictoryMetrics() {
    }

    protected boolean canUploadLeaderboards() {
        return !Settings.isModded && !Settings.isTrial && !Settings.seedSet;
    }

    protected void uploadToSteamLeaderboards() {
        if (!this.canUploadLeaderboards()) {
            return;
        }
        this.uploadScoreHelper(AbstractDungeon.player.getLeaderboardCharacterName());
        StatsScreen.updateHighestScore(this.score);
    }

    protected void uploadScoreHelper(String characterString) {
        StringBuilder highScoreString = new StringBuilder();
        StringBuilder fastestWinString = new StringBuilder(characterString);
        if (Settings.isDailyRun) {
            highScoreString.append("DAILY_" + Long.toString(Settings.dailyDate));
            long lastDaily = Settings.dailyPref.getLong("LAST_DAILY", 0L);
            boolean bl = Settings.hasDoneDailyToday = lastDaily == Settings.dailyDate;
            if (Settings.hasDoneDailyToday) {
                logger.info("Player has already done the daily for: " + Settings.dailyDate);
            }
        } else {
            highScoreString.append(characterString);
            highScoreString.append("_HIGH_SCORE");
        }
        fastestWinString.append("_FASTEST_WIN");
        if (Settings.isBeta) {
            highScoreString.append("_BETA");
            fastestWinString.append("_BETA");
        }
        if (Settings.isDailyRun && !Settings.hasDoneDailyToday) {
            logger.info("Uploading score for day: " + Settings.dailyDate + "\nScore is: " + this.score);
            Settings.dailyPref.putLong("LAST_DAILY", Settings.dailyDate);
            Settings.dailyPref.flush();
            CardCrawlGame.publisherIntegration.uploadDailyLeaderboardScore(highScoreString.toString(), this.score);
        } else if (!Settings.isDailyRun) {
            CardCrawlGame.publisherIntegration.uploadLeaderboardScore(highScoreString.toString(), this.score);
        }
        if (isVictory && this.playtime < 18000L && this.playtime > 280L && !Settings.isDailyRun) {
            CardCrawlGame.publisherIntegration.uploadLeaderboardScore(fastestWinString.toString(), Math.toIntExact(this.playtime));
        }
    }

    public static int calcScore(boolean victory) {
        floorPoints = 0;
        monsterPoints = 0;
        elite1Points = 0;
        elite2Points = 0;
        elite3Points = 0;
        bossPoints = 0;
        ascensionPoints = 0;
        int tmp = AbstractDungeon.floorNum * 5;
        floorPoints = AbstractDungeon.floorNum * 5;
        monsterPoints = CardCrawlGame.monstersSlain * 2;
        elite1Points = CardCrawlGame.elites1Slain * 10;
        elite2Points = CardCrawlGame.elites2Slain * 20;
        elite3Points = CardCrawlGame.elites3Slain * 30;
        bossPoints = 0;
        int bossMultiplier = 50;
        for (int i = 0; i < AbstractDungeon.bossCount; ++i) {
            bossPoints += bossMultiplier;
            bossMultiplier += 50;
        }
        tmp += monsterPoints;
        tmp += elite1Points;
        tmp += elite2Points;
        tmp += elite3Points;
        tmp += bossPoints;
        tmp += CardCrawlGame.champion * 25;
        tmp = CardCrawlGame.perfect >= 3 ? (tmp += 200) : (tmp += CardCrawlGame.perfect * 50);
        if (CardCrawlGame.overkill) {
            tmp += 25;
        }
        if (CardCrawlGame.combo) {
            tmp += 25;
        }
        if (AbstractDungeon.isAscensionMode) {
            ascensionPoints = MathUtils.round((float)tmp * (0.05f * (float)AbstractDungeon.ascensionLevel));
            tmp += ascensionPoints;
        }
        return tmp += GameOverScreen.checkScoreBonus(victory);
    }

    protected static int checkScoreBonus(boolean victory) {
        int points = 0;
        if (AbstractDungeon.player.hasRelic("Spirit Poop")) {
            IS_POOPY = true;
            --points;
        }
        if ((IS_FULL_SET = AbstractDungeon.player.masterDeck.fullSetCheck()) > 0) {
            points += 25 * IS_FULL_SET;
        }
        if (AbstractDungeon.player.relics.size() >= 25) {
            IS_SHINY = true;
            points += 50;
        }
        if (AbstractDungeon.player.masterDeck.size() >= 50) {
            IS_ENCYCLOPEDIA = true;
            points += 50;
        } else if (AbstractDungeon.player.masterDeck.size() >= 35) {
            IS_LIBRARY = true;
            points += 25;
        }
        int tmpDiff = AbstractDungeon.player.maxHealth - AbstractDungeon.player.startingMaxHP;
        if (tmpDiff >= 30) {
            IS_STUFFED = true;
            points += 50;
        } else if (tmpDiff >= 15) {
            IS_WELL_FED = true;
            points += 25;
        }
        if (AbstractDungeon.player.masterDeck.cursedCheck()) {
            IS_CURSES = true;
            points += 100;
        }
        if (CardCrawlGame.goldGained >= 3000) {
            IS_I_LIKE_GOLD = true;
            points += 75;
        } else if (CardCrawlGame.goldGained >= 2000) {
            IS_RAINING_MONEY = true;
            points += 50;
        } else if (CardCrawlGame.goldGained >= 1000) {
            IS_MONEY_MONEY = true;
            points += 25;
        }
        if (CardCrawlGame.mysteryMachine >= 15) {
            IS_MYSTERY_MACHINE = true;
            points += 25;
        }
        if (victory) {
            logger.info("PLAYTIME: " + CardCrawlGame.playtime);
            if ((long)CardCrawlGame.playtime <= 2700L) {
                IS_LIGHT_SPEED = true;
                points += 50;
            } else if ((long)CardCrawlGame.playtime <= 3600L) {
                IS_SPEEDSTER = true;
                points += 25;
            }
            if (AbstractDungeon.player.masterDeck.highlanderCheck()) {
                IS_HIGHLANDER = true;
                points += 100;
            }
            if (AbstractDungeon.player.masterDeck.pauperCheck()) {
                IS_PAUPER = true;
                points += 50;
            }
            if (isVictory && CardCrawlGame.dungeon instanceof TheEnding) {
                points += 250;
            }
        }
        return points;
    }

    protected void renderStatsScreen(SpriteBatch sb) {
        if (this.showingStats) {
            this.fadeBgColor.a = (1.0f - this.statsTimer) * 0.6f;
            sb.setColor(this.fadeBgColor);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
            float y = STAT_START_Y + (float)this.stats.size() * STAT_OFFSET_Y / 2.0f;
            if (this.stats.size() >= 10) {
                y = STAT_START_Y + (float)(this.stats.size() / 2) * STAT_OFFSET_Y / 2.0f;
            }
            for (int i = 0; i < this.stats.size(); ++i) {
                if (this.stats.size() <= 10) {
                    if (i == this.stats.size() - 2) {
                        this.stats.get(i).renderLine(sb, false, y);
                    } else {
                        this.stats.get(i).render(sb, (float)Settings.WIDTH / 2.0f - 220.0f * Settings.scale, y);
                    }
                } else if (i != this.stats.size() - 1) {
                    if (i < (this.stats.size() - 1) / 2) {
                        this.stats.get(i).render(sb, 440.0f * Settings.xScale, y);
                    } else {
                        this.stats.get(i).render(sb, 1050.0f * Settings.xScale, y + STAT_OFFSET_Y * (float)((this.stats.size() - 1) / 2));
                    }
                } else {
                    this.stats.get(i).renderLine(sb, true, y + STAT_OFFSET_Y * (float)(this.stats.size() / 2));
                    this.stats.get(i).render(sb, 740.0f * Settings.xScale, y + STAT_OFFSET_Y * (float)(this.stats.size() / 2 - 1));
                }
                y -= STAT_OFFSET_Y;
            }
            this.renderProgressBar(sb);
        }
    }

    protected void renderProgressBar(SpriteBatch sb) {
        if (this.maxLevel) {
            return;
        }
        this.whiteUiColor.a = this.progressBarAlpha * 0.3f;
        sb.setColor(this.whiteUiColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.progressBarX, (float)Settings.HEIGHT * 0.2f, this.progressBarWidth, 14.0f * Settings.scale);
        sb.setColor(new Color(1.0f, 0.8f, 0.3f, this.progressBarAlpha * 0.9f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.progressBarX, (float)Settings.HEIGHT * 0.2f, this.progressBarWidth * this.progressPercent, 14.0f * Settings.scale);
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.progressBarAlpha * 0.25f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.progressBarX, (float)Settings.HEIGHT * 0.2f, this.progressBarWidth * this.progressPercent, 4.0f * Settings.scale);
        String derp = "[" + (int)this.unlockProgress + "/" + this.unlockCost + "]";
        this.creamUiColor.a = this.progressBarAlpha * 0.9f;
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, derp, 576.0f * Settings.xScale, (float)Settings.HEIGHT * 0.2f - 12.0f * Settings.scale, this.creamUiColor);
        derp = 5 - this.unlockLevel == 1 ? TEXT[42] + (5 - this.unlockLevel) : TEXT[41] + (5 - this.unlockLevel);
        FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont, derp, 1344.0f * Settings.xScale, (float)Settings.HEIGHT * 0.2f - 12.0f * Settings.scale, this.creamUiColor);
    }

    static {
        STAT_OFFSET_Y = 36.0f * Settings.scale;
        STAT_START_Y = (float)Settings.HEIGHT / 2.0f - 20.0f * Settings.scale;
        EXORDIUM_ELITE = CardCrawlGame.languagePack.getScoreString("Exordium Elites Killed");
        CITY_ELITE = CardCrawlGame.languagePack.getScoreString("City Elites Killed");
        BEYOND_ELITE = CardCrawlGame.languagePack.getScoreString("Beyond Elites Killed");
        BOSSES_SLAIN = CardCrawlGame.languagePack.getScoreString("Bosses Slain");
        ASCENSION = CardCrawlGame.languagePack.getScoreString("Ascension");
        CHAMPION = CardCrawlGame.languagePack.getScoreString("Champion");
        PERFECT = CardCrawlGame.languagePack.getScoreString("Perfect");
        BEYOND_PERFECT = CardCrawlGame.languagePack.getScoreString("Beyond Perfect");
        OVERKILL = CardCrawlGame.languagePack.getScoreString("Overkill");
        COMBO = CardCrawlGame.languagePack.getScoreString("Combo");
        POOPY = CardCrawlGame.languagePack.getScoreString("Poopy");
        SPEEDSTER = CardCrawlGame.languagePack.getScoreString("Speedster");
        LIGHT_SPEED = CardCrawlGame.languagePack.getScoreString("Light Speed");
        MONEY_MONEY = CardCrawlGame.languagePack.getScoreString("Money Money");
        RAINING_MONEY = CardCrawlGame.languagePack.getScoreString("Raining Money");
        I_LIKE_GOLD = CardCrawlGame.languagePack.getScoreString("I Like Gold");
        HIGHLANDER = CardCrawlGame.languagePack.getScoreString("Highlander");
        SHINY = CardCrawlGame.languagePack.getScoreString("Shiny");
        COLLECTOR = CardCrawlGame.languagePack.getScoreString("Collector");
        PAUPER = CardCrawlGame.languagePack.getScoreString("Pauper");
        LIBRARIAN = CardCrawlGame.languagePack.getScoreString("Librarian");
        ENCYCLOPEDIAN = CardCrawlGame.languagePack.getScoreString("Encyclopedian");
        WELL_FED = CardCrawlGame.languagePack.getScoreString("Well Fed");
        STUFFED = CardCrawlGame.languagePack.getScoreString("Stuffed");
        CURSES = CardCrawlGame.languagePack.getScoreString("Curses");
        MYSTERY_MACHINE = CardCrawlGame.languagePack.getScoreString("Mystery Machine");
        ON_MY_OWN_TERMS = CardCrawlGame.languagePack.getScoreString("On My Own Terms");
        HEARTBREAKER = CardCrawlGame.languagePack.getScoreString("Heartbreaker");
        IS_POOPY = false;
        IS_SPEEDSTER = false;
        IS_LIGHT_SPEED = false;
        IS_HIGHLANDER = false;
        IS_FULL_SET = 0;
        IS_SHINY = false;
        IS_PAUPER = false;
        IS_LIBRARY = false;
        IS_ENCYCLOPEDIA = false;
        IS_WELL_FED = false;
        IS_STUFFED = false;
        IS_CURSES = false;
        IS_ON_MY_OWN = false;
        IS_MONEY_MONEY = false;
        IS_RAINING_MONEY = false;
        IS_I_LIKE_GOLD = false;
        IS_MYSTERY_MACHINE = false;
    }
}

