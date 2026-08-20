/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.exordium;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.curses.Doubt;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Sssserpent
extends AbstractImageEvent {
    public static final String ID = "Liars Game";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Liars Game");
    public static final String NAME = Sssserpent.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Sssserpent.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Sssserpent.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String AGREE_DIALOG = DESCRIPTIONS[1];
    private static final String DISAGREE_DIALOG = DESCRIPTIONS[2];
    private static final String GOLD_RAIN_MSG = DESCRIPTIONS[3];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;
    private static final int GOLD_REWARD = 175;
    private static final int A_2_GOLD_REWARD = 150;
    private int goldReward = AbstractDungeon.ascensionLevel >= 15 ? 150 : 175;
    private AbstractCard curse = new Doubt();

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_SERPENT");
        }
    }

    public Sssserpent() {
        super(NAME, DIALOG_1, "images/events/liarsGame.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0] + this.goldReward + OPTIONS[1], CardLibrary.getCopy(this.curse.cardID));
        this.imageEventText.setDialogOption(OPTIONS[2]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                if (buttonPressed == 0) {
                    this.imageEventText.updateBodyText(AGREE_DIALOG);
                    this.imageEventText.removeDialogOption(1);
                    this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                    this.screen = CUR_SCREEN.AGREE;
                    AbstractEvent.logMetricGainGoldAndCard(ID, "AGREE", this.curse, this.goldReward);
                    break;
                }
                this.imageEventText.updateBodyText(DISAGREE_DIALOG);
                this.imageEventText.removeDialogOption(1);
                this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                this.screen = CUR_SCREEN.DISAGREE;
                AbstractEvent.logMetricIgnored(ID);
                break;
            }
            case AGREE: {
                AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(this.curse, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.effectList.add(new RainingGoldEffect(this.goldReward));
                AbstractDungeon.player.gainGold(this.goldReward);
                this.imageEventText.updateBodyText(GOLD_RAIN_MSG);
                this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                this.screen = CUR_SCREEN.COMPLETE;
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CUR_SCREEN {
        INTRO,
        AGREE,
        DISAGREE,
        COMPLETE;

    }
}

