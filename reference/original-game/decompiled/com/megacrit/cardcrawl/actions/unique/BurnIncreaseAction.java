/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;

public class BurnIncreaseAction
extends AbstractGameAction {
    private static final float DURATION = 3.0f;
    private boolean gotBurned = false;

    public BurnIncreaseAction() {
        this.duration = 3.0f;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (this.duration == 3.0f) {
            for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
                if (!(c instanceof Burn)) continue;
                c.upgrade();
            }
            for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
                if (!(c instanceof Burn)) continue;
                c.upgrade();
            }
        }
        if (this.duration < 1.5f && !this.gotBurned) {
            AbstractCard c;
            this.gotBurned = true;
            Burn b = new Burn();
            b.upgrade();
            AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(b));
            c = new Burn();
            ((Burn)c).upgrade();
            AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(c));
            Burn d = new Burn();
            d.upgrade();
            AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(d));
        }
        this.tickDuration();
    }
}

