/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TransformCardInHandAction
extends AbstractGameAction {
    private AbstractCard replacement;
    private int handIndex;

    public TransformCardInHandAction(int index, AbstractCard replacement) {
        this.handIndex = index;
        this.replacement = replacement;
        this.startDuration = Settings.FAST_MODE ? 0.05f : 0.15f;
        this.duration = this.startDuration;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            AbstractCard target = AbstractDungeon.player.hand.group.get(this.handIndex);
            this.replacement.current_x = target.current_x;
            this.replacement.current_y = target.current_y;
            this.replacement.target_x = target.target_x;
            this.replacement.target_y = target.target_y;
            this.replacement.drawScale = 1.0f;
            this.replacement.targetDrawScale = target.targetDrawScale;
            this.replacement.angle = target.angle;
            this.replacement.targetAngle = target.targetAngle;
            this.replacement.superFlash(Color.WHITE.cpy());
            AbstractDungeon.player.hand.group.set(this.handIndex, this.replacement);
            AbstractDungeon.player.hand.glowCheck();
        }
        this.tickDuration();
    }
}

