/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

public class GamblingChipAction
extends AbstractGameAction {
    private AbstractPlayer p;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("GamblingChipAction");
    public static final String[] TEXT = GamblingChipAction.uiStrings.TEXT;
    private boolean notchip;

    public GamblingChipAction(AbstractCreature source) {
        this.setValues(AbstractDungeon.player, source, -1);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.notchip = false;
    }

    public GamblingChipAction(AbstractCreature source, boolean notChip) {
        this.setValues(AbstractDungeon.player, source, -1);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.notchip = notChip;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            if (this.notchip) {
                AbstractDungeon.handCardSelectScreen.open(TEXT[1], 99, true, true);
            } else {
                AbstractDungeon.handCardSelectScreen.open(TEXT[0], 99, true, true);
            }
            this.addToBot(new WaitAction(0.25f));
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            if (!AbstractDungeon.handCardSelectScreen.selectedCards.group.isEmpty()) {
                this.addToTop(new DrawCardAction(this.p, AbstractDungeon.handCardSelectScreen.selectedCards.group.size()));
                for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                    AbstractDungeon.player.hand.moveToDiscardPile(c);
                    GameActionManager.incrementDiscard(false);
                    c.triggerOnManualDiscard();
                }
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }
}

