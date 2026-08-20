/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.SnapshotArray;

public class WidgetGroup
extends Group
implements Layout {
    private boolean needsLayout = true;
    private boolean fillParent;
    private boolean layoutEnabled = true;

    public WidgetGroup() {
    }

    public WidgetGroup(Actor ... actors) {
        for (Actor actor : actors) {
            this.addActor(actor);
        }
    }

    @Override
    public float getMinWidth() {
        return this.getPrefWidth();
    }

    @Override
    public float getMinHeight() {
        return this.getPrefHeight();
    }

    @Override
    public float getPrefWidth() {
        return 0.0f;
    }

    @Override
    public float getPrefHeight() {
        return 0.0f;
    }

    @Override
    public float getMaxWidth() {
        return 0.0f;
    }

    @Override
    public float getMaxHeight() {
        return 0.0f;
    }

    @Override
    public void setLayoutEnabled(boolean enabled) {
        if (this.layoutEnabled == enabled) {
            return;
        }
        this.layoutEnabled = enabled;
        this.setLayoutEnabled(this, enabled);
    }

    private void setLayoutEnabled(Group parent, boolean enabled) {
        SnapshotArray<Actor> children = parent.getChildren();
        int n = children.size;
        for (int i = 0; i < n; ++i) {
            Actor actor = (Actor)children.get(i);
            if (actor instanceof Layout) {
                ((Layout)((Object)actor)).setLayoutEnabled(enabled);
                continue;
            }
            if (!(actor instanceof Group)) continue;
            this.setLayoutEnabled((Group)actor, enabled);
        }
    }

    @Override
    public void validate() {
        if (!this.layoutEnabled) {
            return;
        }
        Group parent = this.getParent();
        if (this.fillParent && parent != null) {
            float parentHeight;
            float parentWidth;
            Stage stage = this.getStage();
            if (stage != null && parent == stage.getRoot()) {
                parentWidth = stage.getWidth();
                parentHeight = stage.getHeight();
            } else {
                parentWidth = parent.getWidth();
                parentHeight = parent.getHeight();
            }
            if (this.getWidth() != parentWidth || this.getHeight() != parentHeight) {
                this.setWidth(parentWidth);
                this.setHeight(parentHeight);
                this.invalidate();
            }
        }
        if (!this.needsLayout) {
            return;
        }
        this.needsLayout = false;
        this.layout();
    }

    public boolean needsLayout() {
        return this.needsLayout;
    }

    @Override
    public void invalidate() {
        this.needsLayout = true;
    }

    @Override
    public void invalidateHierarchy() {
        this.invalidate();
        Group parent = this.getParent();
        if (parent instanceof Layout) {
            ((Layout)((Object)parent)).invalidateHierarchy();
        }
    }

    @Override
    protected void childrenChanged() {
        this.invalidateHierarchy();
    }

    @Override
    protected void sizeChanged() {
        this.invalidate();
    }

    @Override
    public void pack() {
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
        this.validate();
        if (this.needsLayout) {
            this.setSize(this.getPrefWidth(), this.getPrefHeight());
            this.validate();
        }
    }

    @Override
    public void setFillParent(boolean fillParent) {
        this.fillParent = fillParent;
    }

    @Override
    public void layout() {
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.validate();
        super.draw(batch, parentAlpha);
    }
}

