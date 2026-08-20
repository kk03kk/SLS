/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import java.util.ArrayList;
import java.util.Collections;

public class GameTips {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Random Tips");
    public static final String[] LABEL = GameTips.tutorialStrings.LABEL;
    private ArrayList<String> tips = new ArrayList();

    public GameTips() {
        this.initialize();
    }

    public void initialize() {
        Collections.addAll(this.tips, GameTips.tutorialStrings.TEXT);
        if (!Settings.isConsoleBuild) {
            Collections.addAll(this.tips, CardCrawlGame.languagePack.getTutorialString((String)"PC Tips").TEXT);
        }
        Collections.shuffle(this.tips);
    }

    public String getTip() {
        String retVal = this.tips.remove(MathUtils.random(this.tips.size() - 1));
        if (this.tips.isEmpty()) {
            this.initialize();
        }
        return retVal;
    }

    public String getPotionTip() {
        return LABEL[0];
    }
}

