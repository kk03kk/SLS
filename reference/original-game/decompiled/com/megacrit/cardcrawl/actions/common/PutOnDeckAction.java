/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class PutOnDeckAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PutOnDeckAction");
    public static final String[] TEXT = PutOnDeckAction.uiStrings.TEXT;
    private AbstractPlayer p;
    private boolean isRandom;
    public static int numPlaced;

    public PutOnDeckAction(AbstractCreature target, AbstractCreature source, int amount, boolean isRandom) {
        this.target = target;
        this.p = (AbstractPlayer)target;
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.isRandom = isRandom;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            int i;
            if (this.p.hand.size() < this.amount) {
                this.amount = this.p.hand.size();
            }
            if (this.isRandom) {
                for (i = 0; i < this.amount; ++i) {
                    this.p.hand.moveToDeck(this.p.hand.getRandomCard(AbstractDungeon.cardRandomRng), false);
                }
            } else {
                if (this.p.hand.group.size() > this.amount) {
                    numPlaced = this.amount;
                    AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, false);
                    this.tickDuration();
                    return;
                }
                for (i = 0; i < this.p.hand.size(); ++i) {
                    this.p.hand.moveToDeck(this.p.hand.getRandomCard(AbstractDungeon.cardRandomRng), this.isRandom);
                }
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                this.p.hand.moveToDeck(c, false);
            }
            AbstractDungeon.player.hand.refreshHandLayout();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

