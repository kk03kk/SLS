/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.deprecated.DEPRECATEDCrescentKick;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CrescentKickAction
extends AbstractGameAction {
    private DEPRECATEDCrescentKick card;

    public CrescentKickAction(AbstractPlayer p, DEPRECATEDCrescentKick card) {
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.card = card;
        this.target = p;
    }

    @Override
    public void update() {
        if (this.card.hadVigor && this.target != null) {
            this.addToTop(new DrawCardAction(AbstractDungeon.player, 1));
            this.addToTop(new GainEnergyAction(1));
        }
        this.isDone = true;
    }
}

