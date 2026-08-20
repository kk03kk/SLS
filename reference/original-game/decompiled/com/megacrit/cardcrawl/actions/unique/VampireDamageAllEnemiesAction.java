/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect;

public class VampireDamageAllEnemiesAction
extends AbstractGameAction {
    public int[] damage;

    public VampireDamageAllEnemiesAction(AbstractCreature source, int[] amount, DamageInfo.DamageType type, AbstractGameAction.AttackEffect effect) {
        this.setValues(null, source, amount[0]);
        this.damage = amount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.damageType = type;
        this.attackEffect = effect;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            boolean playedMusic = false;
            int temp = AbstractDungeon.getCurrRoom().monsters.monsters.size();
            for (int i = 0; i < temp; ++i) {
                if (AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isDying || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).currentHealth <= 0 || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isEscaping) continue;
                if (playedMusic) {
                    AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect, true));
                    continue;
                }
                playedMusic = true;
                AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect));
            }
        }
        this.tickDuration();
        if (this.isDone) {
            for (AbstractPower p : AbstractDungeon.player.powers) {
                p.onDamageAllEnemies(this.damage);
            }
            int healAmount = 0;
            for (int i = 0; i < AbstractDungeon.getCurrRoom().monsters.monsters.size(); ++i) {
                AbstractMonster target = AbstractDungeon.getCurrRoom().monsters.monsters.get(i);
                if (target.isDying || target.currentHealth <= 0 || target.isEscaping) continue;
                target.damage(new DamageInfo(this.source, this.damage[i], this.damageType));
                if (target.lastDamageTaken <= 0) continue;
                healAmount += target.lastDamageTaken;
                for (int j = 0; j < target.lastDamageTaken / 2 && j < 10; ++j) {
                    this.addToBot(new VFXAction(new FlyingOrbEffect(target.hb.cX, target.hb.cY)));
                }
            }
            if (healAmount > 0) {
                if (!Settings.FAST_MODE) {
                    this.addToBot(new WaitAction(0.3f));
                }
                this.addToBot(new HealAction(this.source, this.source, healAmount));
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
            this.addToTop(new WaitAction(0.1f));
        }
    }
}

