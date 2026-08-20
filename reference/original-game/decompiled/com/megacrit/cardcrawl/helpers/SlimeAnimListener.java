/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Event;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SlimeAnimListener
implements AnimationState.AnimationStateListener {
    @Override
    public void event(int trackIndex, Event event) {
        if (!AbstractDungeon.isScreenUp) {
            int roll = MathUtils.random(3);
            if (roll == 0) {
                CardCrawlGame.sound.play("SLIME_BLINK_1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("SLIME_BLINK_2");
            } else if (roll == 2) {
                CardCrawlGame.sound.play("SLIME_BLINK_3");
            } else {
                CardCrawlGame.sound.play("SLIME_BLINK_4");
            }
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

