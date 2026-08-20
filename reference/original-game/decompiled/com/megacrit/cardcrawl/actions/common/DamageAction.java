/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class DamageAction
extends AbstractGameAction {
    private DamageInfo info;
    private int goldAmount = 0;
    private static final float DURATION = 0.1f;
    private static final float POST_ATTACK_WAIT_DUR = 0.1f;
    private boolean skipWait = false;
    private boolean muteSfx = false;

    public DamageAction(AbstractCreature target, DamageInfo info, AbstractGameAction.AttackEffect effect) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = effect;
        this.duration = 0.1f;
    }

    public DamageAction(AbstractCreature target, DamageInfo info, int stealGoldAmount) {
        this(target, info, AbstractGameAction.AttackEffect.SLASH_DIAGONAL);
        this.goldAmount = stealGoldAmount;
    }

    public DamageAction(AbstractCreature target, DamageInfo info) {
        this(target, info, AbstractGameAction.AttackEffect.NONE);
    }

    public DamageAction(AbstractCreature target, DamageInfo info, boolean superFast) {
        this(target, info, AbstractGameAction.AttackEffect.NONE);
        this.skipWait = superFast;
    }

    public DamageAction(AbstractCreature target, DamageInfo info, AbstractGameAction.AttackEffect effect, boolean superFast) {
        this(target, info, effect);
        this.skipWait = superFast;
    }

    public DamageAction(AbstractCreature target, DamageInfo info, AbstractGameAction.AttackEffect effect, boolean superFast, boolean muteSfx) {
        this(target, info, effect, superFast);
        this.muteSfx = muteSfx;
    }

    @Override
    public void update() {
        if (this.shouldCancelAction() && this.info.type != DamageInfo.DamageType.THORNS) {
            this.isDone = true;
            return;
        }
        if (this.duration == 0.1f) {
            if (this.info.type != DamageInfo.DamageType.THORNS && (this.info.owner.isDying || this.info.owner.halfDead)) {
                this.isDone = true;
                return;
            }
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect, this.muteSfx));
            if (this.goldAmount != 0) {
                this.stealGold();
            }
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

    private void stealGold() {
        if (this.target.gold == 0) {
            return;
        }
        CardCrawlGame.sound.play("GOLD_JINGLE");
        if (this.target.gold < this.goldAmount) {
            this.goldAmount = this.target.gold;
        }
        this.target.gold -= this.goldAmount;
        for (int i = 0; i < this.goldAmount; ++i) {
            if (this.source.isPlayer) {
                AbstractDungeon.effectList.add(new GainPennyEffect(this.target.hb.cX, this.target.hb.cY));
                continue;
            }
            AbstractDungeon.effectList.add(new GainPennyEffect(this.source, this.target.hb.cX, this.target.hb.cY, this.source.hb.cX, this.source.hb.cY, false));
        }
    }
}

