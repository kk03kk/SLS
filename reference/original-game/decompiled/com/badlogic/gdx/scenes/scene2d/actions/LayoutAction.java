/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class LayoutAction
extends Action {
    private boolean enabled;

    @Override
    public void setTarget(Actor actor) {
        if (actor != null && !(actor instanceof Layout)) {
            throw new GdxRuntimeException("Actor must implement layout: " + actor);
        }
        super.setTarget(actor);
    }

    @Override
    public boolean act(float delta) {
        ((Layout)((Object)this.target)).setLayoutEnabled(this.enabled);
        return true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setLayoutEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

