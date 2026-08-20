/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.WallopEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class WallopAction
extends AbstractGameAction {
    private DamageInfo info;

    public WallopAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.duration = this.startDuration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.shouldCancelAction()) {
            this.isDone = true;
            return;
        }
        this.tickDuration();
        if (this.isDone) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.BLUNT_HEAVY, false));
            this.target.damage(this.info);
            if (this.target.lastDamageTaken > 0) {
                this.addToTop(new GainBlockAction(this.source, this.target.lastDamageTaken));
                if (this.target.hb != null) {
                    this.addToTop(new VFXAction(new WallopEffect(this.target.lastDamageTaken, this.target.hb.cX, this.target.hb.cY)));
                }
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            } else {
                this.addToTop(new WaitAction(0.1f));
            }
        }
    }
}

