/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ScreenShake;

public class ShakeScreenAction
extends AbstractGameAction {
    private float startDur;
    ScreenShake.ShakeDur shakeDur;
    ScreenShake.ShakeIntensity intensity;

    public ShakeScreenAction(float duration, ScreenShake.ShakeDur dur, ScreenShake.ShakeIntensity intensity) {
        this.duration = duration;
        this.startDur = duration;
        this.shakeDur = dur;
        this.intensity = intensity;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (this.duration == this.startDur) {
            CardCrawlGame.screenShake.shake(this.intensity, this.shakeDur, false);
        }
        this.tickDuration();
    }
}

