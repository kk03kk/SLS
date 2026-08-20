/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.TimeUtils;

public class ClickListener
extends InputListener {
    public static float visualPressedDuration = 0.1f;
    private float tapSquareSize = 14.0f;
    private float touchDownX = -1.0f;
    private float touchDownY = -1.0f;
    private int pressedPointer = -1;
    private int pressedButton = -1;
    private int button;
    private boolean pressed;
    private boolean over;
    private boolean cancelled;
    private long visualPressedTime;
    private long tapCountInterval = 400000000L;
    private int tapCount;
    private long lastTapTime;

    public ClickListener() {
    }

    public ClickListener(int button) {
        this.button = button;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (this.pressed) {
            return false;
        }
        if (pointer == 0 && this.button != -1 && button != this.button) {
            return false;
        }
        this.pressed = true;
        this.pressedPointer = pointer;
        this.pressedButton = button;
        this.touchDownX = x;
        this.touchDownY = y;
        this.visualPressedTime = TimeUtils.millis() + (long)(visualPressedDuration * 1000.0f);
        return true;
    }

    @Override
    public void touchDragged(InputEvent event, float x, float y, int pointer) {
        if (pointer != this.pressedPointer || this.cancelled) {
            return;
        }
        this.pressed = this.isOver(event.getListenerActor(), x, y);
        if (this.pressed && pointer == 0 && this.button != -1 && !Gdx.input.isButtonPressed(this.button)) {
            this.pressed = false;
        }
        if (!this.pressed) {
            this.invalidateTapSquare();
        }
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        if (pointer == this.pressedPointer) {
            if (!this.cancelled) {
                boolean touchUpOver = this.isOver(event.getListenerActor(), x, y);
                if (touchUpOver && pointer == 0 && this.button != -1 && button != this.button) {
                    touchUpOver = false;
                }
                if (touchUpOver) {
                    long time = TimeUtils.nanoTime();
                    if (time - this.lastTapTime > this.tapCountInterval) {
                        this.tapCount = 0;
                    }
                    ++this.tapCount;
                    this.lastTapTime = time;
                    this.clicked(event, x, y);
                }
            }
            this.pressed = false;
            this.pressedPointer = -1;
            this.pressedButton = -1;
            this.cancelled = false;
        }
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        if (pointer == -1 && !this.cancelled) {
            this.over = true;
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        if (pointer == -1 && !this.cancelled) {
            this.over = false;
        }
    }

    public void cancel() {
        if (this.pressedPointer == -1) {
            return;
        }
        this.cancelled = true;
        this.pressed = false;
    }

    public void clicked(InputEvent event, float x, float y) {
    }

    public boolean isOver(Actor actor, float x, float y) {
        Actor hit = actor.hit(x, y, true);
        if (hit == null || !hit.isDescendantOf(actor)) {
            return this.inTapSquare(x, y);
        }
        return true;
    }

    public boolean inTapSquare(float x, float y) {
        if (this.touchDownX == -1.0f && this.touchDownY == -1.0f) {
            return false;
        }
        return Math.abs(x - this.touchDownX) < this.tapSquareSize && Math.abs(y - this.touchDownY) < this.tapSquareSize;
    }

    public boolean inTapSquare() {
        return this.touchDownX != -1.0f;
    }

    public void invalidateTapSquare() {
        this.touchDownX = -1.0f;
        this.touchDownY = -1.0f;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    public boolean isVisualPressed() {
        if (this.pressed) {
            return true;
        }
        if (this.visualPressedTime <= 0L) {
            return false;
        }
        if (this.visualPressedTime > TimeUtils.millis()) {
            return true;
        }
        this.visualPressedTime = 0L;
        return false;
    }

    public boolean isOver() {
        return this.over || this.pressed;
    }

    public void setTapSquareSize(float halfTapSquareSize) {
        this.tapSquareSize = halfTapSquareSize;
    }

    public float getTapSquareSize() {
        return this.tapSquareSize;
    }

    public void setTapCountInterval(float tapCountInterval) {
        this.tapCountInterval = (long)(tapCountInterval * 1.0E9f);
    }

    public int getTapCount() {
        return this.tapCount;
    }

    public void setTapCount(int tapCount) {
        this.tapCount = tapCount;
    }

    public float getTouchDownX() {
        return this.touchDownX;
    }

    public float getTouchDownY() {
        return this.touchDownY;
    }

    public int getPressedButton() {
        return this.pressedButton;
    }

    public int getPressedPointer() {
        return this.pressedPointer;
    }

    public int getButton() {
        return this.button;
    }

    public void setButton(int button) {
        this.button = button;
    }
}

