/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class BrillianceAction
extends AbstractGameAction {
    public int[] multiDamage;
    private boolean freeToPlayOnce = false;
    private AbstractPlayer p;
    private int energyOnUse = -1;

    public BrillianceAction(AbstractPlayer p, int amount, boolean freeToPlayOnce, int energyOnUse) {
        this.amount = amount;
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
        if (effect > 0) {
            this.addToBot(new HealAction(this.p, this.p, effect * this.amount));
            if (!this.freeToPlayOnce) {
                this.p.energy.use(EnergyPanel.totalCount);
            }
        }
        this.isDone = true;
    }
}

