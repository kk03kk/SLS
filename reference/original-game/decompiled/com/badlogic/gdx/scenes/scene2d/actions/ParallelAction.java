/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class ParallelAction
extends Action {
    Array<Action> actions = new Array(4);
    private boolean complete;

    public ParallelAction() {
    }

    public ParallelAction(Action action1) {
        this.addAction(action1);
    }

    public ParallelAction(Action action1, Action action2) {
        this.addAction(action1);
        this.addAction(action2);
    }

    public ParallelAction(Action action1, Action action2, Action action3) {
        this.addAction(action1);
        this.addAction(action2);
        this.addAction(action3);
    }

    public ParallelAction(Action action1, Action action2, Action action3, Action action4) {
        this.addAction(action1);
        this.addAction(action2);
        this.addAction(action3);
        this.addAction(action4);
    }

    public ParallelAction(Action action1, Action action2, Action action3, Action action4, Action action5) {
        this.addAction(action1);
        this.addAction(action2);
        this.addAction(action3);
        this.addAction(action4);
        this.addAction(action5);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean act(float delta) {
        if (this.complete) {
            return true;
        }
        this.complete = true;
        Pool pool = this.getPool();
        this.setPool(null);
        try {
            Array<Action> actions = this.actions;
            int n = actions.size;
            for (int i = 0; i < n && this.actor != null; ++i) {
                Action currentAction = actions.get(i);
                if (currentAction.getActor() != null && !currentAction.act(delta)) {
                    this.complete = false;
                }
                if (this.actor != null) continue;
                boolean bl = true;
                return bl;
            }
            boolean bl = this.complete;
            return bl;
        }
        finally {
            this.setPool(pool);
        }
    }

    @Override
    public void restart() {
        this.complete = false;
        Array<Action> actions = this.actions;
        int n = actions.size;
        for (int i = 0; i < n; ++i) {
            actions.get(i).restart();
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.actions.clear();
    }

    public void addAction(Action action) {
        this.actions.add(action);
        if (this.actor != null) {
            action.setActor(this.actor);
        }
    }

    @Override
    public void setActor(Actor actor) {
        Array<Action> actions = this.actions;
        int n = actions.size;
        for (int i = 0; i < n; ++i) {
            actions.get(i).setActor(actor);
        }
        super.setActor(actor);
    }

    public Array<Action> getActions() {
        return this.actions;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append(super.toString());
        buffer.append('(');
        Array<Action> actions = this.actions;
        int n = actions.size;
        for (int i = 0; i < n; ++i) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(actions.get(i));
        }
        buffer.append(')');
        return buffer.toString();
    }
}

