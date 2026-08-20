/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class ShuffleAction
extends AbstractGameAction {
    private CardGroup group;
    private boolean triggerRelics;

    public ShuffleAction(CardGroup theGroup) {
        this(theGroup, false);
    }

    public ShuffleAction(CardGroup theGroup, boolean trigger) {
        this.setValues(null, null, 0);
        this.duration = 0.0f;
        this.actionType = AbstractGameAction.ActionType.SHUFFLE;
        this.group = theGroup;
        this.triggerRelics = trigger;
    }

    @Override
    public void update() {
        if (this.triggerRelics) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onShuffle();
            }
        }
        this.group.shuffle();
        this.isDone = true;
    }
}

