/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import java.util.ArrayList;

public class EmptyCage
extends AbstractRelic {
    public static final String ID = "Empty Cage";
    private boolean cardsSelected = true;

    public EmptyCage() {
        super(ID, "cage.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.cardsSelected = false;
        if (AbstractDungeon.isScreenUp) {
            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.overlayMenu.cancelButton.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.INCOMPLETE;
        CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (AbstractCard card : AbstractDungeon.player.masterDeck.getPurgeableCards().group) {
            tmp.addToTop(card);
        }
        if (tmp.group.isEmpty()) {
            this.cardsSelected = true;
            return;
        }
        if (tmp.group.size() <= 2) {
            this.deleteCards(tmp.group);
        } else {
            AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck.getPurgeableCards(), 2, this.DESCRIPTIONS[1], false, false, false, true);
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.cardsSelected && AbstractDungeon.gridSelectScreen.selectedCards.size() == 2) {
            this.deleteCards(AbstractDungeon.gridSelectScreen.selectedCards);
        }
    }

    public void deleteCards(ArrayList<AbstractCard> group) {
        this.cardsSelected = true;
        float displayCount = 0.0f;
        for (AbstractCard card : group) {
            card.untip();
            card.unhover();
            AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(card, (float)Settings.WIDTH / 3.0f + displayCount, (float)Settings.HEIGHT / 2.0f));
            displayCount += (float)Settings.WIDTH / 6.0f;
            AbstractDungeon.player.masterDeck.removeCard(card);
        }
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
    }

    @Override
    public AbstractRelic makeCopy() {
        return new EmptyCage();
    }
}

