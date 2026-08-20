/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.MoveNameEffect;

public class ShowMoveNameAction
extends AbstractGameAction {
    private String msg;

    public ShowMoveNameAction(AbstractMonster source, String msg) {
        this.setValues((AbstractCreature)source, source);
        this.msg = msg;
        this.actionType = AbstractGameAction.ActionType.TEXT;
    }

    public ShowMoveNameAction(AbstractMonster source) {
        this.setValues((AbstractCreature)source, source);
        this.msg = source.moveName;
        source.moveName = null;
        this.actionType = AbstractGameAction.ActionType.TEXT;
    }

    @Override
    public void update() {
        if (this.source != null && !this.source.isDying) {
            AbstractDungeon.effectList.add(new MoveNameEffect(this.source.hb.cX - this.source.animX, this.source.hb.cY + this.source.hb.height / 2.0f, this.msg));
        }
        this.isDone = true;
    }
}

