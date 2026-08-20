/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.animations;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class VFXAction
extends AbstractGameAction {
    private AbstractGameEffect effect;
    private float startingDuration;
    private boolean isTopLevelEffect = false;

    public VFXAction(AbstractGameEffect effect) {
        this(null, effect, 0.0f);
    }

    public VFXAction(AbstractGameEffect effect, float duration) {
        this(null, effect, duration);
    }

    public VFXAction(AbstractCreature source, AbstractGameEffect effect, float duration) {
        this.setValues(source, source);
        this.effect = effect;
        this.duration = duration;
        this.startingDuration = duration;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    public VFXAction(AbstractCreature source, AbstractGameEffect effect, float duration, boolean topLevel) {
        this.setValues(source, source);
        this.effect = effect;
        this.duration = duration;
        this.startingDuration = duration;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.isTopLevelEffect = topLevel;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            if (this.isTopLevelEffect) {
                AbstractDungeon.topLevelEffects.add(this.effect);
            } else {
                AbstractDungeon.effectList.add(this.effect);
            }
        }
        this.tickDuration();
    }
}

