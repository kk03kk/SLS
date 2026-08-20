/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class MalleablePower
extends AbstractPower {
    public static final String POWER_ID = "Malleable";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Malleable");
    public static final String NAME = MalleablePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = MalleablePower.powerStrings.DESCRIPTIONS;
    private static final int STARTING_BLOCK = 3;
    private int basePower;

    public MalleablePower(AbstractCreature owner) {
        this(owner, 3);
    }

    public MalleablePower(AbstractCreature owner, int amt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amt;
        this.basePower = amt;
        this.updateDescription();
        this.loadRegion("malleable");
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] + NAME + DESCRIPTIONS[2] + this.basePower + DESCRIPTIONS[3];
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (this.owner.isPlayer) {
            return;
        }
        this.amount = this.basePower;
        this.updateDescription();
    }

    @Override
    public void atEndOfRound() {
        if (!this.owner.isPlayer) {
            return;
        }
        this.amount = this.basePower;
        this.updateDescription();
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (damageAmount < this.owner.currentHealth && damageAmount > 0 && info.owner != null && info.type == DamageInfo.DamageType.NORMAL && info.type != DamageInfo.DamageType.HP_LOSS) {
            this.flash();
            if (this.owner.isPlayer) {
                this.addToTop(new GainBlockAction(this.owner, this.owner, this.amount));
            } else {
                this.addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
            }
            ++this.amount;
            this.updateDescription();
        }
        return damageAmount;
    }

    @Override
    public void stackPower(int stackAmount) {
        this.amount += stackAmount;
        this.basePower += stackAmount;
    }
}

