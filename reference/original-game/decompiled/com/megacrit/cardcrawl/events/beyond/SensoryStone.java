/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SensoryStone
extends AbstractImageEvent {
    public static final String ID = "SensoryStone";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("SensoryStone");
    public static final String NAME = SensoryStone.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = SensoryStone.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = SensoryStone.eventStrings.OPTIONS;
    private static final String INTRO_TEXT = DESCRIPTIONS[0];
    private static final String INTRO_TEXT_2 = DESCRIPTIONS[1];
    private static final String MEMORY_1_TEXT = DESCRIPTIONS[2];
    private static final String MEMORY_2_TEXT = DESCRIPTIONS[3];
    private static final String MEMORY_3_TEXT = DESCRIPTIONS[4];
    private static final String MEMORY_4_TEXT = DESCRIPTIONS[5];
    private CurScreen screen = CurScreen.INTRO;
    private int choice;

    public SensoryStone() {
        super(NAME, INTRO_TEXT, "images/events/sensoryStone.jpg");
        this.noCardsInRewards = true;
        this.imageEventText.setDialogOption(OPTIONS[5]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_SENSORY");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                this.imageEventText.updateBodyText(INTRO_TEXT_2);
                this.imageEventText.updateDialogOption(0, OPTIONS[0]);
                this.imageEventText.setDialogOption(OPTIONS[1] + 5 + OPTIONS[3]);
                this.imageEventText.setDialogOption(OPTIONS[2] + 10 + OPTIONS[3]);
                this.screen = CurScreen.INTRO_2;
                break;
            }
            case INTRO_2: {
                this.getRandomMemory();
                switch (buttonPressed) {
                    case 0: {
                        this.screen = CurScreen.ACCEPT;
                        SensoryStone.logMetric(ID, "Memory 1");
                        this.choice = 1;
                        this.reward(this.choice);
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        break;
                    }
                    case 1: {
                        this.screen = CurScreen.ACCEPT;
                        SensoryStone.logMetricTakeDamage(ID, "Memory 2", 5);
                        this.choice = 2;
                        this.reward(this.choice);
                        AbstractDungeon.player.damage(new DamageInfo(null, 5, DamageInfo.DamageType.HP_LOSS));
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        break;
                    }
                    case 2: {
                        this.screen = CurScreen.ACCEPT;
                        SensoryStone.logMetricTakeDamage(ID, "Memory 3", 10);
                        this.choice = 3;
                        this.reward(this.choice);
                        AbstractDungeon.player.damage(new DamageInfo(null, 10, DamageInfo.DamageType.HP_LOSS));
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                    }
                }
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case ACCEPT: {
                this.reward(this.choice);
            }
            default: {
                this.openMap();
            }
        }
    }

    private void getRandomMemory() {
        ArrayList<String> memories = new ArrayList<String>();
        memories.add(MEMORY_1_TEXT);
        memories.add(MEMORY_2_TEXT);
        memories.add(MEMORY_3_TEXT);
        memories.add(MEMORY_4_TEXT);
        Collections.shuffle(memories, new Random(AbstractDungeon.miscRng.randomLong()));
        this.imageEventText.updateBodyText((String)memories.get(0));
    }

    private void reward(int num) {
        AbstractDungeon.getCurrRoom().rewards.clear();
        for (int i = 0; i < num; ++i) {
            AbstractDungeon.getCurrRoom().addCardReward(new RewardItem(AbstractCard.CardColor.COLORLESS));
        }
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
        AbstractDungeon.combatRewardScreen.open();
        this.screen = CurScreen.LEAVE;
    }

    private static enum CurScreen {
        INTRO,
        INTRO_2,
        ACCEPT,
        LEAVE;

    }
}

