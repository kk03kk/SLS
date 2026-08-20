/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;

public abstract class ChangeListener
implements EventListener {
    @Override
    public boolean handle(Event event) {
        if (!(event instanceof ChangeEvent)) {
            return false;
        }
        this.changed((ChangeEvent)event, event.getTarget());
        return false;
    }

    public abstract void changed(ChangeEvent var1, Actor var2);

    public static class ChangeEvent
    extends Event {
    }
}

