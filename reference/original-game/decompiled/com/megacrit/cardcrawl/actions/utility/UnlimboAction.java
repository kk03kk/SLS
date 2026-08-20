/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;

public class UnlimboAction
extends AbstractGameAction {
    private AbstractCard card;
    private boolean exhaust;

    public UnlimboAction(AbstractCard card, boolean exhaust) {
        this.duration = Settings.ACTION_DUR_XFAST;
        this.card = card;
        this.exhaust = exhaust;
    }

    public UnlimboAction(AbstractCard card) {
        this(card, false);
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_XFAST) {
            if (!this.exhaust) {
                // empty if block
            }
            AbstractDungeon.player.limbo.removeCard(this.card);
            if (this.exhaust) {
                AbstractDungeon.effectList.add(new ExhaustCardEffect(this.card));
            }
            this.isDone = true;
        }
    }
}

