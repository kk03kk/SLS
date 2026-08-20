/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class VulnerablePower
extends AbstractPower {
    public static final String POWER_ID = "Vulnerable";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Vulnerable");
    public static final String NAME = VulnerablePower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = VulnerablePower.powerStrings.DESCRIPTIONS;
    private boolean justApplied = false;
    private static final float EFFECTIVENESS = 1.5f;
    private static final int EFFECTIVENESS_STRING = 50;

    public VulnerablePower(AbstractCreature owner, int amount, boolean isSourceMonster) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("vulnerable");
        if (AbstractDungeon.actionManager.turnHasEnded && isSourceMonster) {
            this.justApplied = true;
        }
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        if (this.justApplied) {
            this.justApplied = false;
            return;
        }
        if (this.amount == 0) {
            this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, POWER_ID));
        } else {
            this.addToBot(new ReducePowerAction(this.owner, this.owner, POWER_ID, 1));
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? (this.owner != null && this.owner.isPlayer && AbstractDungeon.player.hasRelic("Odd Mushroom") ? DESCRIPTIONS[0] + 25 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : (this.owner != null && !this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Frog") ? DESCRIPTIONS[0] + 75 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2] : DESCRIPTIONS[0] + 50 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2])) : (this.owner != null && this.owner.isPlayer && AbstractDungeon.player.hasRelic("Odd Mushroom") ? DESCRIPTIONS[0] + 25 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3] : (this.owner != null && !this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Frog") ? DESCRIPTIONS[0] + 75 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3] : DESCRIPTIONS[0] + 50 + DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[3]));
    }

    @Override
    public float atDamageReceive(float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL) {
            if (this.owner.isPlayer && AbstractDungeon.player.hasRelic("Odd Mushroom")) {
                return damage * 1.25f;
            }
            if (this.owner != null && !this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Frog")) {
                return damage * 1.75f;
            }
            return damage * 1.5f;
        }
        return damage;
    }
}

