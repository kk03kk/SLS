/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MarkPower
extends AbstractPower {
    public static final String POWER_ID = "PathToVictoryPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("PathToVictoryPower");

    public MarkPower(AbstractCreature owner, int amt) {
        this.name = MarkPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.updateDescription();
        this.loadRegion("pressure_points");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void updateDescription() {
        this.description = MarkPower.powerStrings.DESCRIPTIONS[0] + this.amount + MarkPower.powerStrings.DESCRIPTIONS[1];
    }

    @Override
    public void triggerMarks(AbstractCard card) {
        if (card.cardID.equals("PathToVictory")) {
            this.addToBot(new LoseHPAction(this.owner, null, this.amount, AbstractGameAction.AttackEffect.FIRE));
        }
    }
}

