/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class CombustPower
extends AbstractPower {
    public static final String POWER_ID = "Combust";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Combust");
    public static final String NAME = CombustPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = CombustPower.powerStrings.DESCRIPTIONS;
    private int hpLoss;

    public CombustPower(AbstractCreature owner, int hpLoss, int damageAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = damageAmount;
        this.hpLoss = hpLoss;
        this.updateDescription();
        this.loadRegion("combust");
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            this.addToBot(new LoseHPAction(this.owner, this.owner, this.hpLoss, AbstractGameAction.AttackEffect.FIRE));
            this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(this.amount, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.FIRE));
        }
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        ++this.hpLoss;
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.hpLoss + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }
}

