/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.stats;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.localization.AchievementStrings;
import com.megacrit.cardcrawl.screens.stats.RunData;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.io.File;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CharStat {
    private static final Logger logger = LogManager.getLogger(CharStat.class.getName());
    private static final AchievementStrings achievementStrings = CardCrawlGame.languagePack.getAchievementString("CharStat");
    public static final String[] NAMES = CharStat.achievementStrings.NAMES;
    public static final String[] TEXT = CharStat.achievementStrings.TEXT;
    private Prefs pref;
    private String info;
    private String info2;
    private static Gson gson = new Gson();
    private int cardsUnlocked;
    private int relicsUnlocked;
    private int cardsDiscovered;
    private int cardsToDiscover;
    public int furthestAscent;
    public int highestScore;
    public int highestDaily;
    private int totalFloorsClimbed;
    private int numVictory;
    private int numDeath;
    public int winStreak;
    public int bestWinStreak;
    public int enemyKilled;
    public int bossKilled;
    public long playTime;
    public long fastestTime;
    private ArrayList<RunData> runs = new ArrayList();
    public static final String CARD_UNLOCK = "CARD_UNLOCK";
    public static final String RELIC_UNLOCK = "RELIC_UNLOCK";
    public static final String HIGHEST_FLOOR = "HIGHEST_FLOOR";
    public static final String HIGHEST_SCORE = "HIGHEST_SCORE";
    public static final String HIGHEST_DAILY = "HIGHEST_DAILY";
    public static final String TOTAL_FLOORS = "TOTAL_FLOORS";
    public static final String TOTAL_CRYSTALS_FED = "TOTAL_CRYSTALS_FED";
    public static final String WIN_COUNT = "WIN_COUNT";
    public static final String LOSE_COUNT = "LOSE_COUNT";
    public static final String WIN_STREAK = "WIN_STREAK";
    public static final String BEST_WIN_STREAK = "BEST_WIN_STREAK";
    public static final String ASCENSION_LEVEL = "ASCENSION_LEVEL";
    public static final String LAST_ASCENSION_LEVEL = "LAST_ASCENSION_LEVEL";
    public static final String ENEMY_KILL = "ENEMY_KILL";
    public static final String BOSS_KILL = "BOSS_KILL";
    public static final String PLAYTIME = "PLAYTIME";
    public static final String FASTEST_VICTORY = "FAST_VICTORY";

    public CharStat(ArrayList<CharStat> allChars) {
        this.cardsUnlocked = 0;
        this.relicsUnlocked = 0;
        this.furthestAscent = 0;
        this.highestScore = 0;
        this.totalFloorsClimbed = 0;
        this.numVictory = 0;
        this.numDeath = 0;
        this.enemyKilled = 0;
        this.bossKilled = 0;
        this.playTime = 0L;
        this.fastestTime = 999999999999L;
        int highestFloorTmp = 0;
        int highestDailyTmp = 0;
        for (CharStat stat : allChars) {
            this.cardsUnlocked += stat.cardsUnlocked;
            this.relicsUnlocked += stat.relicsUnlocked;
            if (stat.furthestAscent > highestFloorTmp) {
                highestFloorTmp = this.furthestAscent = stat.furthestAscent;
            }
            if (stat.highestDaily > highestDailyTmp) {
                highestDailyTmp = this.highestDaily = stat.highestDaily;
            }
            if (stat.fastestTime < this.fastestTime && stat.fastestTime != 0L) {
                this.fastestTime = stat.fastestTime;
            }
            this.totalFloorsClimbed += stat.totalFloorsClimbed;
            this.numVictory += stat.numVictory;
            this.numDeath += stat.numDeath;
            this.enemyKilled += stat.enemyKilled;
            this.bossKilled += stat.bossKilled;
            this.playTime += stat.playTime;
        }
        this.info = TEXT[0] + CharStat.formatHMSM(this.playTime) + " NL ";
        this.info = this.info + TEXT[1] + this.numVictory + " NL ";
        this.info = this.info + TEXT[2] + this.numDeath + " NL ";
        this.info = this.info + TEXT[3] + this.totalFloorsClimbed + " NL ";
        this.info = this.info + TEXT[4] + this.bossKilled + " NL ";
        this.info = this.info + TEXT[5] + this.enemyKilled + " NL ";
        this.info2 = TEXT[7] + UnlockTracker.getCardsSeenString() + " NL ";
        int unlockedCardCount = UnlockTracker.unlockedRedCardCount + UnlockTracker.unlockedGreenCardCount + UnlockTracker.unlockedBlueCardCount + UnlockTracker.unlockedPurpleCardCount;
        int lockedCardCount = UnlockTracker.lockedRedCardCount + UnlockTracker.lockedGreenCardCount + UnlockTracker.lockedBlueCardCount + UnlockTracker.lockedPurpleCardCount;
        this.info2 = this.info2 + TEXT[8] + unlockedCardCount + "/" + lockedCardCount + " NL ";
        this.info2 = this.info2 + TEXT[9] + UnlockTracker.getRelicsSeenString() + " NL ";
        this.info2 = this.info2 + TEXT[10] + UnlockTracker.unlockedRelicCount + "/" + UnlockTracker.lockedRelicCount + " NL ";
        if (this.fastestTime != 999999999999L) {
            this.info2 = this.info2 + TEXT[13] + CharStat.formatHMSM(this.fastestTime) + " NL ";
        }
    }

    public CharStat(AbstractPlayer c) {
        FileHandle[] files;
        this.pref = c.getPrefs();
        this.cardsUnlocked = this.calculateCardsUnlocked(c);
        this.cardsDiscovered = this.getSeenCardCount(c);
        this.cardsToDiscover = this.getCardCountForChar(c);
        this.relicsUnlocked = this.pref.getInteger(RELIC_UNLOCK, 0);
        this.furthestAscent = this.pref.getInteger(HIGHEST_FLOOR, 0);
        this.highestDaily = this.pref.getInteger(HIGHEST_DAILY, 0);
        this.totalFloorsClimbed = this.pref.getInteger(TOTAL_FLOORS, 0);
        this.numVictory = this.pref.getInteger(WIN_COUNT, 0);
        this.numDeath = this.pref.getInteger(LOSE_COUNT, 0);
        this.winStreak = this.pref.getInteger(WIN_STREAK, 0);
        this.bestWinStreak = this.pref.getInteger(BEST_WIN_STREAK, 0);
        this.enemyKilled = this.pref.getInteger(ENEMY_KILL, 0);
        this.bossKilled = this.pref.getInteger(BOSS_KILL, 0);
        this.playTime = this.pref.getLong(PLAYTIME, 0L);
        this.fastestTime = this.pref.getLong(FASTEST_VICTORY, 0L);
        this.highestScore = this.pref.getInteger(HIGHEST_SCORE, 0);
        this.info = TEXT[0] + CharStat.formatHMSM(this.playTime) + " NL ";
        this.info = this.info + TEXT[7] + this.cardsDiscovered + "/" + this.cardsToDiscover + " NL ";
        this.info = this.info + TEXT[8] + this.cardsUnlocked + "/" + UnlockTracker.lockedRedCardCount + " NL ";
        if (this.fastestTime != 0L) {
            this.info = this.info + TEXT[13] + CharStat.formatHMSM(this.fastestTime) + " NL ";
        }
        this.info = this.info + TEXT[23] + this.highestScore + " NL ";
        if (this.bestWinStreak > 0) {
            this.info = this.info + TEXT[22] + this.bestWinStreak + " NL ";
        }
        this.info2 = TEXT[17] + this.numVictory + " NL ";
        this.info2 = this.info2 + TEXT[18] + this.numDeath + " NL ";
        this.info2 = this.info2 + TEXT[19] + this.totalFloorsClimbed + " NL ";
        this.info2 = this.info2 + TEXT[20] + this.bossKilled + " NL ";
        this.info2 = this.info2 + TEXT[21] + this.enemyKilled + " NL ";
        StringBuilder sb = new StringBuilder();
        sb.append("runs").append(File.separator);
        if (CardCrawlGame.saveSlot != 0) {
            sb.append(CardCrawlGame.saveSlot).append("_");
        }
        sb.append(c.chosenClass.name()).append(File.separator);
        for (FileHandle file : files = Gdx.files.local(sb.toString()).list()) {
            try {
                this.runs.add(gson.fromJson(file.readString(), RunData.class));
            }
            catch (Exception e) {
                file.delete();
                logger.warn("Deleted corrupt .run file, preventing crash!", (Throwable)e);
            }
        }
    }

    private int calculateCardsUnlocked(AbstractPlayer c) {
        return c.getUnlockedCardCount();
    }

    private int getSeenCardCount(AbstractPlayer c) {
        return c.getSeenCardCount();
    }

    private int getCardCountForChar(AbstractPlayer c) {
        return c.getCardCount();
    }

    public void highestScore(int score) {
        if (score > this.highestScore) {
            this.highestScore = score;
            this.pref.putInteger(HIGHEST_SCORE, this.highestScore);
            this.pref.flush();
        }
    }

    public void furthestAscent(int floor) {
        if (floor > this.furthestAscent) {
            this.furthestAscent = floor;
            this.pref.putInteger(HIGHEST_FLOOR, this.furthestAscent);
            this.pref.flush();
        }
    }

    public void highestDaily(int score) {
        if (score > this.highestDaily) {
            this.highestDaily = score;
            this.pref.putInteger(HIGHEST_DAILY, this.highestDaily);
            this.pref.flush();
        }
    }

    public void incrementFloorClimbed() {
        ++this.totalFloorsClimbed;
        this.pref.putInteger(TOTAL_FLOORS, this.totalFloorsClimbed);
        this.pref.flush();
    }

    public void incrementDeath() {
        ++this.numDeath;
        if (!AbstractDungeon.isAscensionMode) {
            this.winStreak = 0;
            this.pref.putInteger(WIN_STREAK, this.winStreak);
        }
        this.pref.putInteger(LOSE_COUNT, this.numDeath);
        this.pref.flush();
    }

    public int getVictoryCount() {
        return this.numVictory;
    }

    public int getDeathCount() {
        return this.numDeath;
    }

    public void unlockAscension() {
        this.pref.putInteger(ASCENSION_LEVEL, 1);
        this.pref.putInteger(LAST_ASCENSION_LEVEL, 1);
    }

    public void incrementAscension() {
        if (!Settings.isTrial) {
            int derp = this.pref.getInteger(ASCENSION_LEVEL, 1);
            if (derp == AbstractDungeon.ascensionLevel) {
                if (++derp <= 20) {
                    this.pref.putInteger(ASCENSION_LEVEL, derp);
                    this.pref.putInteger(LAST_ASCENSION_LEVEL, derp);
                    this.pref.flush();
                    logger.info("ASCENSION LEVEL IS NOW: " + derp);
                } else {
                    this.pref.putInteger(ASCENSION_LEVEL, 20);
                    this.pref.putInteger(LAST_ASCENSION_LEVEL, 20);
                    this.pref.flush();
                    logger.info("MAX ASCENSION");
                }
            } else {
                logger.info("Played Ascension that wasn't Max");
            }
        }
    }

    public void incrementVictory() {
        ++this.numVictory;
        if (!AbstractDungeon.isAscensionMode) {
            ++this.winStreak;
            this.pref.putInteger(WIN_STREAK, this.winStreak);
            if (this.winStreak > this.pref.getInteger(BEST_WIN_STREAK, 0)) {
                this.pref.putInteger(BEST_WIN_STREAK, this.winStreak);
            }
        }
        this.pref.putInteger(WIN_COUNT, this.numVictory);
        this.pref.flush();
    }

    public void incrementBossSlain() {
        ++this.bossKilled;
        this.pref.putInteger(BOSS_KILL, this.bossKilled);
        this.pref.flush();
    }

    public void incrementEnemySlain() {
        ++this.enemyKilled;
        this.pref.putInteger(ENEMY_KILL, this.enemyKilled);
        this.pref.flush();
    }

    public void incrementPlayTime(long time) {
        this.playTime += time;
        this.pref.putLong(PLAYTIME, this.playTime);
        this.pref.flush();
    }

    public static String formatHMSM(float t) {
        String res = "";
        long duration = (long)t;
        int seconds = (int)(duration % 60L);
        int minutes = (int)((duration /= 60L) % 60L);
        int hours = (int)t / 3600;
        res = hours > 0 ? String.format(TEXT[24], hours, minutes, seconds) : String.format(TEXT[25], minutes, seconds);
        return res;
    }

    public static String formatHMSM(long t) {
        String res = "";
        long duration = t;
        int seconds = (int)(duration % 60L);
        int minutes = (int)((duration /= 60L) % 60L);
        int hours = (int)t / 3600;
        res = hours > 0 ? String.format(TEXT[26], hours, minutes, seconds) : String.format(TEXT[27], minutes, seconds);
        return res;
    }

    public static String formatHMSM(int t) {
        String res = "";
        long duration = t;
        int seconds = (int)(duration % 60L);
        int minutes = (int)((duration /= 60L) % 60L);
        int hours = t / 3600;
        res = String.format(TEXT[28], hours, minutes, seconds);
        return res;
    }

    public void updateFastestVictory(long newTime) {
        if (newTime < this.fastestTime || this.fastestTime == 0L) {
            this.fastestTime = newTime;
            this.pref.putLong(FASTEST_VICTORY, this.fastestTime);
            this.pref.flush();
            logger.info("Fastest victory time updated to: " + this.fastestTime);
        } else {
            logger.info("Did not save fastest victory.");
        }
    }

    public void render(SpriteBatch sb, float screenX, float renderY) {
        FontHelper.renderSmartText(sb, FontHelper.panelNameFont, this.info, screenX + 75.0f * Settings.scale, renderY + 766.0f * Settings.yScale, 9999.0f, 38.0f * Settings.scale, Settings.CREAM_COLOR);
        if (this.info2 != null) {
            FontHelper.renderSmartText(sb, FontHelper.panelNameFont, this.info2, screenX + 675.0f * Settings.scale, renderY + 766.0f * Settings.yScale, 9999.0f, 38.0f * Settings.scale, Settings.CREAM_COLOR);
        }
    }
}

