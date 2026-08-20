/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class PathVictoryAction
extends AbstractGameAction {
    public PathVictoryAction() {
        if (AbstractDungeon.player.hasPower("No Draw")) {
            AbstractDungeon.player.getPower("No Draw").flash();
            this.setValues(AbstractDungeon.player, this.source, 1);
            this.isDone = true;
            this.duration = 0.0f;
            this.actionType = AbstractGameAction.ActionType.WAIT;
            return;
        }
        this.setValues(AbstractDungeon.player, this.source, this.amount);
        this.actionType = AbstractGameAction.ActionType.DRAW;
        this.duration = Settings.FAST_MODE ? Settings.ACTION_DUR_XFAST : Settings.ACTION_DUR_FASTER;
    }

    @Override
    public void update() {
        int deckSize = AbstractDungeon.player.drawPile.size();
        int discardSize = AbstractDungeon.player.discardPile.size();
        if (SoulGroup.isActive()) {
            return;
        }
        if (deckSize + discardSize == 0) {
            this.isDone = true;
            return;
        }
        if (AbstractDungeon.player.hand.size() == 10) {
            AbstractDungeon.player.createHandIsFullDialog();
            this.isDone = true;
            return;
        }
        if (deckSize == 0 && discardSize != 0) {
            this.addToTop(new PathVictoryAction());
            this.addToTop(new EmptyDeckShuffleAction());
            this.isDone = true;
            return;
        }
        if (deckSize != 0) {
            AbstractCard c = AbstractDungeon.player.drawPile.getTopCard();
            c.setCostForTurn(0);
            AbstractDungeon.player.draw();
            AbstractDungeon.player.hand.refreshHandLayout();
            this.isDone = true;
            return;
        }
    }
}

