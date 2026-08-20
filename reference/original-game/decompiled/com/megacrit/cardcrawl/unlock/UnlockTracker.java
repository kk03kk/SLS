/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.screens.stats.AchievementItem;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.EchoFormUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.HyperbeamUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.MeteorStrikeUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.NovaUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.ReboundUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.RecycleUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.SunderUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.TurboUnlock;
import com.megacrit.cardcrawl.unlock.cards.defect.UndoUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.EvolveUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.ExhumeUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.HavocUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.HeavyBladeUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.ImmolateUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.LimitBreakUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.SentinelUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.SpotWeaknessUnlock;
import com.megacrit.cardcrawl.unlock.cards.ironclad.WildStrikeUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.AccuracyUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.BaneUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.CatalystUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.CloakAndDaggerUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.ConcentrateUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.CorpseExplosionUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.GrandFinaleUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.SetupUnlock;
import com.megacrit.cardcrawl.unlock.cards.silent.StormOfSteelUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.AlphaUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.BlasphemyUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.ClarityUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.DevotionUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.ForeignInfluenceUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.ForesightUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.MentalFortressUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.ProstrateUnlock;
import com.megacrit.cardcrawl.unlock.cards.watcher.WishUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.CablesUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.DataDiskUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.EmotionChipUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.RunicCapacitorUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.TurnipUnlock;
import com.megacrit.cardcrawl.unlock.relics.defect.VirusUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.BlueCandleUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.DeadBranchUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.OmamoriUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.PrayerWheelUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.ShovelUnlock;
import com.megacrit.cardcrawl.unlock.relics.ironclad.SingingBowlUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.ArtOfWarUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.CourierUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.DuvuDollUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.PandorasBoxUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.SmilingMaskUnlock;
import com.megacrit.cardcrawl.unlock.relics.silent.TinyChestUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.AkabekoUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.CeramicFishUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.CloakClaspUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.StrikeDummyUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.TeardropUnlock;
import com.megacrit.cardcrawl.unlock.relics.watcher.YangUnlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnlockTracker {
    private static final Logger logger = LogManager.getLogger(UnlockTracker.class.getName());
    public static Prefs unlockPref;
    public static Prefs seenPref;
    public static Prefs betaCardPref;
    public static Prefs bossSeenPref;
    public static Prefs relicSeenPref;
    public static Prefs achievementPref;
    public static Prefs unlockProgress;
    public static HashMap<String, String> unlockReqs;
    public static ArrayList<String> lockedCards;
    public static ArrayList<String> lockedCharacters;
    public static ArrayList<String> lockedLoadouts;
    public static ArrayList<String> lockedRelics;
    public static int lockedRedCardCount;
    public static int unlockedRedCardCount;
    public static int lockedGreenCardCount;
    public static int unlockedGreenCardCount;
    public static int lockedBlueCardCount;
    public static int unlockedBlueCardCount;
    public static int lockedPurpleCardCount;
    public static int unlockedPurpleCardCount;
    public static int lockedRelicCount;
    public static int unlockedRelicCount;
    private static final int STARTING_UNLOCK_COST = 300;

    public static void initialize() {
        achievementPref = SaveHelper.getPrefs("STSAchievements");
        unlockPref = SaveHelper.getPrefs("STSUnlocks");
        unlockProgress = SaveHelper.getPrefs("STSUnlockProgress");
        seenPref = SaveHelper.getPrefs("STSSeenCards");
        betaCardPref = SaveHelper.getPrefs("STSBetaCardPreference");
        bossSeenPref = SaveHelper.getPrefs("STSSeenBosses");
        relicSeenPref = SaveHelper.getPrefs("STSSeenRelics");
        UnlockTracker.refresh();
    }

    public static void retroactiveUnlock() {
        ArrayList<String> cardKeys = new ArrayList<String>();
        ArrayList<String> relicKeys = new ArrayList<String>();
        ArrayList<AbstractUnlock> bundle = new ArrayList<AbstractUnlock>();
        UnlockTracker.appendRetroactiveUnlockList(AbstractPlayer.PlayerClass.IRONCLAD, unlockProgress.getInteger(AbstractPlayer.PlayerClass.IRONCLAD.toString() + "UnlockLevel", -1), bundle, cardKeys, relicKeys);
        UnlockTracker.appendRetroactiveUnlockList(AbstractPlayer.PlayerClass.THE_SILENT, unlockProgress.getInteger(AbstractPlayer.PlayerClass.THE_SILENT.toString() + "UnlockLevel", -1), bundle, cardKeys, relicKeys);
        UnlockTracker.appendRetroactiveUnlockList(AbstractPlayer.PlayerClass.DEFECT, unlockProgress.getInteger(AbstractPlayer.PlayerClass.DEFECT.toString() + "UnlockLevel", -1), bundle, cardKeys, relicKeys);
        UnlockTracker.appendRetroactiveUnlockList(AbstractPlayer.PlayerClass.WATCHER, unlockProgress.getInteger(AbstractPlayer.PlayerClass.WATCHER.toString() + "UnlockLevel", -1), bundle, cardKeys, relicKeys);
        boolean changed = false;
        for (String k : cardKeys) {
            if (unlockPref.getInteger(k) == 2) continue;
            unlockPref.putInteger(k, 2);
            changed = true;
            logger.info("RETROACTIVE CARD UNLOCK:  " + k);
        }
        for (String k : relicKeys) {
            if (unlockPref.getInteger(k) == 2) continue;
            unlockPref.putInteger(k, 2);
            changed = true;
            logger.info("RETROACTIVE RELIC UNLOCK: " + k);
        }
        if (UnlockTracker.isCharacterLocked("Watcher") && !UnlockTracker.isCharacterLocked("Defect") && (UnlockTracker.isAchievementUnlocked("RUBY") || UnlockTracker.isAchievementUnlocked("EMERALD") || UnlockTracker.isAchievementUnlocked("SAPPHIRE"))) {
            unlockPref.putInteger("Watcher", 2);
            lockedCharacters.remove("Watcher");
            changed = true;
        }
        if (changed) {
            logger.info("RETRO UNLOCKED, SAVING");
            unlockPref.flush();
        }
    }

    private static void appendRetroactiveUnlockList(AbstractPlayer.PlayerClass c, int lvl, ArrayList<AbstractUnlock> bundle, ArrayList<String> cardKeys, ArrayList<String> relicKeys) {
        while (lvl > 0) {
            bundle = UnlockTracker.getUnlockBundle(c, lvl - 1);
            for (AbstractUnlock u : bundle) {
                if (u.type == AbstractUnlock.UnlockType.RELIC) {
                    logger.info(u.key + " should be unlocked.");
                    relicKeys.add(u.key);
                    continue;
                }
                if (u.type != AbstractUnlock.UnlockType.CARD) continue;
                logger.info(u.key + " should be unlocked.");
                cardKeys.add(u.key);
            }
            --lvl;
        }
    }

    public static void refresh() {
        lockedCards.clear();
        lockedCharacters.clear();
        lockedLoadouts.clear();
        lockedRelics.clear();
        UnlockTracker.addCard("Havoc");
        UnlockTracker.addCard("Sentinel");
        UnlockTracker.addCard("Exhume");
        UnlockTracker.addCard("Wild Strike");
        UnlockTracker.addCard("Evolve");
        UnlockTracker.addCard("Immolate");
        UnlockTracker.addCard("Heavy Blade");
        UnlockTracker.addCard("Spot Weakness");
        UnlockTracker.addCard("Limit Break");
        UnlockTracker.addCard("Concentrate");
        UnlockTracker.addCard("Setup");
        UnlockTracker.addCard("Grand Finale");
        UnlockTracker.addCard("Cloak And Dagger");
        UnlockTracker.addCard("Accuracy");
        UnlockTracker.addCard("Storm of Steel");
        UnlockTracker.addCard("Bane");
        UnlockTracker.addCard("Catalyst");
        UnlockTracker.addCard("Corpse Explosion");
        UnlockTracker.addCard("Rebound");
        UnlockTracker.addCard("Undo");
        UnlockTracker.addCard("Echo Form");
        UnlockTracker.addCard("Turbo");
        UnlockTracker.addCard("Sunder");
        UnlockTracker.addCard("Meteor Strike");
        UnlockTracker.addCard("Hyperbeam");
        UnlockTracker.addCard("Recycle");
        UnlockTracker.addCard("Core Surge");
        UnlockTracker.addCard("Prostrate");
        UnlockTracker.addCard("Blasphemy");
        UnlockTracker.addCard("Devotion");
        UnlockTracker.addCard("ForeignInfluence");
        UnlockTracker.addCard("Alpha");
        UnlockTracker.addCard("MentalFortress");
        UnlockTracker.addCard("SpiritShield");
        UnlockTracker.addCard("Wish");
        UnlockTracker.addCard("Wireheading");
        UnlockTracker.addCharacter("The Silent");
        UnlockTracker.addCharacter("Defect");
        UnlockTracker.addCharacter("Watcher");
        UnlockTracker.addRelic("Omamori");
        UnlockTracker.addRelic("Prayer Wheel");
        UnlockTracker.addRelic("Shovel");
        UnlockTracker.addRelic("Art of War");
        UnlockTracker.addRelic("The Courier");
        UnlockTracker.addRelic("Pandora's Box");
        UnlockTracker.addRelic("Blue Candle");
        UnlockTracker.addRelic("Dead Branch");
        UnlockTracker.addRelic("Singing Bowl");
        UnlockTracker.addRelic("Du-Vu Doll");
        UnlockTracker.addRelic("Smiling Mask");
        UnlockTracker.addRelic("Tiny Chest");
        UnlockTracker.addRelic("Cables");
        UnlockTracker.addRelic("DataDisk");
        UnlockTracker.addRelic("Emotion Chip");
        UnlockTracker.addRelic("Runic Capacitor");
        UnlockTracker.addRelic("Turnip");
        UnlockTracker.addRelic("Symbiotic Virus");
        UnlockTracker.addRelic("Akabeko");
        UnlockTracker.addRelic("Yang");
        UnlockTracker.addRelic("CeramicFish");
        UnlockTracker.addRelic("StrikeDummy");
        UnlockTracker.addRelic("TeardropLocket");
        UnlockTracker.addRelic("CloakClasp");
        UnlockTracker.countUnlockedCards();
    }

    public static int incrementUnlockRamp(int currentCost) {
        switch (currentCost) {
            case 300: {
                return 750;
            }
            case 500: {
                return 1000;
            }
            case 750: {
                return 1000;
            }
            case 1000: {
                return 1500;
            }
            case 1500: {
                return 2000;
            }
            case 2000: {
                return 2500;
            }
            case 2500: {
                return 3000;
            }
            case 3000: {
                return 3000;
            }
            case 4000: {
                return 4000;
            }
        }
        return currentCost + 250;
    }

    public static void resetUnlockProgress(AbstractPlayer.PlayerClass c) {
        unlockProgress.putInteger(c.toString() + "UnlockLevel", 0);
        unlockProgress.putInteger(c.toString() + "Progress", 0);
        unlockProgress.putInteger(c.toString() + "CurrentCost", 300);
        unlockProgress.putInteger(c.toString() + "TotalScore", 0);
        unlockProgress.putInteger(c.toString() + "HighScore", 0);
    }

    public static int getUnlockLevel(AbstractPlayer.PlayerClass c) {
        return unlockProgress.getInteger(c.toString() + "UnlockLevel", 0);
    }

    public static int getCurrentProgress(AbstractPlayer.PlayerClass c) {
        return unlockProgress.getInteger(c.toString() + "Progress", 0);
    }

    public static int getCurrentScoreCost(AbstractPlayer.PlayerClass c) {
        return unlockProgress.getInteger(c.toString() + "CurrentCost", 300);
    }

    public static void addScore(AbstractPlayer.PlayerClass c, int scoreGained) {
        String key_unlock_level = c.toString() + "UnlockLevel";
        String key_progress = c.toString() + "Progress";
        String key_current_cost = c.toString() + "CurrentCost";
        String key_total_score = c.toString() + "TotalScore";
        String key_high_score = c.toString() + "HighScore";
        logger.info("Keys");
        logger.info(key_unlock_level);
        logger.info(key_progress);
        logger.info(key_current_cost);
        logger.info(key_total_score);
        logger.info(key_high_score);
        int p = unlockProgress.getInteger(key_progress, 0);
        if ((p += scoreGained) >= unlockProgress.getInteger(key_current_cost, 300)) {
            logger.info("[DEBUG] Level up!");
            int lvl = unlockProgress.getInteger(key_unlock_level, 0);
            unlockProgress.putInteger(key_unlock_level, ++lvl);
            unlockProgress.putInteger(key_progress, p -= unlockProgress.getInteger(key_current_cost, 300));
            logger.info("[DEBUG] Score Progress: " + key_progress);
            int current_cost = unlockProgress.getInteger(key_current_cost, 300);
            unlockProgress.putInteger(key_current_cost, UnlockTracker.incrementUnlockRamp(current_cost));
            if (p > unlockProgress.getInteger(key_current_cost, 300)) {
                unlockProgress.putInteger(key_progress, unlockProgress.getInteger(key_current_cost, 300) - 1);
                logger.info("Overfloat maxes out next level");
            }
        } else {
            unlockProgress.putInteger(key_progress, p);
        }
        int total = unlockProgress.getInteger(key_total_score, 0);
        unlockProgress.putInteger(key_total_score, total += scoreGained);
        logger.info("[DEBUG] Total score: " + total);
        int highscore = unlockProgress.getInteger(key_high_score, 0);
        if (scoreGained > highscore) {
            unlockProgress.putInteger(key_high_score, scoreGained);
            logger.info("[DEBUG] New high score: " + scoreGained);
        }
        unlockProgress.flush();
    }

    public static void countUnlockedCards() {
        ArrayList<String> tmp = new ArrayList<String>();
        int count = 0;
        tmp.add("Havoc");
        tmp.add("Sentinel");
        tmp.add("Exhume");
        tmp.add("Wild Strike");
        tmp.add("Evolve");
        tmp.add("Immolate");
        tmp.add("Heavy Blade");
        tmp.add("Spot Weakness");
        tmp.add("Limit Break");
        for (String s : tmp) {
            if (UnlockTracker.isCardLocked(s)) continue;
            ++count;
        }
        lockedRedCardCount = tmp.size();
        unlockedRedCardCount = count;
        tmp.clear();
        count = 0;
        tmp.add("Concentrate");
        tmp.add("Setup");
        tmp.add("Grand Finale");
        tmp.add("Cloak And Dagger");
        tmp.add("Accuracy");
        tmp.add("Storm of Steel");
        tmp.add("Bane");
        tmp.add("Catalyst");
        tmp.add("Corpse Explosion");
        for (String s : tmp) {
            if (UnlockTracker.isCardLocked(s)) continue;
            ++count;
        }
        lockedGreenCardCount = tmp.size();
        unlockedGreenCardCount = count;
        tmp.clear();
        count = 0;
        tmp.add("Rebound");
        tmp.add("Undo");
        tmp.add("Echo Form");
        tmp.add("Turbo");
        tmp.add("Sunder");
        tmp.add("Meteor Strike");
        tmp.add("Hyperbeam");
        tmp.add("Recycle");
        tmp.add("Core Surge");
        for (String s : tmp) {
            if (UnlockTracker.isCardLocked(s)) continue;
            ++count;
        }
        lockedBlueCardCount = tmp.size();
        unlockedBlueCardCount = count;
        tmp.clear();
        count = 0;
        tmp.add("Prostrate");
        tmp.add("Blasphemy");
        tmp.add("Devotion");
        tmp.add("ForeignInfluence");
        tmp.add("Alpha");
        tmp.add("MentalFortress");
        tmp.add("SpiritShield");
        tmp.add("Wish");
        tmp.add("Wireheading");
        for (String s : tmp) {
            if (UnlockTracker.isCardLocked(s)) continue;
            ++count;
        }
        lockedPurpleCardCount = tmp.size();
        unlockedPurpleCardCount = count;
        tmp.clear();
        count = 0;
        tmp.add("Omamori");
        tmp.add("Prayer Wheel");
        tmp.add("Shovel");
        tmp.add("Art of War");
        tmp.add("The Courier");
        tmp.add("Pandora's Box");
        tmp.add("Blue Candle");
        tmp.add("Dead Branch");
        tmp.add("Singing Bowl");
        tmp.add("Du-Vu Doll");
        tmp.add("Smiling Mask");
        tmp.add("Tiny Chest");
        tmp.add("Cables");
        tmp.add("DataDisk");
        tmp.add("Emotion Chip");
        tmp.add("Runic Capacitor");
        tmp.add("Turnip");
        tmp.add("Symbiotic Virus");
        tmp.add("Akabeko");
        tmp.add("Yang");
        tmp.add("CeramicFish");
        tmp.add("StrikeDummy");
        tmp.add("TeardropLocket");
        tmp.add("CloakClasp");
        for (String s : tmp) {
            if (UnlockTracker.isRelicLocked(s)) continue;
            ++count;
        }
        lockedRelicCount = tmp.size();
        unlockedRelicCount = count;
        logger.info("RED UNLOCKS:   " + unlockedRedCardCount + "/" + lockedRedCardCount);
        logger.info("GREEN UNLOCKS: " + unlockedGreenCardCount + "/" + lockedGreenCardCount);
        logger.info("BLUE UNLOCKS: " + unlockedBlueCardCount + "/" + lockedBlueCardCount);
        logger.info("PURPLE UNLOCKS: " + unlockedPurpleCardCount + "/" + lockedPurpleCardCount);
        logger.info("RELIC UNLOCKS: " + unlockedRelicCount + "/" + lockedRelicCount);
        logger.info("CARDS SEEN:    " + seenPref.get().keySet().size() + "/" + CardLibrary.totalCardCount);
        logger.info("RELICS SEEN:   " + relicSeenPref.get().keySet().size() + "/" + RelicLibrary.totalRelicCount);
    }

    public static String getCardsSeenString() {
        return CardLibrary.seenRedCards + CardLibrary.seenGreenCards + CardLibrary.seenBlueCards + CardLibrary.seenPurpleCards + CardLibrary.seenColorlessCards + CardLibrary.seenCurseCards + "/" + CardLibrary.totalCardCount;
    }

    public static String getRelicsSeenString() {
        return RelicLibrary.seenRelics + "/" + RelicLibrary.totalRelicCount;
    }

    public static void addCard(String key) {
        if (unlockPref.getString(key).equals("true")) {
            unlockPref.putInteger(key, 2);
            logger.info("Converting " + key + " from bool to int");
            unlockPref.flush();
        } else if (unlockPref.getString(key).equals("false")) {
            unlockPref.putInteger(key, 0);
            logger.info("Converting " + key + " from bool to int");
            unlockPref.flush();
        }
        if (unlockPref.getInteger(key, 0) != 2) {
            lockedCards.add(key);
        }
    }

    public static void addCharacter(String key) {
        if (unlockPref.getString(key).equals("true")) {
            unlockPref.putInteger(key, 2);
            logger.info("Converting " + key + " from bool to int");
            unlockPref.flush();
        } else if (unlockPref.getString(key).equals("false")) {
            unlockPref.putInteger(key, 0);
            logger.info("Converting " + key + " from bool to int");
            unlockPref.flush();
        }
        if (unlockPref.getInteger(key, 0) != 2) {
            lockedCharacters.add(key);
        }
    }

    public static void addRelic(String key) {
        if (unlockPref.getInteger(key, 0) != 2) {
            lockedRelics.add(key);
        }
    }

    public static void unlockAchievement(String key) {
        if (Settings.isModded || Settings.isShowBuild || !Settings.isStandardRun()) {
            return;
        }
        CardCrawlGame.publisherIntegration.unlockAchievement(key);
        if (!achievementPref.getBoolean(key, false)) {
            achievementPref.putBoolean(key, true);
            logger.info("Achievement Unlocked: " + key);
        }
        if (UnlockTracker.allAchievementsExceptPlatinumUnlocked() && !UnlockTracker.isAchievementUnlocked("ETERNAL_ONE")) {
            CardCrawlGame.publisherIntegration.unlockAchievement("ETERNAL_ONE");
            achievementPref.putBoolean("ETERNAL_ONE", true);
            logger.info("Achievement Unlocked: ETERNAL_ONE");
        }
        achievementPref.flush();
    }

    public static boolean allAchievementsExceptPlatinumUnlocked() {
        return UnlockTracker.achievementPref.data.entrySet().size() >= 45;
    }

    public static boolean isAchievementUnlocked(String key) {
        return achievementPref.getBoolean(key, false);
    }

    public static void unlockLuckyDay() {
        if (Settings.isModded) {
            return;
        }
        String key = "LUCKY_DAY";
        CardCrawlGame.publisherIntegration.unlockAchievement(key);
        if (!achievementPref.getBoolean(key, false)) {
            achievementPref.putBoolean(key, true);
            achievementPref.flush();
            logger.info("Achievement Unlocked: " + key);
        }
    }

    public static void hardUnlock(String key) {
        if (Settings.isShowBuild) {
            return;
        }
        if (unlockPref.getInteger(key, 0) == 1) {
            unlockPref.putInteger(key, 2);
            unlockPref.flush();
            logger.info("Hard Unlock: " + key);
        }
    }

    public static void hardUnlockOverride(String key) {
        if (Settings.isShowBuild) {
            return;
        }
        unlockPref.putInteger(key, 2);
        unlockPref.flush();
        logger.info("Hard Unlock: " + key);
    }

    public static boolean isCardLocked(String key) {
        return lockedCards.contains(key);
    }

    public static void unlockCard(String key) {
        seenPref.putInteger(key, 1);
        seenPref.flush();
        unlockPref.putInteger(key, 2);
        unlockPref.flush();
        lockedCards.remove(key);
        if (CardLibrary.getCard(key) != null) {
            CardLibrary.getCard((String)key).isSeen = true;
            CardLibrary.getCard(key).unlock();
        }
    }

    public static boolean isCharacterLocked(String key) {
        if (key.equals("The Silent") && Settings.isDemo) {
            return false;
        }
        if (Settings.isAlpha) {
            return false;
        }
        return lockedCharacters.contains(key);
    }

    public static boolean isAscensionUnlocked(AbstractPlayer p) {
        int victories = StatsScreen.getVictory(p.getCharStat());
        if (victories > 0) {
            if (!achievementPref.getBoolean("ASCEND_0", false)) {
                UnlockTracker.unlockAchievement("ASCEND_0");
            }
            if (!achievementPref.getBoolean("ASCEND_10", false)) {
                StatsScreen.retroactiveAscend10Unlock(p.getPrefs());
            }
            if (!achievementPref.getBoolean("ASCEND_20", false)) {
                StatsScreen.retroactiveAscend20Unlock(p.getPrefs());
            }
            return true;
        }
        return false;
    }

    public static boolean isRelicLocked(String key) {
        return lockedRelics.contains(key);
    }

    public static void markCardAsSeen(String key) {
        if (CardLibrary.getCard(key) != null && !CardLibrary.getCard((String)key).isSeen) {
            CardLibrary.getCard((String)key).isSeen = true;
            seenPref.putInteger(key, 1);
            seenPref.flush();
        } else {
            logger.info("Already seen: " + key);
        }
    }

    public static boolean isCardSeen(String key) {
        return seenPref.getInteger(key, 0) != 0;
    }

    public static void markRelicAsSeen(String key) {
        if (RelicLibrary.getRelic(key) != null && !RelicLibrary.getRelic((String)key).isSeen) {
            RelicLibrary.getRelic((String)key).isSeen = true;
            relicSeenPref.putInteger(key, 1);
            relicSeenPref.flush();
        } else if (Settings.isDebug) {
            logger.info("Already seen: " + key);
        }
    }

    public static boolean isRelicSeen(String key) {
        return relicSeenPref.getInteger(key, 0) == 1;
    }

    public static void markBossAsSeen(String originalName) {
        if (bossSeenPref.getInteger(originalName) != 1) {
            bossSeenPref.putInteger(originalName, 1);
            bossSeenPref.flush();
        }
    }

    public static boolean isBossSeen(String key) {
        return bossSeenPref.getInteger(key, 0) == 1;
    }

    public static ArrayList<AbstractUnlock> getUnlockBundle(AbstractPlayer.PlayerClass c, int unlockLevel) {
        ArrayList<AbstractUnlock> tmpBundle = new ArrayList<AbstractUnlock>();
        block0 : switch (c) {
            case IRONCLAD: {
                switch (unlockLevel) {
                    case 0: {
                        tmpBundle.add(new HeavyBladeUnlock());
                        tmpBundle.add(new SpotWeaknessUnlock());
                        tmpBundle.add(new LimitBreakUnlock());
                        break block0;
                    }
                    case 1: {
                        tmpBundle.add(new OmamoriUnlock());
                        tmpBundle.add(new PrayerWheelUnlock());
                        tmpBundle.add(new ShovelUnlock());
                        break block0;
                    }
                    case 2: {
                        tmpBundle.add(new WildStrikeUnlock());
                        tmpBundle.add(new EvolveUnlock());
                        tmpBundle.add(new ImmolateUnlock());
                        break block0;
                    }
                    case 3: {
                        tmpBundle.add(new HavocUnlock());
                        tmpBundle.add(new SentinelUnlock());
                        tmpBundle.add(new ExhumeUnlock());
                        break block0;
                    }
                    case 4: {
                        tmpBundle.add(new BlueCandleUnlock());
                        tmpBundle.add(new DeadBranchUnlock());
                        tmpBundle.add(new SingingBowlUnlock());
                        break block0;
                    }
                }
                break;
            }
            case THE_SILENT: {
                switch (unlockLevel) {
                    case 0: {
                        tmpBundle.add(new BaneUnlock());
                        tmpBundle.add(new CatalystUnlock());
                        tmpBundle.add(new CorpseExplosionUnlock());
                        break block0;
                    }
                    case 1: {
                        tmpBundle.add(new DuvuDollUnlock());
                        tmpBundle.add(new SmilingMaskUnlock());
                        tmpBundle.add(new TinyChestUnlock());
                        break block0;
                    }
                    case 2: {
                        tmpBundle.add(new CloakAndDaggerUnlock());
                        tmpBundle.add(new AccuracyUnlock());
                        tmpBundle.add(new StormOfSteelUnlock());
                        break block0;
                    }
                    case 3: {
                        tmpBundle.add(new ArtOfWarUnlock());
                        tmpBundle.add(new CourierUnlock());
                        tmpBundle.add(new PandorasBoxUnlock());
                        break block0;
                    }
                    case 4: {
                        tmpBundle.add(new ConcentrateUnlock());
                        tmpBundle.add(new SetupUnlock());
                        tmpBundle.add(new GrandFinaleUnlock());
                        break block0;
                    }
                }
                break;
            }
            case DEFECT: {
                switch (unlockLevel) {
                    case 0: {
                        tmpBundle.add(new ReboundUnlock());
                        tmpBundle.add(new UndoUnlock());
                        tmpBundle.add(new EchoFormUnlock());
                        break block0;
                    }
                    case 1: {
                        tmpBundle.add(new TurboUnlock());
                        tmpBundle.add(new SunderUnlock());
                        tmpBundle.add(new MeteorStrikeUnlock());
                        break block0;
                    }
                    case 2: {
                        tmpBundle.add(new HyperbeamUnlock());
                        tmpBundle.add(new RecycleUnlock());
                        tmpBundle.add(new NovaUnlock());
                        break block0;
                    }
                    case 3: {
                        tmpBundle.add(new CablesUnlock());
                        tmpBundle.add(new TurnipUnlock());
                        tmpBundle.add(new RunicCapacitorUnlock());
                        break block0;
                    }
                    case 4: {
                        tmpBundle.add(new EmotionChipUnlock());
                        tmpBundle.add(new VirusUnlock());
                        tmpBundle.add(new DataDiskUnlock());
                        break block0;
                    }
                }
                break;
            }
            case WATCHER: {
                switch (unlockLevel) {
                    case 0: {
                        tmpBundle.add(new ProstrateUnlock());
                        tmpBundle.add(new BlasphemyUnlock());
                        tmpBundle.add(new DevotionUnlock());
                        break block0;
                    }
                    case 1: {
                        tmpBundle.add(new ForeignInfluenceUnlock());
                        tmpBundle.add(new AlphaUnlock());
                        tmpBundle.add(new MentalFortressUnlock());
                        break block0;
                    }
                    case 2: {
                        tmpBundle.add(new ClarityUnlock());
                        tmpBundle.add(new WishUnlock());
                        tmpBundle.add(new ForesightUnlock());
                        break block0;
                    }
                    case 3: {
                        tmpBundle.add(new AkabekoUnlock());
                        tmpBundle.add(new YangUnlock());
                        tmpBundle.add(new CeramicFishUnlock());
                        break block0;
                    }
                    case 4: {
                        tmpBundle.add(new StrikeDummyUnlock());
                        tmpBundle.add(new TeardropUnlock());
                        tmpBundle.add(new CloakClaspUnlock());
                        break block0;
                    }
                }
            }
        }
        return tmpBundle;
    }

    public static void addCardUnlockToList(HashMap<String, AbstractUnlock> map, String key, AbstractUnlock unlock) {
        if (UnlockTracker.isCardLocked(key)) {
            map.put(key, unlock);
        }
    }

    public static void addRelicUnlockToList(HashMap<String, AbstractUnlock> map, String key, AbstractUnlock unlock) {
        if (UnlockTracker.isRelicLocked(key)) {
            map.put(key, unlock);
        }
    }

    public static float getCompletionPercentage() {
        float totalPercent = 0.0f;
        totalPercent += UnlockTracker.getAscensionProgress() * 0.3f;
        totalPercent += UnlockTracker.getUnlockProgress() * 0.25f;
        totalPercent += UnlockTracker.getAchievementProgress() * 0.35f;
        totalPercent += UnlockTracker.getSeenCardsProgress() * 0.05f;
        return (totalPercent += UnlockTracker.getSeenRelicsProgress() * 0.05f) * 100.0f;
    }

    private static float getAscensionProgress() {
        ArrayList<Prefs> allCharacterPrefs = CardCrawlGame.characterManager.getAllPrefs();
        int sum = 0;
        for (Prefs p : allCharacterPrefs) {
            sum += p.getInteger("ASCENSION_LEVEL", 0);
        }
        float retVal = (float)sum / 60.0f;
        logger.info("Ascension Progress: " + retVal);
        if (retVal > 1.0f) {
            retVal = 1.0f;
        }
        return retVal;
    }

    private static float getUnlockProgress() {
        int sum = Math.min(UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.IRONCLAD), 5);
        sum += Math.min(UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.THE_SILENT), 5);
        sum += Math.min(UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.DEFECT), 5);
        float retVal = (float)(sum += Math.min(UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.WATCHER), 5)) / 15.0f;
        logger.info("Unlock IC: " + UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.IRONCLAD));
        logger.info("Unlock Silent: " + UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.THE_SILENT));
        logger.info("Unlock Defect: " + UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.DEFECT));
        logger.info("Unlock Watcher: " + UnlockTracker.getUnlockLevel(AbstractPlayer.PlayerClass.WATCHER));
        logger.info("Unlock Progress: " + retVal);
        if (retVal > 1.0f) {
            retVal = 1.0f;
        }
        return retVal;
    }

    private static float getAchievementProgress() {
        int sum = 0;
        for (AchievementItem item : StatsScreen.achievements.items) {
            if (!item.isUnlocked) continue;
            ++sum;
        }
        float retVal = (float)sum / (float)StatsScreen.achievements.items.size();
        logger.info("Achievement Progress: " + retVal);
        if (retVal > 1.0f) {
            retVal = 1.0f;
        }
        return retVal;
    }

    private static float getSeenCardsProgress() {
        int sum = 0;
        for (Map.Entry<String, AbstractCard> c : CardLibrary.cards.entrySet()) {
            if (!c.getValue().isSeen) continue;
            ++sum;
        }
        float retVal = (float)sum / (float)CardLibrary.cards.size();
        logger.info("Seen Cards Progress: " + retVal);
        if (retVal > 1.0f) {
            retVal = 1.0f;
        }
        return retVal;
    }

    private static float getSeenRelicsProgress() {
        float retVal = (float)RelicLibrary.seenRelics / (float)RelicLibrary.totalRelicCount;
        logger.info("Seen Relics Progress: " + retVal);
        if (retVal > 1.0f) {
            retVal = 1.0f;
        }
        return retVal;
    }

    public static long getTotalPlaytime() {
        return Settings.totalPlayTime;
    }

    static {
        unlockReqs = new HashMap();
        lockedCards = new ArrayList();
        lockedCharacters = new ArrayList();
        lockedLoadouts = new ArrayList();
        lockedRelics = new ArrayList();
    }
}

