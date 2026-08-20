/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class HandCheckAction
extends AbstractGameAction {
    private AbstractPlayer player = AbstractDungeon.player;

    @Override
    public void update() {
        this.player.hand.applyPowers();
        this.player.hand.glowCheck();
        this.isDone = true;
    }
}

