/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.RelicAboveCreatureEffect;

public class RelicAboveCreatureAction
extends AbstractGameAction {
    private boolean used = false;
    private AbstractRelic relic;

    public RelicAboveCreatureAction(AbstractCreature source, AbstractRelic relic) {
        this.setValues(source, source);
        this.relic = relic;
        this.actionType = AbstractGameAction.ActionType.TEXT;
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    @Override
    public void update() {
        if (!this.used) {
            AbstractDungeon.effectList.add(new RelicAboveCreatureEffect(this.source.hb.cX - this.source.animX, this.source.hb.cY + this.source.hb.height / 2.0f - this.source.animY, this.relic));
            this.used = true;
        }
        this.tickDuration();
    }
}

