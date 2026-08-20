/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Duplicator
extends AbstractImageEvent {
    public static final String ID = "Duplicator";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Duplicator");
    public static final String NAME = Duplicator.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Duplicator.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Duplicator.eventStrings.OPTIONS;
    private int screenNum = 0;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String IGNORE = DESCRIPTIONS[2];

    public Duplicator() {
        super(NAME, DIALOG_1, "images/events/shrine4.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.imageEventText.setDialogOption(OPTIONS[1]);
    }

    @Override
    public void onEnterRoom() {
        CardCrawlGame.music.playTempBGM("SHRINE");
    }

    @Override
    public void update() {
        super.update();
        if (!(AbstractDungeon.isScreenUp || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty() || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty())) {
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0).makeStatEquivalentCopy();
            Duplicator.logMetricObtainCard(ID, "Copied", c);
            c.inBottleFlame = false;
            c.inBottleLightning = false;
            c.inBottleTornado = false;
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DIALOG_2);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        this.imageEventText.clearRemainingOptions();
                        this.use();
                        this.screenNum = 2;
                        break;
                    }
                    case 1: {
                        this.screenNum = 2;
                        this.imageEventText.updateBodyText(IGNORE);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        this.imageEventText.clearRemainingOptions();
                        Duplicator.logMetricIgnored(ID);
                    }
                }
                break;
            }
            case 1: {
                this.screenNum = 2;
                break;
            }
            case 2: {
                this.openMap();
                break;
            }
        }
    }

    public void use() {
        AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck, 1, OPTIONS[2], false, false, false, false);
    }

    public void logMetric(String result) {
        AbstractEvent.logMetric(ID, result);
    }
}

