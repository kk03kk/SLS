/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class MeditateAction
extends AbstractGameAction {
    public static final String[] TEXT = CardCrawlGame.languagePack.getUIString((String)"BetterToHandAction").TEXT;
    private AbstractPlayer player;
    private int numberOfCards;
    private boolean optional = false;

    public MeditateAction(int numberOfCards) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.player = AbstractDungeon.player;
        this.numberOfCards = numberOfCards;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.player.discardPile.isEmpty() || this.numberOfCards <= 0) {
                this.isDone = true;
                return;
            }
            if (this.player.discardPile.size() <= this.numberOfCards && !this.optional) {
                ArrayList<AbstractCard> cardsToMove = new ArrayList<AbstractCard>();
                for (AbstractCard c : this.player.discardPile.group) {
                    cardsToMove.add(c);
                    c.retain = true;
                }
                for (AbstractCard c : cardsToMove) {
                    if (this.player.hand.size() < 10) {
                        this.player.hand.addToHand(c);
                        this.player.discardPile.removeCard(c);
                    }
                    c.lighten(false);
                }
                this.isDone = true;
                return;
            }
            if (this.numberOfCards == 1) {
                if (this.optional) {
                    AbstractDungeon.gridSelectScreen.open(this.player.discardPile, this.numberOfCards, true, TEXT[0]);
                } else {
                    AbstractDungeon.gridSelectScreen.open(this.player.discardPile, this.numberOfCards, TEXT[0], false);
                }
            } else if (this.optional) {
                AbstractDungeon.gridSelectScreen.open(this.player.discardPile, this.numberOfCards, true, TEXT[1] + this.numberOfCards + TEXT[2]);
            } else {
                AbstractDungeon.gridSelectScreen.open(this.player.discardPile, this.numberOfCards, TEXT[1] + this.numberOfCards + TEXT[2], false);
            }
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                if (this.player.hand.size() < 10) {
                    this.player.hand.addToHand(c);
                    c.retain = true;
                    this.player.discardPile.removeCard(c);
                }
                c.lighten(false);
                c.unhover();
            }
            for (AbstractCard c : this.player.discardPile.group) {
                c.unhover();
                c.target_x = CardGroup.DISCARD_PILE_X;
                c.target_y = 0.0f;
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.player.hand.refreshHandLayout();
        }
        this.tickDuration();
    }
}

