/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class RecycleAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RecycleAction");
    public static final String[] TEXT = RecycleAction.uiStrings.TEXT;
    private AbstractPlayer p;

    public RecycleAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() == 1) {
                if (this.p.hand.getBottomCard().costForTurn == -1) {
                    this.addToTop(new GainEnergyAction(EnergyPanel.getCurrentEnergy()));
                } else if (this.p.hand.getBottomCard().costForTurn > 0) {
                    this.addToTop(new GainEnergyAction(this.p.hand.getBottomCard().costForTurn));
                }
                this.p.hand.moveToExhaustPile(this.p.hand.getBottomCard());
                this.tickDuration();
                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (c.costForTurn == -1) {
                    this.addToTop(new GainEnergyAction(EnergyPanel.getCurrentEnergy()));
                } else if (c.costForTurn > 0) {
                    this.addToTop(new GainEnergyAction(c.costForTurn));
                }
                this.p.hand.moveToExhaustPile(c);
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
            AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
        }
        this.tickDuration();
    }
}

