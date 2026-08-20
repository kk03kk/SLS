/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.SnakeDagger;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.TintEffect;

public class ReviveMonsterAction
extends AbstractGameAction {
    private boolean healingEffect;

    public ReviveMonsterAction(AbstractMonster target, AbstractCreature source, boolean healEffect) {
        this.setValues(target, source, 0);
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        if (AbstractDungeon.player.hasRelic("Philosopher's Stone")) {
            target.addPower(new StrengthPower(target, 1));
        }
        this.healingEffect = healEffect;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f && this.target instanceof AbstractMonster) {
            this.target.isDying = false;
            this.target.heal(this.target.maxHealth, this.healingEffect);
            this.target.healthBarRevivedEvent();
            ((AbstractMonster)this.target).deathTimer = 0.0f;
            ((AbstractMonster)this.target).tint = new TintEffect();
            ((AbstractMonster)this.target).tintFadeOutCalled = false;
            ((AbstractMonster)this.target).isDead = false;
            this.target.powers.clear();
            if (this.target instanceof SnakeDagger) {
                ((SnakeDagger)this.target).firstMove = true;
                ((SnakeDagger)this.target).initializeAnimation();
            }
            if (this.target instanceof AbstractMonster) {
                for (AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onSpawnMonster((AbstractMonster)this.target);
                }
            }
            ((AbstractMonster)this.target).intent = AbstractMonster.Intent.NONE;
            ((AbstractMonster)this.target).rollMove();
        }
        this.tickDuration();
    }
}

