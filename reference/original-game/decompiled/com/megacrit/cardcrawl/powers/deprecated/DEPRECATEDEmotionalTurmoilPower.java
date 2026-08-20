/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.deprecated;

import com.megacrit.cardcrawl.actions.deprecated.DEPRECATEDRandomStanceAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DEPRECATEDEmotionalTurmoilPower
extends AbstractPower {
    public static final String POWER_ID = "EmotionalTurmoilPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("EmotionalTurmoilPower");

    public DEPRECATEDEmotionalTurmoilPower(AbstractCreature owner) {
        this.name = DEPRECATEDEmotionalTurmoilPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.updateDescription();
        this.loadRegion("draw");
        this.type = AbstractPower.PowerType.BUFF;
        this.isTurnBased = false;
    }

    @Override
    public void atStartOfTurnPostDraw() {
        this.addToBot(new DEPRECATEDRandomStanceAction());
    }

    @Override
    public void updateDescription() {
        this.description = DEPRECATEDEmotionalTurmoilPower.powerStrings.DESCRIPTIONS[0];
    }
}

