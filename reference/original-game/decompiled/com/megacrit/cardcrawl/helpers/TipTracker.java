/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TipTracker {
    private static final Logger logger = LogManager.getLogger(TipTracker.class.getName());
    public static Prefs pref;
    public static HashMap<String, Boolean> tips;
    public static final String NEOW_SKIP = "NEOW_SKIP";
    public static final String NEOW_INTRO = "NEOW_INTRO";
    public static final String NO_FTUE_CHECK = "NO_FTUE";
    public static final String COMBAT_TIP = "COMBAT_TIP";
    public static final String BLOCK_TIP = "BLOCK TIP";
    public static final String POWER_TIP = "POWER_TIP";
    public static final String M_POWER_TIP = "M_POWER_TIP";
    public static final String ENERGY_USE_TIP = "ENERGY_USE_TIP";
    public static int energyUseCounter;
    public static final String SHUFFLE_TIP = "SHUFFLE_TIP";
    public static int shuffleCounter;
    public static final int SHUFFLE_THRESHOLD = 1;
    public static final String POTION_TIP = "POTION_TIP";
    public static final String CARD_REWARD_TIP = "CARD_REWARD_TIP";
    public static final String INTENT_TIP = "INTENT_TIP";
    public static int blockCounter;
    public static final int BLOCK_THRESHOLD = 3;
    public static final String RELIC_TIP = "RELIC_TIP";
    public static int relicCounter;

    public static void initialize() {
        pref = SaveHelper.getPrefs("STSTips");
        TipTracker.refresh();
    }

    public static void refresh() {
        tips.clear();
        tips.put(NEOW_SKIP, pref.getBoolean(NEOW_SKIP, false));
        tips.put(NEOW_INTRO, pref.getBoolean(NEOW_INTRO, false));
        tips.put(NO_FTUE_CHECK, pref.getBoolean(NO_FTUE_CHECK, false));
        tips.put(COMBAT_TIP, pref.getBoolean(COMBAT_TIP, false));
        tips.put(BLOCK_TIP, pref.getBoolean(BLOCK_TIP, false));
        tips.put(POWER_TIP, pref.getBoolean(POWER_TIP, false));
        tips.put(M_POWER_TIP, pref.getBoolean(M_POWER_TIP, false));
        tips.put(ENERGY_USE_TIP, pref.getBoolean(ENERGY_USE_TIP, false));
        energyUseCounter = tips.get(ENERGY_USE_TIP) != false ? 9 : 0;
        tips.put(SHUFFLE_TIP, pref.getBoolean(SHUFFLE_TIP, false));
        shuffleCounter = tips.get(SHUFFLE_TIP) != false ? 99 : 0;
        shuffleCounter = 0;
        tips.put(POTION_TIP, pref.getBoolean(POTION_TIP, false));
        tips.put(CARD_REWARD_TIP, pref.getBoolean(CARD_REWARD_TIP, false));
        tips.put(INTENT_TIP, pref.getBoolean(INTENT_TIP, false));
        blockCounter = 0;
        tips.put(RELIC_TIP, pref.getBoolean(RELIC_TIP, false));
        relicCounter = tips.get(RELIC_TIP) != false ? 99 : 0;
    }

    public static void neverShowAgain(String key) {
        logger.info(key + " will never be shown again!");
        pref.putBoolean(key, true);
        tips.put(key, true);
        pref.flush();
    }

    public static void showAgain(String key) {
        logger.info(key + " is reactivated");
        pref.putBoolean(key, false);
        tips.put(key, false);
        pref.flush();
    }

    public static void disableAllFtues() {
        TipTracker.neverShowAgain(BLOCK_TIP);
        TipTracker.neverShowAgain(CARD_REWARD_TIP);
        TipTracker.neverShowAgain(COMBAT_TIP);
        TipTracker.neverShowAgain(ENERGY_USE_TIP);
        TipTracker.neverShowAgain(INTENT_TIP);
        TipTracker.neverShowAgain(M_POWER_TIP);
        TipTracker.neverShowAgain(NO_FTUE_CHECK);
        TipTracker.neverShowAgain(POTION_TIP);
        TipTracker.neverShowAgain(POWER_TIP);
        TipTracker.neverShowAgain(RELIC_TIP);
        TipTracker.neverShowAgain(SHUFFLE_TIP);
    }

    public static void reset() {
        for (Map.Entry<String, Boolean> c : tips.entrySet()) {
            TipTracker.showAgain(c.getKey());
        }
    }

    static {
        tips = new HashMap();
        energyUseCounter = 0;
        shuffleCounter = 0;
        blockCounter = 0;
        relicCounter = 0;
    }
}

