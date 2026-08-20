/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class BendAction
extends AbstractGameAction {
    private AbstractPlayer p;
    private ArrayList<AbstractCard> cannotChoose = new ArrayList();

    public BendAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (AbstractCard c : this.p.hand.group) {
                if (c.type == AbstractCard.CardType.ATTACK && c.cost > 0) continue;
                this.cannotChoose.add(c);
            }
            if (this.cannotChoose.size() == this.p.hand.group.size()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.group.size() - this.cannotChoose.size() == 1) {
                for (AbstractCard c : this.p.hand.group) {
                    if (c.type != AbstractCard.CardType.ATTACK) continue;
                    c.modifyCostForCombat(-1);
                    this.isDone = true;
                    return;
                }
            }
            this.p.hand.group.removeAll(this.cannotChoose);
            if (this.p.hand.group.size() > 1) {
                AbstractDungeon.handCardSelectScreen.open("Upgrade.", 1, false);
                this.tickDuration();
                return;
            }
            if (this.p.hand.group.size() == 1) {
                this.p.hand.getTopCard().modifyCostForCombat(-1);
                this.returnCards();
                this.isDone = true;
            }
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                c.modifyCostForCombat(-1);
                this.p.hand.addToTop(c);
            }
            this.returnCards();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
            AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
            this.isDone = true;
        }
        this.tickDuration();
    }

    private void returnCards() {
        for (AbstractCard c : this.cannotChoose) {
            this.p.hand.addToTop(c);
        }
        this.p.hand.refreshHandLayout();
    }
}

