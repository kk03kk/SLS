/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;

public class PurificationShrine
extends AbstractImageEvent {
    public static final String ID = "Purifier";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Purifier");
    public static final String NAME = PurificationShrine.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = PurificationShrine.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = PurificationShrine.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String IGNORE = DESCRIPTIONS[2];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;

    public PurificationShrine() {
        super(NAME, DIALOG_1, "images/events/shrine3.jpg");
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
        if (!AbstractDungeon.isScreenUp && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            CardCrawlGame.sound.play("CARD_EXHAUST");
            PurificationShrine.logMetricCardRemoval(ID, "Purged", AbstractDungeon.gridSelectScreen.selectedCards.get(0));
            AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(AbstractDungeon.gridSelectScreen.selectedCards.get(0), Settings.WIDTH / 2, Settings.HEIGHT / 2));
            AbstractDungeon.player.masterDeck.removeCard(AbstractDungeon.gridSelectScreen.selectedCards.get(0));
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
                        this.imageEventText.updateBodyText(DIALOG_2);
                        AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, OPTIONS[2], false, false, false, true);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        this.imageEventText.clearRemainingOptions();
                        break;
                    }
                    case 1: {
                        this.screen = CUR_SCREEN.COMPLETE;
                        PurificationShrine.logMetricIgnored(ID);
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

