/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class PummelDamageAction
extends AbstractGameAction {
    private DamageInfo info;
    private static final float DURATION = 0.01f;
    private static final float POST_ATTACK_WAIT_DUR = 0.1f;

    public PummelDamageAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = AbstractGameAction.AttackEffect.BLUNT_LIGHT;
        this.duration = 0.01f;
    }

    @Override
    public void update() {
        if (this.duration == 0.01f && this.target != null && this.target.currentHealth > 0) {
            if (this.info.type != DamageInfo.DamageType.THORNS && this.info.owner.isDying) {
                this.isDone = true;
                return;
            }
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX + MathUtils.random(-100.0f * Settings.xScale, 100.0f * Settings.xScale), this.target.hb.cY + MathUtils.random(-100.0f * Settings.scale, 100.0f * Settings.scale), this.attackEffect));
        }
        this.tickDuration();
        if (this.isDone && this.target != null && this.target.currentHealth > 0) {
            this.target.damage(this.info);
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
            this.addToTop(new WaitAction(0.1f));
        }
        if (this.target == null) {
            this.isDone = true;
        }
    }
}

