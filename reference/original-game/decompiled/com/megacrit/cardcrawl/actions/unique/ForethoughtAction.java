/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class ForethoughtAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ForethoughtAction");
    public static final String[] TEXT = ForethoughtAction.uiStrings.TEXT;
    private boolean chooseAny;

    public ForethoughtAction(boolean upgraded) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.chooseAny = upgraded;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() == 1 && !this.chooseAny) {
                AbstractCard c = this.p.hand.getTopCard();
                if (c.cost > 0) {
                    c.freeToPlayOnce = true;
                }
                this.p.hand.moveToBottomOfDeck(c);
                AbstractDungeon.player.hand.refreshHandLayout();
                this.isDone = true;
                return;
            }
            if (!this.chooseAny) {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false);
            } else {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 99, true, true);
            }
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (c.cost > 0) {
                    c.freeToPlayOnce = true;
                }
                this.p.hand.moveToBottomOfDeck(c);
            }
            AbstractDungeon.player.hand.refreshHandLayout();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

