/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;

public class RemoveActorAction
extends Action {
    private boolean removed;

    @Override
    public boolean act(float delta) {
        if (!this.removed) {
            this.removed = true;
            this.target.remove();
        }
        return true;
    }

    @Override
    public void restart() {
        this.removed = false;
    }
}

