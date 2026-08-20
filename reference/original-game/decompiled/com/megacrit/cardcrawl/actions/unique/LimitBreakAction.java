/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class LimitBreakAction
extends AbstractGameAction {
    private AbstractPlayer p;

    public LimitBreakAction() {
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_XFAST && this.p.hasPower("Strength")) {
            int strAmt = this.p.getPower((String)"Strength").amount;
            this.addToTop(new ApplyPowerAction(this.p, this.p, new StrengthPower(this.p, strAmt), strAmt));
        }
        this.tickDuration();
    }
}

