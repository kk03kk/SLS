/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.TextAboveCreatureEffect;

public class TextAboveCreatureAction
extends AbstractGameAction {
    private boolean used = false;
    private String msg;

    public TextAboveCreatureAction(AbstractCreature source, TextType type) {
        if (type == TextType.STUNNED) {
            this.setValues(source, source);
            this.msg = AbstractCreature.TEXT[3];
            this.actionType = AbstractGameAction.ActionType.TEXT;
            this.duration = Settings.ACTION_DUR_FASTER;
        } else if (type == TextType.INTERRUPTED) {
            this.setValues(source, source);
            this.msg = AbstractCreature.TEXT[4];
            this.actionType = AbstractGameAction.ActionType.TEXT;
            this.duration = Settings.ACTION_DUR_FASTER;
        } else {
            this.isDone = true;
        }
    }

    public TextAboveCreatureAction(AbstractCreature source, String text) {
        this.setValues(source, source);
        this.msg = text;
        this.actionType = AbstractGameAction.ActionType.TEXT;
        this.duration = Settings.ACTION_DUR_FASTER;
    }

    @Override
    public void update() {
        if (!this.used) {
            AbstractDungeon.effectList.add(new TextAboveCreatureEffect(this.source.hb.cX - this.source.animX, this.source.hb.cY + this.target.hb.height / 2.0f, this.msg, Color.WHITE.cpy()));
            this.used = true;
        }
        this.tickDuration();
    }

    public static enum TextType {
        STUNNED,
        INTERRUPTED;

    }
}

