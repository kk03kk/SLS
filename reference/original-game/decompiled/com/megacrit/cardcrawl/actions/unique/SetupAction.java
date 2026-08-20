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

public class SetupAction
extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("SetupAction");
    public static final String[] TEXT = SetupAction.uiStrings.TEXT;

    public SetupAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() == 1) {
                AbstractCard c = this.p.hand.getTopCard();
                if (c.cost > 0) {
                    c.freeToPlayOnce = true;
                }
                this.p.hand.moveToDeck(c, false);
                AbstractDungeon.player.hand.refreshHandLayout();
                this.isDone = true;
                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (c.cost > 0) {
                    c.freeToPlayOnce = true;
                }
                this.p.hand.moveToDeck(c, false);
            }
            AbstractDungeon.player.hand.refreshHandLayout();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

