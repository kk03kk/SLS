/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class BetterDrawPileToHandAction
extends AbstractGameAction {
    public static final String[] TEXT = CardCrawlGame.languagePack.getUIString((String)"BetterToHandAction").TEXT;
    private AbstractPlayer player;
    private int numberOfCards;
    private boolean optional;

    public BetterDrawPileToHandAction(int numberOfCards, boolean optional) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.player = AbstractDungeon.player;
        this.numberOfCards = numberOfCards;
        this.optional = optional;
    }

    public BetterDrawPileToHandAction(int numberOfCards) {
        this(numberOfCards, false);
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.player.drawPile.isEmpty() || this.numberOfCards <= 0) {
                this.isDone = true;
                return;
            }
            if (this.player.drawPile.size() <= this.numberOfCards && !this.optional) {
                ArrayList<AbstractCard> cardsToMove = new ArrayList<AbstractCard>();
                for (AbstractCard c : this.player.drawPile.group) {
                    cardsToMove.add(c);
                }
                for (AbstractCard c : cardsToMove) {
                    if (this.player.hand.size() == 10) {
                        this.player.drawPile.moveToDiscardPile(c);
                        this.player.createHandIsFullDialog();
                        continue;
                    }
                    this.player.drawPile.moveToHand(c, this.player.drawPile);
                }
                this.isDone = true;
                return;
            }
            CardGroup temp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard c : this.player.drawPile.group) {
                temp.addToTop(c);
            }
            temp.sortAlphabetically(true);
            temp.sortByRarityPlusStatusCardType(false);
            if (this.numberOfCards == 1) {
                if (this.optional) {
                    AbstractDungeon.gridSelectScreen.open(temp, this.numberOfCards, true, TEXT[0]);
                } else {
                    AbstractDungeon.gridSelectScreen.open(temp, this.numberOfCards, TEXT[0], false);
                }
            } else if (this.optional) {
                AbstractDungeon.gridSelectScreen.open(temp, this.numberOfCards, true, TEXT[1] + this.numberOfCards + TEXT[2]);
            } else {
                AbstractDungeon.gridSelectScreen.open(temp, this.numberOfCards, TEXT[1] + this.numberOfCards + TEXT[2], false);
            }
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                if (this.player.hand.size() == 10) {
                    this.player.drawPile.moveToDiscardPile(c);
                    this.player.createHandIsFullDialog();
                    continue;
                }
                this.player.drawPile.moveToHand(c, this.player.drawPile);
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.player.hand.refreshHandLayout();
        }
        this.tickDuration();
    }
}

