/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.stats;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.AchievementStrings;
import com.megacrit.cardcrawl.screens.stats.AchievementItem;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;

public class AchievementGrid {
    private static final AchievementStrings achievementStrings = CardCrawlGame.languagePack.getAchievementString("AchievementGrid");
    public static final String[] NAMES = AchievementGrid.achievementStrings.NAMES;
    public static final String[] TEXT = AchievementGrid.achievementStrings.TEXT;
    public ArrayList<AchievementItem> items = new ArrayList();
    private static final float SPACING = 200.0f * Settings.scale;
    private static final int ITEMS_PER_ROW = 5;
    public static final String SHRUG_KEY = "SHRUG_IT_OFF";
    public static final String PURITY_KEY = "PURITY";
    public static final String COME_AT_ME_KEY = "COME_AT_ME";
    public static final String THE_PACT_KEY = "THE_PACT";
    public static final String ADRENALINE_KEY = "ADRENALINE";
    public static final String POWERFUL_KEY = "POWERFUL";
    public static final String JAXXED_KEY = "JAXXED";
    public static final String IMPERVIOUS_KEY = "IMPERVIOUS";
    public static final String BARRICADED_KEY = "BARRICADED";
    public static final String CATALYST_KEY = "CATALYST";
    public static final String PLAGUE_KEY = "PLAGUE";
    public static final String NINJA_KEY = "NINJA";
    public static final String INFINITY_KEY = "INFINITY";
    public static final String YOU_ARE_NOTHING_KEY = "YOU_ARE_NOTHING";
    public static final String PERFECT_KEY = "PERFECT";
    public static final String ONE_RELIC_KEY = "ONE_RELIC";
    public static final String SPEED_CLIMBER_KEY = "SPEED_CLIMBER";
    public static final String ASCEND_0_KEY = "ASCEND_0";
    public static final String ASCEND_10_KEY = "ASCEND_10";
    public static final String ASCEND_20_KEY = "ASCEND_20";
    public static final String MINMALIST_KEY = "MINIMALIST";
    public static final String DONUT_KEY = "DONUT";
    public static final String COMMON_SENSE_KEY = "COMMON_SENSE";
    public static final String FOCUSED_KEY = "FOCUSED";
    public static final String LUCKY_DAY_KEY = "LUCKY_DAY";
    public static final String NEON_KEY = "NEON";
    public static final String TRANSIENT_KEY = "TRANSIENT";
    public static final String GUARDIAN_KEY = "GUARDIAN";
    public static final String GHOST_GUARDIAN_KEY = "GHOST_GUARDIAN";
    public static final String SLIME_BOSS_KEY = "SLIME_BOSS";
    public static final String AUTOMATON_KEY = "AUTOMATON";
    public static final String COLLECTOR_KEY = "COLLECTOR";
    public static final String CHAMP_KEY = "CHAMP";
    public static final String CROW_KEY = "CROW";
    public static final String SHAPES_KEY = "SHAPES";
    public static final String TIME_EATER_KEY = "TIME_EATER";
    public static final String RUBY_KEY = "RUBY";
    public static final String EMERALD_KEY = "EMERALD";
    public static final String SAPPHIRE_KEY = "SAPPHIRE";
    public static final String AMETHYST_KEY = "AMETHYST";
    public static final String RUBY_PLUS_KEY = "RUBY_PLUS";
    public static final String EMERALD_PLUS_KEY = "EMERALD_PLUS";
    public static final String SAPPHIRE_PLUS_KEY = "SAPPHIRE_PLUS";
    public static final String AMETHYST_PLUS_KEY = "AMETHYST_PLUS";
    public static final String THE_ENDING_KEY = "THE_ENDING";
    public static final String ETERNAL_ONE_KEY = "ETERNAL_ONE";

