/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public abstract class AbstractGameAction {
    protected static final float DEFAULT_DURATION = 0.5f;
    protected float duration;
    protected float startDuration;
    public ActionType actionType;
    public AttackEffect attackEffect = AttackEffect.NONE;
    public DamageInfo.DamageType damageType;
    public boolean isDone = false;
    public int amount;
    public AbstractCreature target;
    public AbstractCreature source;

    protected void setValues(AbstractCreature target, DamageInfo info) {
        this.target = target;
        this.source = info.owner;
        this.amount = info.output;
        this.duration = 0.5f;
    }

    protected void setValues(AbstractCreature target, AbstractCreature source, int amount) {
        this.target = target;
        this.source = source;
        this.amount = amount;
        this.duration = 0.5f;
    }

    protected void setValues(AbstractCreature target, AbstractCreature source) {
        this.target = target;
        this.source = source;
        this.amount = 0;
        this.duration = 0.5f;
    }

    protected boolean isDeadOrEscaped(AbstractCreature target) {
        if (target.isDying || target.halfDead) {
            return true;
        }
        if (!target.isPlayer) {
            AbstractMonster m = (AbstractMonster)target;
            if (m.isEscaping) {
                return true;
            }
        }
        return false;
    }

    protected void addToBot(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    protected void addToTop(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToTop(action);
    }

    public abstract void update();

    protected void tickDuration() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    protected boolean shouldCancelAction() {
        return this.target == null || this.source != null && this.source.isDying || this.target.isDeadOrEscaped();
    }

    public static enum ActionType {
        BLOCK,
        POWER,
        CARD_MANIPULATION,
        DAMAGE,
        DEBUFF,
        DISCARD,
        DRAW,
        EXHAUST,
        HEAL,
        ENERGY,
        TEXT,
        USE,
        CLEAR_CARD_QUEUE,
        DIALOG,
        SPECIAL,
        WAIT,
        SHUFFLE,
        REDUCE_POWER;

    }

    public static enum AttackEffect {
        BLUNT_LIGHT,
        BLUNT_HEAVY,
        SLASH_DIAGONAL,
        SMASH,
        SLASH_HEAVY,
        SLASH_HORIZONTAL,
        SLASH_VERTICAL,
        NONE,
        FIRE,
        POISON,
        SHIELD,
        LIGHTNING;

    }
}

