/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class DeckToHandAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;

    public DeckToHandAction(int amount) {
        this.setValues(this.p, AbstractDungeon.player, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_MED;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            AbstractDungeon.gridSelectScreen.open(this.p.drawPile, this.amount, "Select a card to add to your hand.", false);
            this.tickDuration();
            return;
        }
        if (AbstractDungeon.gridSelectScreen.selectedCards.size() != 0) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                this.p.hand.addToHand(c);
                this.p.drawPile.removeCard(c);
                c.unhover();
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            this.p.hand.refreshHandLayout();
        }
        this.tickDuration();
    }
}

