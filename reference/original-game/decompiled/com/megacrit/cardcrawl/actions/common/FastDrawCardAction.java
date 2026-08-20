/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.PlayerTurnEffect;

public class FastDrawCardAction
extends AbstractGameAction {
    private boolean shuffleCheck = false;

    public FastDrawCardAction(AbstractCreature source, int amount, boolean endTurnDraw) {
        if (endTurnDraw) {
            AbstractDungeon.effectList.add(new PlayerTurnEffect());
        } else if (AbstractDungeon.player.hasPower("No Draw")) {
            AbstractDungeon.player.getPower("No Draw").flash();
            this.setValues(AbstractDungeon.player, source, amount);
            this.isDone = true;
            this.duration = 0.0f;
            this.actionType = AbstractGameAction.ActionType.WAIT;
            return;
        }
        this.setValues(AbstractDungeon.player, source, amount);
        this.actionType = AbstractGameAction.ActionType.DRAW;
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    public FastDrawCardAction(AbstractCreature source, int amount) {
        this(source, amount, false);
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
        if (!this.shuffleCheck) {
            if (this.amount + AbstractDungeon.player.hand.size() > 10) {
                int handSizeAndDraw = 10 - (this.amount + AbstractDungeon.player.hand.size());
                this.amount += handSizeAndDraw;
                AbstractDungeon.player.createHandIsFullDialog();
            }
            if (this.amount > deckSize) {
                int tmp = this.amount - deckSize;
                this.addToTop(new FastDrawCardAction(AbstractDungeon.player, tmp));
                this.addToTop(new EmptyDeckShuffleAction());
                if (deckSize != 0) {
                    this.addToTop(new FastDrawCardAction(AbstractDungeon.player, deckSize));
                }
                this.amount = 0;
                this.isDone = true;
            }
            this.shuffleCheck = true;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.amount != 0 && this.duration < 0.0f) {
            this.duration = Settings.ACTION_DUR_XFAST;
            --this.amount;
            AbstractDungeon.player.draw();
            AbstractDungeon.player.hand.refreshHandLayout();
            if (this.amount == 0) {
                this.isDone = true;
            }
        }
    }
}

