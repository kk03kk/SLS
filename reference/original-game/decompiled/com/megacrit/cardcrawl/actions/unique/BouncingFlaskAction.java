/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.vfx.combat.PotionBounceEffect;

public class BouncingFlaskAction
extends AbstractGameAction {
    private static final float DURATION = 0.01f;
    private static final float POST_ATTACK_WAIT_DUR = 0.1f;
    private int numTimes;
    private int amount;

    public BouncingFlaskAction(AbstractCreature target, int amount, int numTimes) {
        this.target = target;
        this.actionType = AbstractGameAction.ActionType.DEBUFF;
        this.duration = 0.01f;
        this.numTimes = numTimes;
        this.amount = amount;
    }

    @Override
    public void update() {
        if (this.target == null) {
            this.isDone = true;
            return;
        }
        if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
            AbstractDungeon.actionManager.clearPostCombatActions();
            this.isDone = true;
            return;
        }
        if (this.numTimes > 1 && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            --this.numTimes;
            AbstractMonster randomMonster = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
            this.addToTop(new BouncingFlaskAction(randomMonster, this.amount, this.numTimes));
            this.addToTop(new VFXAction(new PotionBounceEffect(this.target.hb.cX, this.target.hb.cY, randomMonster.hb.cX, randomMonster.hb.cY), 0.4f));
        }
        if (this.target.currentHealth > 0) {
            this.addToTop(new ApplyPowerAction(this.target, AbstractDungeon.player, new PoisonPower(this.target, AbstractDungeon.player, this.amount), this.amount, true, AbstractGameAction.AttackEffect.POISON));
            this.addToTop(new WaitAction(0.1f));
        }
        this.isDone = true;
    }
}

