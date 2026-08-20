/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;

public class UpgradeShrine
extends AbstractImageEvent {
    public static final String ID = "Upgrade Shrine";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Upgrade Shrine");
    public static final String NAME = UpgradeShrine.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = UpgradeShrine.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = UpgradeShrine.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String IGNORE = DESCRIPTIONS[2];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;

    public UpgradeShrine() {
        super(NAME, DIALOG_1, "images/events/shrine2.jpg");
        if (AbstractDungeon.player.masterDeck.hasUpgradableCards().booleanValue()) {
            this.imageEventText.setDialogOption(OPTIONS[0]);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[3], true);
        }
        this.imageEventText.setDialogOption(OPTIONS[1]);
    }

    @Override
    public void onEnterRoom() {
        CardCrawlGame.music.playTempBGM("SHRINE");
    }

    @Override
    public void update() {
        super.update();
        if (!AbstractDungeon.isScreenUp && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            c.upgrade();
            UpgradeShrine.logMetricCardUpgrade(ID, "Upgraded", c);
            AbstractDungeon.player.bottledCardUpgradeCheck(c);
            AbstractDungeon.effectsQueue.add(new ShowCardBrieflyEffect(c.makeStatEquivalentCopy()));
            AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                        this.imageEventText.updateBodyText(DIALOG_2);
                        AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck.getUpgradableCards(), 1, OPTIONS[2], true, false, false, false);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    case 1: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                        UpgradeShrine.logMetricIgnored(ID);
                        this.imageEventText.updateBodyText(IGNORE);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
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

