/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class DamageAllButOneEnemyAction
extends AbstractGameAction {
    public int[] damage;
    private boolean firstFrame = true;

    public DamageAllButOneEnemyAction(AbstractCreature source, AbstractCreature target, int[] amount, DamageInfo.DamageType type, AbstractGameAction.AttackEffect effect, boolean isFast) {
        this.setValues(target, source, amount[0]);
        this.damage = amount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.damageType = type;
        this.attackEffect = effect;
        this.duration = isFast ? Settings.ACTION_DUR_XFAST : Settings.ACTION_DUR_FAST;
    }

    public DamageAllButOneEnemyAction(AbstractCreature source, AbstractCreature target, int[] amount, DamageInfo.DamageType type, AbstractGameAction.AttackEffect effect) {
        this(source, target, amount, type, effect, false);
    }

    @Override
    public void update() {
        if (this.firstFrame) {
            boolean playedMusic = false;
            int temp = AbstractDungeon.getCurrRoom().monsters.monsters.size();
            for (int i = 0; i < temp; ++i) {
                if (AbstractDungeon.getCurrRoom().monsters.monsters.get(i) == this.target || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isDying || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).currentHealth <= 0 || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isEscaping) continue;
                if (playedMusic) {
                    AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect, true));
                    continue;
                }
                playedMusic = true;
                AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect));
            }
            this.firstFrame = false;
        }
        this.tickDuration();
        if (this.isDone) {
            for (AbstractPower p : AbstractDungeon.player.powers) {
                p.onDamageAllEnemies(this.damage);
            }
            int temp = AbstractDungeon.getCurrRoom().monsters.monsters.size();
            for (int i = 0; i < temp; ++i) {
                if (AbstractDungeon.getCurrRoom().monsters.monsters.get(i) != this.target && !AbstractDungeon.getCurrRoom().monsters.monsters.get(i).isDeadOrEscaped()) {
                    if (this.attackEffect == AbstractGameAction.AttackEffect.POISON) {
                        AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.color = Color.CHARTREUSE.cpy();
                        AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.changeColor(Color.WHITE.cpy());
                    } else if (this.attackEffect == AbstractGameAction.AttackEffect.FIRE) {
                        AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.color = Color.RED.cpy();
                        AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.changeColor(Color.WHITE.cpy());
                    }
                    DamageInfo info = new DamageInfo(this.source, this.damage[i], this.damageType);
                    info.applyPowers(this.source, AbstractDungeon.getCurrRoom().monsters.monsters.get(i));
                    AbstractDungeon.getCurrRoom().monsters.monsters.get(i).damage(info);
                }
                if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                    AbstractDungeon.actionManager.clearPostCombatActions();
                }
                this.addToTop(new WaitAction(0.1f));
            }
        }
    }
}

