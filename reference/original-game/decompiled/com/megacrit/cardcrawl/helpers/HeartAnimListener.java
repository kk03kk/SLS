/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Event;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;

public class HeartAnimListener
implements AnimationState.AnimationStateListener {
    @Override
    public void event(int trackIndex, Event event) {
        if (!AbstractDungeon.isScreenUp && event.getData().getName().equals("maxbeat")) {
            CardCrawlGame.sound.playAV("HEART_SIMPLE", MathUtils.random(-0.05f, 0.05f), 0.75f);
            CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    @Override
    public void complete(int trackIndex, int loopCount) {
    }

    @Override
    public void start(int trackIndex) {
    }

    @Override
    public void end(int trackIndex) {
    }
}

