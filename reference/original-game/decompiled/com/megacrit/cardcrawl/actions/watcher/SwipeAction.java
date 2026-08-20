/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class SwipeAction
extends AbstractGameAction {
    private DamageInfo info;
    private static final float POST_ATTACK_WAIT_DUR = 0.1f;
    private boolean skipWait = false;

    public SwipeAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = AbstractGameAction.AttackEffect.SLASH_VERTICAL;
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    @Override
    public void update() {
        if (this.shouldCancelAction() && this.info.type != DamageInfo.DamageType.THORNS) {
            this.isDone = true;
            return;
        }
        if (this.duration == Settings.ACTION_DUR_XFAST) {
            if (this.info.type != DamageInfo.DamageType.THORNS && (this.info.owner.isDying || this.info.owner.halfDead)) {
                this.isDone = true;
                return;
            }
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect, false));
        }
        this.tickDuration();
        if (this.isDone) {
            if (this.attackEffect == AbstractGameAction.AttackEffect.POISON) {
                this.target.tint.color.set(Color.CHARTREUSE.cpy());
                this.target.tint.changeColor(Color.WHITE.cpy());
            } else if (this.attackEffect == AbstractGameAction.AttackEffect.FIRE) {
                this.target.tint.color.set(Color.RED);
                this.target.tint.changeColor(Color.WHITE.cpy());
            }
            this.target.damage(this.info);
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
            if (!this.skipWait && !Settings.FAST_MODE) {
                this.addToTop(new WaitAction(0.1f));
            }
        }
    }
}

