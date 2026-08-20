/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class GainBlockAction
extends AbstractGameAction {
    private static final float DUR = 0.25f;

    public GainBlockAction(AbstractCreature target, int amount) {
        this.target = target;
        this.amount = amount;
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.duration = 0.25f;
        this.startDuration = 0.25f;
    }

    public GainBlockAction(AbstractCreature target, AbstractCreature source, int amount) {
        this.setValues(target, source, amount);
        this.actionType = AbstractGameAction.ActionType.BLOCK;
        this.duration = 0.25f;
        this.startDuration = 0.25f;
    }

    public GainBlockAction(AbstractCreature target, int amount, boolean superFast) {
        this(target, amount);
        if (superFast) {
            this.duration = this.startDuration = Settings.ACTION_DUR_XFAST;
        }
    }

    public GainBlockAction(AbstractCreature target, AbstractCreature source, int amount, boolean superFast) {
        this(target, source, amount);
        if (superFast) {
            this.duration = this.startDuration = Settings.ACTION_DUR_XFAST;
        }
    }

    @Override
    public void update() {
        if (!this.target.isDying && !this.target.isDead && this.duration == this.startDuration) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.SHIELD));
            this.target.addBlock(this.amount);
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                c.applyPowers();
            }
        }
        this.tickDuration();
    }
}

