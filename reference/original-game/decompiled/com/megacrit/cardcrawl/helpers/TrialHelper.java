/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.trials.AbstractTrial;
import com.megacrit.cardcrawl.trials.AnyColorDraftTrial;
import com.megacrit.cardcrawl.trials.CursedTrial;
import com.megacrit.cardcrawl.trials.DraftTrial;
import com.megacrit.cardcrawl.trials.HoarderTrial;
import com.megacrit.cardcrawl.trials.InceptionTrial;
import com.megacrit.cardcrawl.trials.LoseMaxHpTrial;
import com.megacrit.cardcrawl.trials.MyTrueFormTrial;
import com.megacrit.cardcrawl.trials.OneHpTrial;
import com.megacrit.cardcrawl.trials.RandomModsTrial;
import com.megacrit.cardcrawl.trials.SlowpokeTrial;
import com.megacrit.cardcrawl.trials.SneckoTrial;
import com.megacrit.cardcrawl.trials.StarterDeckTrial;
import java.util.HashMap;

public class TrialHelper {
    private static HashMap<String, TRIAL> trialKeysMap;

    private static void initialize() {
        if (trialKeysMap != null) {
            return;
        }
        trialKeysMap = new HashMap();
        trialKeysMap.put(TrialHelper.formatKey("RandomMods"), TRIAL.RANDOM_MODS);
        trialKeysMap.put(TrialHelper.formatKey("DailyMods"), TRIAL.RANDOM_MODS);
        trialKeysMap.put(TrialHelper.formatKey("StarterDeck"), TRIAL.NO_CARD_DROPS);
        trialKeysMap.put(TrialHelper.formatKey("Inception"), TRIAL.UNCEASING_TOP);
        trialKeysMap.put(TrialHelper.formatKey("FadeAway"), TRIAL.LOSE_MAX_HP);
        trialKeysMap.put(TrialHelper.formatKey("PraiseSnecko"), TRIAL.SNECKO);
        trialKeysMap.put(TrialHelper.formatKey("YoureTooSlow"), TRIAL.SLOW);
        trialKeysMap.put(TrialHelper.formatKey("MyTrueForm"), TRIAL.FORMS);
        trialKeysMap.put(TrialHelper.formatKey("Draft"), TRIAL.DRAFT);
        trialKeysMap.put(TrialHelper.formatKey("MegaDraft"), TRIAL.MEGA_DRAFT);
        trialKeysMap.put(TrialHelper.formatKey("1HitWonder"), TRIAL.ONE_HP);
        trialKeysMap.put(TrialHelper.formatKey("MoreCards"), TRIAL.MORE_CARDS);
        trialKeysMap.put(TrialHelper.formatKey("Cursed"), TRIAL.CURSED);
    }

    private static String formatKey(String key) {
        return SeedHelper.sterilizeString(key);
    }

    public static boolean isTrialSeed(String seed) {
        TrialHelper.initialize();
        return trialKeysMap.containsKey(seed);
    }

    public static AbstractTrial getTrialForSeed(String seed) {
        TrialHelper.initialize();
        if (seed == null) {
            return null;
        }
        TRIAL picked = trialKeysMap.get(seed);
        if (picked == null) {
            return null;
        }
        switch (picked) {
            case RANDOM_MODS: {
                return new RandomModsTrial();
            }
            case NO_CARD_DROPS: {
                return new StarterDeckTrial();
            }
            case UNCEASING_TOP: {
                return new InceptionTrial();
            }
            case LOSE_MAX_HP: {
                return new LoseMaxHpTrial();
            }
            case SNECKO: {
                return new SneckoTrial();
            }
            case SLOW: {
                return new SlowpokeTrial();
            }
            case FORMS: {
                return new MyTrueFormTrial();
            }
            case DRAFT: {
                return new DraftTrial();
            }
            case MEGA_DRAFT: {
                return new AnyColorDraftTrial();
            }
            case ONE_HP: {
                return new OneHpTrial();
            }
            case MORE_CARDS: {
                return new HoarderTrial();
            }
            case CURSED: {
                return new CursedTrial();
            }
        }
        return null;
    }

    private static enum TRIAL {
        RANDOM_MODS,
        NO_CARD_DROPS,
        UNCEASING_TOP,
        LOSE_MAX_HP,
        SNECKO,
        SLOW,
        FORMS,
        DRAFT,
        MEGA_DRAFT,
        ONE_HP,
        MORE_CARDS,
        CURSED;

    }
}

