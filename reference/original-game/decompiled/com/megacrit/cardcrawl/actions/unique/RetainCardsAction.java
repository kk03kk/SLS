/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class RetainCardsAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RetainCardsAction");
    public static final String[] TEXT = RetainCardsAction.uiStrings.TEXT;

    public RetainCardsAction(AbstractCreature source, int amount) {
        this.setValues(AbstractDungeon.player, source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], this.amount, false, true, false, false, true);
            this.addToBot(new WaitAction(0.25f));
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (!c.isEthereal) {
                    c.retain = true;
                }
                AbstractDungeon.player.hand.addToTop(c);
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

