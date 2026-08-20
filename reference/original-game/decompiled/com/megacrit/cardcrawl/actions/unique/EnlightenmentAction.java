/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class EnlightenmentAction
extends AbstractGameAction {
    private AbstractPlayer p;
    private boolean forCombat = false;

    public EnlightenmentAction(boolean forRestOfCombat) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
        this.forCombat = forRestOfCombat;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (AbstractCard c : this.p.hand.group) {
                if (c.costForTurn > 1) {
                    c.costForTurn = 1;
                    c.isCostModifiedForTurn = true;
                }
                if (!this.forCombat || c.cost <= 1) continue;
                c.cost = 1;
                c.isCostModified = true;
            }
        }
        this.tickDuration();
    }
}

