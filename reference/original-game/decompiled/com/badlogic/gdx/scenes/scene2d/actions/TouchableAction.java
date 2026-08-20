/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Touchable;

public class TouchableAction
extends Action {
    private Touchable touchable;

    @Override
    public boolean act(float delta) {
        this.target.setTouchable(this.touchable);
        return true;
    }

    public Touchable getTouchable() {
        return this.touchable;
    }

    public void setTouchable(Touchable touchable) {
        this.touchable = touchable;
    }
}

