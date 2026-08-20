/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class DEPRECATEDDisciplinePower
extends AbstractPower {
    public static final String POWER_ID = "DisciplinePower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("DisciplinePower");

    public DEPRECATEDDisciplinePower(AbstractCreature owner) {
        this.name = DEPRECATEDDisciplinePower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("no_stance");
        this.type = AbstractPower.PowerType.BUFF;
        this.amount = -1;
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (EnergyPanel.totalCount > 0) {
            this.amount = EnergyPanel.totalCount;
            this.fontScale = 8.0f;
        }
    }

    @Override
    public void atStartOfTurn() {
        if (this.amount != -1) {
            this.addToTop(new DrawCardAction(this.amount));
            this.amount = -1;
            this.fontScale = 8.0f;
            this.flash();
        }
    }

    @Override
    public void updateDescription() {
        this.description = DEPRECATEDDisciplinePower.powerStrings.DESCRIPTIONS[0];
    }
}

