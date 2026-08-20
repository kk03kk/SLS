/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.tempCards.Expunger;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class ConjureBladeAction
extends AbstractGameAction {
    public int[] multiDamage;
    private boolean freeToPlayOnce;
    private AbstractPlayer p;
    private int energyOnUse;

    public ConjureBladeAction(AbstractPlayer p, boolean freeToPlayOnce, int energyOnUse) {
        this.p = p;
        this.freeToPlayOnce = freeToPlayOnce;
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.energyOnUse = energyOnUse;
    }

    @Override
    public void update() {
        int effect = EnergyPanel.totalCount;
        if (this.energyOnUse != -1) {
            effect = this.energyOnUse;
        }
        if (this.p.hasRelic("Chemical X")) {
            effect += 2;
            this.p.getRelic("Chemical X").flash();
        }
        Expunger c = new Expunger();
        c.setX(effect);
        this.addToBot(new MakeTempCardInDrawPileAction(c, 1, true, true));
        if (!this.freeToPlayOnce) {
            this.p.energy.use(EnergyPanel.totalCount);
        }
        this.isDone = true;
    }
}

