/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.Timer;

public class DragScrollListener
extends DragListener {
    private ScrollPane scroll;
    private Timer.Task scrollUp;
    private Timer.Task scrollDown;
    Interpolation interpolation = Interpolation.exp5In;
    float minSpeed = 15.0f;
    float maxSpeed = 75.0f;
    float tickSecs = 0.05f;
    long startTime;
    long rampTime = 1750L;

    public DragScrollListener(final ScrollPane scroll) {
        this.scroll = scroll;
        this.scrollUp = new Timer.Task(){

            @Override
            public void run() {
                scroll.setScrollY(scroll.getScrollY() - DragScrollListener.this.getScrollPixels());
            }
        };
        this.scrollDown = new Timer.Task(){

            @Override
            public void run() {
                scroll.setScrollY(scroll.getScrollY() + DragScrollListener.this.getScrollPixels());
            }
        };
    }

    public void setup(float minSpeedPixels, float maxSpeedPixels, float tickSecs, float rampSecs) {
        this.minSpeed = minSpeedPixels;
        this.maxSpeed = maxSpeedPixels;
        this.tickSecs = tickSecs;
        this.rampTime = (long)(rampSecs * 1000.0f);
    }

    float getScrollPixels() {
        return this.interpolation.apply(this.minSpeed, this.maxSpeed, Math.min(1.0f, (float)(System.currentTimeMillis() - this.startTime) / (float)this.rampTime));
    }

    @Override
    public void drag(InputEvent event, float x, float y, int pointer) {
        if (x >= 0.0f && x < this.scroll.getWidth()) {
            if (y >= this.scroll.getHeight()) {
                this.scrollDown.cancel();
                if (!this.scrollUp.isScheduled()) {
                    this.startTime = System.currentTimeMillis();
                    Timer.schedule(this.scrollUp, this.tickSecs, this.tickSecs);
                }
                return;
            }
            if (y < 0.0f) {
                this.scrollUp.cancel();
                if (!this.scrollDown.isScheduled()) {
                    this.startTime = System.currentTimeMillis();
                    Timer.schedule(this.scrollDown, this.tickSecs, this.tickSecs);
                }
                return;
            }
        }
        this.scrollUp.cancel();
        this.scrollDown.cancel();
    }

    @Override
    public void dragStop(InputEvent event, float x, float y, int pointer) {
        this.scrollUp.cancel();
        this.scrollDown.cancel();
    }
}

