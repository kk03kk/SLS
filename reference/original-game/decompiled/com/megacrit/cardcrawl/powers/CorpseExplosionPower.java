/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class CorpseExplosionPower
extends AbstractPower {
    public static final String POWER_ID = "CorpseExplosionPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("CorpseExplosionPower");
    public static final String NAME = CorpseExplosionPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = CorpseExplosionPower.powerStrings.DESCRIPTIONS;

    public CorpseExplosionPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 1;
        this.type = AbstractPower.PowerType.DEBUFF;
        this.updateDescription();
        this.loadRegion("cExplosion");
    }

    @Override
    public void onDeath() {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead() && this.owner.currentHealth <= 0) {
            this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(this.owner.maxHealth * this.amount, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.FIRE));
        }
    }

    @Override
    public void updateDescription() {
        this.description = this.amount == 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
    }
}

