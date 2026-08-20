/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.curses.Regret;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class GoldShrine
extends AbstractImageEvent {
    public static final String ID = "Golden Shrine";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Golden Shrine");
    public static final String NAME = GoldShrine.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = GoldShrine.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = GoldShrine.eventStrings.OPTIONS;
    private static final int GOLD_AMT = 100;
    private static final int CURSE_GOLD_AMT = 275;
    private static final int A_2_GOLD_AMT = 50;
    private int goldAmt;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String DIALOG_3 = DESCRIPTIONS[2];
    private static final String IGNORE = DESCRIPTIONS[3];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;

    public GoldShrine() {
        super(NAME, DIALOG_1, "images/events/goldShrine.jpg");
        this.goldAmt = AbstractDungeon.ascensionLevel >= 15 ? 50 : 100;
        this.imageEventText.setDialogOption(OPTIONS[0] + this.goldAmt + OPTIONS[1]);
        this.imageEventText.setDialogOption(OPTIONS[2], CardLibrary.getCopy("Regret"));
        this.imageEventText.setDialogOption(OPTIONS[3]);
    }

    @Override
    public void onEnterRoom() {
        CardCrawlGame.music.playTempBGM("SHRINE");
    }

    @Override
    public void update() {
        super.update();
        if (!RoomEventDialog.waitForInput) {
            this.buttonEffect(this.roomEventText.getSelectedOption());
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        GoldShrine.logMetricGainGold(ID, "Pray", this.goldAmt);
                        this.imageEventText.updateBodyText(DIALOG_2);
                        this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                        AbstractDungeon.effectList.add(new RainingGoldEffect(this.goldAmt));
                        AbstractDungeon.player.gainGold(this.goldAmt);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    case 1: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        Regret curse = new Regret();
                        GoldShrine.logMetricGainGoldAndCard(ID, "Desecrate", curse, 275);
                        AbstractDungeon.effectList.add(new RainingGoldEffect(275));
                        AbstractDungeon.player.gainGold(275);
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(curse, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        this.imageEventText.updateBodyText(DIALOG_3);
                        this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    case 2: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        GoldShrine.logMetricIgnored(ID);
                        this.imageEventText.updateBodyText(IGNORE);
                        this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                        this.imageEventText.clearRemainingOptions();
                    }
                }
                break;
            }
            case COMPLETE: {
                this.openMap();
            }
        }
    }

    private static enum CUR_SCREEN {
        INTRO,
        COMPLETE;

    }
}

