/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Pool;

public class Event
implements Pool.Poolable {
    private Stage stage;
    private Actor targetActor;
    private Actor listenerActor;
    private boolean capture;
    private boolean bubbles = true;
    private boolean handled;
    private boolean stopped;
    private boolean cancelled;

    public void handle() {
        this.handled = true;
    }

    public void cancel() {
        this.cancelled = true;
        this.stopped = true;
        this.handled = true;
    }

    public void stop() {
        this.stopped = true;
    }

    @Override
    public void reset() {
        this.stage = null;
        this.targetActor = null;
        this.listenerActor = null;
        this.capture = false;
        this.bubbles = true;
        this.handled = false;
        this.stopped = false;
        this.cancelled = false;
    }

    public Actor getTarget() {
        return this.targetActor;
    }

    public void setTarget(Actor targetActor) {
        this.targetActor = targetActor;
    }

    public Actor getListenerActor() {
        return this.listenerActor;
    }

    public void setListenerActor(Actor listenerActor) {
        this.listenerActor = listenerActor;
    }

    public boolean getBubbles() {
        return this.bubbles;
    }

    public void setBubbles(boolean bubbles) {
        this.bubbles = bubbles;
    }

    public boolean isHandled() {
        return this.handled;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    public boolean isCapture() {
        return this.capture;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return this.stage;
    }
}

