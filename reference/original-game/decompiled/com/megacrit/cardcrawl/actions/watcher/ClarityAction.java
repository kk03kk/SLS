/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class ClarityAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ClarityAction");
    public static final String[] TEXT = ClarityAction.uiStrings.TEXT;
    private float startingDuration;

    public ClarityAction(int numCards) {
        this.amount = numCards;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startingDuration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            if (AbstractDungeon.player.drawPile.isEmpty()) {
                this.isDone = true;
                return;
            }
            CardGroup tmpGroup = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (int i = 0; i < Math.min(this.amount, AbstractDungeon.player.drawPile.size()); ++i) {
                tmpGroup.addToTop(AbstractDungeon.player.drawPile.group.get(AbstractDungeon.player.drawPile.size() - i - 1));
            }
            AbstractDungeon.gridSelectScreen.open(tmpGroup, this.amount, false, TEXT[0]);
        } else if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
                AbstractDungeon.player.drawPile.moveToHand(c, AbstractDungeon.player.drawPile);
            }
            for (AbstractCard c : AbstractDungeon.gridSelectScreen.targetGroup.group) {
                if (AbstractDungeon.gridSelectScreen.selectedCards.contains(c)) continue;
                AbstractDungeon.player.drawPile.moveToExhaustPile(c);
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
        }
        this.tickDuration();
    }
}

