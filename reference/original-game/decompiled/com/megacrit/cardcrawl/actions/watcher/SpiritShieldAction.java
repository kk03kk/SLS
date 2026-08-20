/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SpiritShieldAction
extends AbstractGameAction {
    private int blockPerCard;

    public SpiritShieldAction(int blockPerCard) {
        this.blockPerCard = blockPerCard;
    }

    @Override
    public void update() {
        if (!AbstractDungeon.player.hand.isEmpty()) {
            this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, AbstractDungeon.player.hand.group.size() * this.blockPerCard));
        }
        this.isDone = true;
    }
}

