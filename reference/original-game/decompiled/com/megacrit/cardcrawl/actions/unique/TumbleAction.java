/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DiscardSpecificCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TumbleAction
extends AbstractGameAction {
    public TumbleAction(AbstractCreature source, int amount) {
        this.source = source;
        this.amount = amount;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (c.type != AbstractCard.CardType.ATTACK) continue;
                this.addToTop(new GainBlockAction(this.source, this.source, this.amount));
                this.addToTop(new DiscardSpecificCardAction(c));
            }
        }
        this.tickDuration();
    }
}