    public AchievementGrid() {
        this.items.add(new AchievementItem(NAMES[0], TEXT[0], "shrugItOff", SHRUG_KEY));
        this.items.add(new AchievementItem(NAMES[1], TEXT[1], "purity", PURITY_KEY));
        this.items.add(new AchievementItem(NAMES[2], TEXT[2], "comeAtMe", COME_AT_ME_KEY));
        this.items.add(new AchievementItem(NAMES[3], TEXT[3], "thePact", THE_PACT_KEY));
        this.items.add(new AchievementItem(NAMES[4], TEXT[4], "adrenaline", ADRENALINE_KEY));
        this.items.add(new AchievementItem(NAMES[5], TEXT[5], "powerful", POWERFUL_KEY));
        this.items.add(new AchievementItem(NAMES[6], TEXT[6], "jaxxed", JAXXED_KEY));
        this.items.add(new AchievementItem(NAMES[7], TEXT[7], "impervious", IMPERVIOUS_KEY));
        this.items.add(new AchievementItem(NAMES[8], TEXT[8], "barricaded", BARRICADED_KEY));
        this.items.add(new AchievementItem(NAMES[9], TEXT[9], "catalyst", CATALYST_KEY));
        this.items.add(new AchievementItem(NAMES[10], TEXT[10], "plague", PLAGUE_KEY));
        this.items.add(new AchievementItem(NAMES[11], TEXT[11], "ninja", NINJA_KEY));
        this.items.add(new AchievementItem(NAMES[12], TEXT[12], "infinity", INFINITY_KEY));
        this.items.add(new AchievementItem(NAMES[35], TEXT[35], "focused", FOCUSED_KEY));
        this.items.add(new AchievementItem(NAMES[36], TEXT[36], "neon", NEON_KEY));
        this.items.add(new AchievementItem(NAMES[13], TEXT[13], "youAreNothing", YOU_ARE_NOTHING_KEY));
        this.items.add(new AchievementItem(NAMES[31], TEXT[31], "minimalist", MINMALIST_KEY));
        this.items.add(new AchievementItem(NAMES[32], TEXT[32], "donut", DONUT_KEY));
        this.items.add(new AchievementItem(NAMES[14], TEXT[14], "perfect", PERFECT_KEY));
        this.items.add(new AchievementItem(NAMES[27], TEXT[27], "onerelic", ONE_RELIC_KEY));
        this.items.add(new AchievementItem(NAMES[28], TEXT[28], "speed", SPEED_CLIMBER_KEY));
        this.items.add(new AchievementItem(NAMES[34], TEXT[34], "commonSense", COMMON_SENSE_KEY));
        this.items.add(new AchievementItem(NAMES[38], TEXT[38], "luckyDay", LUCKY_DAY_KEY));
        this.items.add(new AchievementItem(NAMES[29], TEXT[29], "0", ASCEND_0_KEY));
        this.items.add(new AchievementItem(NAMES[30], TEXT[30], "10", ASCEND_10_KEY));
        this.items.add(new AchievementItem(NAMES[40], TEXT[40], "20", ASCEND_20_KEY));
        this.items.add(new AchievementItem(NAMES[39], TEXT[39], "transient", TRANSIENT_KEY));
        this.items.add(new AchievementItem(NAMES[15], TEXT[15], "guardian", GUARDIAN_KEY, true));
        this.items.add(new AchievementItem(NAMES[16], TEXT[16], "ghostGuardian", GHOST_GUARDIAN_KEY, true));
        this.items.add(new AchievementItem(NAMES[17], TEXT[17], "slimeBoss", SLIME_BOSS_KEY, true));
        this.items.add(new AchievementItem(NAMES[18], TEXT[18], "automaton", AUTOMATON_KEY, true));
        this.items.add(new AchievementItem(NAMES[19], TEXT[19], "collector", COLLECTOR_KEY, true));
        this.items.add(new AchievementItem(NAMES[20], TEXT[20], "champ", CHAMP_KEY, true));
        this.items.add(new AchievementItem(NAMES[21], TEXT[21], "awakenedOne", CROW_KEY, true));
        this.items.add(new AchievementItem(NAMES[22], TEXT[22], "shapes", SHAPES_KEY, true));
        this.items.add(new AchievementItem(NAMES[23], TEXT[23], "timeEater", TIME_EATER_KEY, true));
        this.items.add(new AchievementItem(NAMES[24], TEXT[24], "ironclad", RUBY_KEY));
        this.items.add(new AchievementItem(NAMES[25], TEXT[25], "silent", EMERALD_KEY));
        this.items.add(new AchievementItem(NAMES[33], TEXT[33], "sapphire", SAPPHIRE_KEY));
        this.items.add(new AchievementItem(NAMES[46], TEXT[46], "amethyst", AMETHYST_KEY));
        this.items.add(new AchievementItem(NAMES[41], TEXT[41], "rubyPlus", RUBY_PLUS_KEY, true));
        this.items.add(new AchievementItem(NAMES[42], TEXT[42], "emeraldPlus", EMERALD_PLUS_KEY, true));
        this.items.add(new AchievementItem(NAMES[43], TEXT[43], "sapphirePlus", SAPPHIRE_PLUS_KEY, true));
        this.items.add(new AchievementItem(NAMES[47], TEXT[47], "amethyst_plus", AMETHYST_PLUS_KEY, true));
        this.items.add(new AchievementItem(NAMES[44], TEXT[44], "theEnding", THE_ENDING_KEY, true));
        this.items.add(new AchievementItem(NAMES[45], TEXT[45], "eternal_one", ETERNAL_ONE_KEY, true));
        if (UnlockTracker.allAchievementsExceptPlatinumUnlocked()) {
            UnlockTracker.unlockAchievement(ETERNAL_ONE_KEY);
        }
    }

    public void update() {
        for (AchievementItem i : this.items) {
            i.update();
        }
    }

    public void render(SpriteBatch sb, float renderY) {
        for (int i = 0; i < this.items.size(); ++i) {
            this.items.get(i).render(sb, 560.0f * Settings.scale + (float)(i % 5) * SPACING, renderY - (float)(i / 5) * SPACING + 680.0f * Settings.yScale);
        }
    }

    public void refreshImg() {
        for (AchievementItem i : this.items) {
            i.reloadImg();
        }
    }
}

