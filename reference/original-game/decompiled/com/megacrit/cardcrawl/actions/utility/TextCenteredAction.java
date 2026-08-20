/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.TextCenteredEffect;

public class TextCenteredAction
extends AbstractGameAction {
    private boolean used = false;
    private String msg;
    private static final float DURATION = 2.0f;

    public TextCenteredAction(AbstractCreature source, String text) {
        this.setValues(source, source);
        this.msg = text;
        this.duration = 2.0f;
        this.actionType = AbstractGameAction.ActionType.TEXT;
    }

    @Override
    public void update() {
        if (!this.used) {
            AbstractDungeon.effectList.add(new TextCenteredEffect(this.msg));
            this.used = true;
        }
        this.tickDuration();
    }
}

