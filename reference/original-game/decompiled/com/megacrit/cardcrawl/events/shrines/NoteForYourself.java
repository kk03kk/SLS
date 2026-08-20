/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.red.IronWave;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class NoteForYourself
extends AbstractImageEvent {
    public static final String ID = "NoteForYourself";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("NoteForYourself");
    public static final String NAME = NoteForYourself.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = NoteForYourself.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = NoteForYourself.eventStrings.OPTIONS;
    private AbstractCard obtainCard = null;
    public AbstractCard saveCard = null;
    private boolean cardSelect = false;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;

    public NoteForYourself() {
        super(NAME, DIALOG_1, "images/events/selfNote.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.initializeObtainCard();
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                this.screen = CUR_SCREEN.CHOOSE;
                this.imageEventText.updateDialogOption(0, OPTIONS[1] + this.obtainCard.name + OPTIONS[2], this.obtainCard);
                this.imageEventText.setDialogOption(OPTIONS[3]);
                break;
            }
            case CHOOSE: {
                this.screen = CUR_SCREEN.COMPLETE;
                AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                switch (buttonPressed) {
                    case 0: {
                        for (AbstractRelic r : AbstractDungeon.player.relics) {
                            r.onObtainCard(this.obtainCard);
                        }
                        AbstractDungeon.player.masterDeck.addToTop(this.obtainCard);
                        for (AbstractRelic r : AbstractDungeon.player.relics) {
                            r.onMasterDeckChange();
                        }
                        this.cardSelect = true;
                        AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, DESCRIPTIONS[2], false, false, false, false);
                        break;
                    }
                    default: {
                        NoteForYourself.logMetricIgnored(ID);
                    }
                }
                this.imageEventText.updateBodyText(DESCRIPTIONS[3]);
                this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                this.imageEventText.clearRemainingOptions();
                this.screen = CUR_SCREEN.COMPLETE;
                AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                break;
            }
            case COMPLETE: {
                this.openMap();
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.cardSelect && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            AbstractCard storeCard = AbstractDungeon.gridSelectScreen.selectedCards.remove(0);
            NoteForYourself.logMetricObtainCardAndLoseCard(ID, "Took Card", this.obtainCard, storeCard);
            AbstractDungeon.player.masterDeck.removeCard(storeCard);
            this.saveCard = storeCard;
            this.cardSelect = false;
        }
    }

    private void initializeObtainCard() {
        this.obtainCard = CardLibrary.getCard(CardCrawlGame.playerPref.getString("NOTE_CARD", "Iron Wave"));
        if (this.obtainCard == null) {
            this.obtainCard = new IronWave();
        }
        this.obtainCard = this.obtainCard.makeCopy();
        for (int i = 0; i < CardCrawlGame.playerPref.getInteger("NOTE_UPGRADE", 0); ++i) {
            this.obtainCard.upgrade();
        }
    }

    private static enum CUR_SCREEN {
        INTRO,
        CHOOSE,
        COMPLETE;

    }
}

