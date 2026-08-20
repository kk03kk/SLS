/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.EventListener;

public class RemoveListenerAction
extends Action {
    private EventListener listener;
    private boolean capture;

    @Override
    public boolean act(float delta) {
        if (this.capture) {
            this.target.removeCaptureListener(this.listener);
        } else {
            this.target.removeListener(this.listener);
        }
        return true;
    }

    public EventListener getListener() {
        return this.listener;
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    public boolean getCapture() {
        return this.capture;
    }

    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    @Override
    public void reset() {
        super.reset();
        this.listener = null;
    }
}

