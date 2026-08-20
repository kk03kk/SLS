/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.defect.ImpulseAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class EmotionChip
extends AbstractRelic {
    public static final String ID = "Emotion Chip";

    public EmotionChip() {
        super(ID, "emotionChip.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
        this.pulse = false;
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atTurnStart() {
        if (this.pulse) {
            this.pulse = false;
            this.flash();
            this.addToBot(new ImpulseAction());
        }
    }

    @Override
    public void wasHPLost(int damageAmount) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && damageAmount > 0) {
            this.flash();
            if (!this.pulse) {
                this.beginPulse();
                this.pulse = true;
            }
        }
    }

    @Override
    public void onVictory() {
        this.pulse = false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new EmotionChip();
    }
}

