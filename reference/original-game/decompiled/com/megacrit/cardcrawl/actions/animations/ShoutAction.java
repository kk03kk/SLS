/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.animations;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.MegaSpeechBubble;

public class ShoutAction
extends AbstractGameAction {
    private String msg;
    private boolean used = false;
    private float bubbleDuration;
    private static final float DEFAULT_BUBBLE_DUR = 3.0f;

    public ShoutAction(AbstractCreature source, String text, float duration, float bubbleDuration) {
        this.setValues(source, source);
        this.duration = Settings.FAST_MODE ? Settings.ACTION_DUR_MED : duration;
        this.msg = text;
        this.actionType = AbstractGameAction.ActionType.TEXT;
        this.bubbleDuration = bubbleDuration;
    }

    public ShoutAction(AbstractCreature source, String text) {
        this(source, text, 0.5f, 3.0f);
    }

    @Override
    public void update() {
        if (!this.used) {
            AbstractDungeon.effectList.add(new MegaSpeechBubble(this.source.hb.cX + this.source.dialogX, this.source.hb.cY + this.source.dialogY, this.bubbleDuration, this.msg, this.source.isPlayer));
            this.used = true;
        }
        this.tickDuration();
    }
}

