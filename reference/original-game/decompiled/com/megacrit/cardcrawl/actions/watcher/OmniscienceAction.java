/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class OmniscienceAction
extends AbstractGameAction {
    public static final String[] TEXT = CardCrawlGame.languagePack.getUIString((String)"WishAction").TEXT;
    private AbstractPlayer player;
    private int playAmt;

    public OmniscienceAction(int numberOfCards) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
        this.player = AbstractDungeon.player;
        this.playAmt = numberOfCards;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.player.drawPile.isEmpty()) {
                this.isDone = true;
                return;
            }
            CardGroup temp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard c : this.player.drawPile.group) {
                temp.addToTop(c);
            }
            temp.sortAlphabetically(true);
            temp.sortByRarityPlusStatusCardType(false);
            AbstractDungeon.gridSelectScreen.open(temp, 1, TEXT[0], false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                c.exhaust = true;
                AbstractDungeon.player.drawPile.group.remove(c);
                AbstractDungeon.getCurrRoom().souls.remove(c);
                this.addToBot(new NewQueueCardAction(c, true, false, true));
                for (int i = 0; i < this.playAmt - 1; ++i) {
                    AbstractCard tmp = c.makeStatEquivalentCopy();
                    tmp.purgeOnUse = true;
                    this.addToBot(new NewQueueCardAction(tmp, true, false, true));
                }
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.player.hand.refreshHandLayout();
        }
        this.tickDuration();
    }
}

