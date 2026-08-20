/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.animations;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.SpeechBubble;

public class TalkAction
extends AbstractGameAction {
    private String msg;
    private boolean used = false;
    private float bubbleDuration;
    private boolean player = false;

    public TalkAction(AbstractCreature source, String text, float duration, float bubbleDuration) {
        this.setValues(source, source);
        this.duration = Settings.FAST_MODE ? Settings.ACTION_DUR_MED : duration;
        this.msg = text;
        this.actionType = AbstractGameAction.ActionType.TEXT;
        this.bubbleDuration = bubbleDuration;
        this.player = false;
    }

    public TalkAction(AbstractCreature source, String text) {
        this(source, text, 2.0f, 2.0f);
    }

    public TalkAction(boolean isPlayer, String text, float duration, float bubbleDuration) {
        this(AbstractDungeon.player, text, duration, bubbleDuration);
        this.player = isPlayer;
    }

    @Override
    public void update() {
        if (!this.used) {
            if (this.player) {
                AbstractDungeon.effectList.add(new SpeechBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, this.bubbleDuration, this.msg, this.source.isPlayer));
            } else {
                AbstractDungeon.effectList.add(new SpeechBubble(this.source.hb.cX + this.source.dialogX, this.source.hb.cY + this.source.dialogY, this.bubbleDuration, this.msg, this.source.isPlayer));
            }
            this.used = true;
        }
        this.tickDuration();
    }
}

