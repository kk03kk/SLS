/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.animations;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

public class SetAnimationAction
extends AbstractGameAction {
    private boolean called = false;
    private String animation;

    public SetAnimationAction(AbstractCreature owner, String animationName) {
        this.setValues(null, owner, 0);
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.animation = animationName;
    }

    @Override
    public void update() {
        if (!this.called) {
            this.source.state.setAnimation(0, this.animation, false);
            this.called = true;
            this.source.state.addAnimation(0, "idle", true, 0.0f);
        }
        this.tickDuration();
    }
}

