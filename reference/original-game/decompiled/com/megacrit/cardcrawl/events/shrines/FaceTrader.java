/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class FaceTrader
extends AbstractImageEvent {
    public static final String ID = "FaceTrader";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("FaceTrader");
    public static final String NAME = FaceTrader.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = FaceTrader.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = FaceTrader.eventStrings.OPTIONS;
    private static int goldReward;
    private static int damage;
    private CurScreen screen = CurScreen.INTRO;

    public FaceTrader() {
        super(NAME, DESCRIPTIONS[0], "images/events/facelessTrader.jpg");
        goldReward = AbstractDungeon.ascensionLevel >= 15 ? 50 : 75;
        damage = AbstractDungeon.player.maxHealth / 10;
        if (damage == 0) {
            damage = 1;
        }
        this.imageEventText.setDialogOption(OPTIONS[4]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                        this.imageEventText.updateDialogOption(0, OPTIONS[0] + damage + OPTIONS[5] + goldReward + OPTIONS[1]);
                        this.imageEventText.setDialogOption(OPTIONS[2]);
                        this.imageEventText.setDialogOption(OPTIONS[3]);
                        this.screen = CurScreen.MAIN;
                    }
                }
                break;
            }
            case MAIN: {
                switch (buttonPressed) {
                    case 0: {
                        FaceTrader.logMetricGainGoldAndDamage(ID, "Touch", goldReward, damage);
                        this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
                        AbstractDungeon.effectList.add(new RainingGoldEffect(goldReward));
                        AbstractDungeon.player.gainGold(goldReward);
                        AbstractDungeon.player.damage(new DamageInfo(null, damage));
                        CardCrawlGame.sound.play("ATTACK_POISON");
                        break;
                    }
                    case 1: {
                        AbstractRelic r = this.getRandomFace();
                        FaceTrader.logMetricObtainRelic(ID, "Trade", r);
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, r);
                        this.imageEventText.updateBodyText(DESCRIPTIONS[3]);
                        break;
                    }
                    case 2: {
                        this.logMetric("Leave");
                        this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
                        break;
                    }
                }
                this.imageEventText.clearAllDialogs();
                this.imageEventText.setDialogOption(OPTIONS[3]);
                this.screen = CurScreen.RESULT;
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private AbstractRelic getRandomFace() {
        ArrayList<String> ids = new ArrayList<String>();
        if (!AbstractDungeon.player.hasRelic("CultistMask")) {
            ids.add("CultistMask");
        }
        if (!AbstractDungeon.player.hasRelic("FaceOfCleric")) {
            ids.add("FaceOfCleric");
        }
        if (!AbstractDungeon.player.hasRelic("GremlinMask")) {
            ids.add("GremlinMask");
        }
        if (!AbstractDungeon.player.hasRelic("NlothsMask")) {
            ids.add("NlothsMask");
        }
        if (!AbstractDungeon.player.hasRelic("SsserpentHead")) {
            ids.add("SsserpentHead");
        }
        if (ids.size() <= 0) {
            ids.add("Circlet");
        }
        Collections.shuffle(ids, new Random(AbstractDungeon.miscRng.randomLong()));
        return RelicLibrary.getRelic((String)ids.get(0)).makeCopy();
    }

    public void logMetric(String actionTaken) {
        AbstractEvent.logMetric(ID, actionTaken);
    }

    private static enum CurScreen {
        INTRO,
        MAIN,
        RESULT;

    }
}

