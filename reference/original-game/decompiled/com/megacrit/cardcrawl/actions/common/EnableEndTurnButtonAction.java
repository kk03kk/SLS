/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EnableEndTurnButtonAction
extends AbstractGameAction {
    @Override
    public void update() {
        AbstractDungeon.overlayMenu.endTurnButton.enable();
        this.isDone = true;
    }
}

