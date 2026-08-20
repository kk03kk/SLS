/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.cards.curses.Shame;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Addict
extends AbstractImageEvent {
    public static final String ID = "Addict";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Addict");
    public static final String NAME = Addict.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Addict.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Addict.eventStrings.OPTIONS;
    private static final int GOLD_COST = 85;
    private int screenNum = 0;

    public Addict() {
        super(NAME, DESCRIPTIONS[0], "images/events/addict.jpg");
        if (AbstractDungeon.player.gold >= 85) {
            this.imageEventText.setDialogOption(OPTIONS[0] + 85 + OPTIONS[1], AbstractDungeon.player.gold < 85);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[2] + 85 + OPTIONS[3], AbstractDungeon.player.gold < 85);
        }
        this.imageEventText.setDialogOption(OPTIONS[4], CardLibrary.getCopy("Shame"));
        this.imageEventText.setDialogOption(OPTIONS[5]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                        if (AbstractDungeon.player.gold < 85) break;
                        AbstractRelic relic = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
                        AbstractEvent.logMetricObtainRelicAtCost(ID, "Obtained Relic", relic, 85);
                        AbstractDungeon.player.loseGold(85);
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(this.drawX, this.drawY, relic);
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
                        Shame card = new Shame();
                        AbstractRelic relic = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
                        AbstractEvent.logMetricObtainCardAndRelic(ID, "Stole Relic", card, relic);
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(card, Settings.WIDTH / 2, Settings.HEIGHT / 2));
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(this.drawX, this.drawY, relic);
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    default: {
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        this.openMap();
                    }
                }
                this.screenNum = 1;
                break;
            }
            case 1: {
                this.openMap();
            }
        }
    }
}

