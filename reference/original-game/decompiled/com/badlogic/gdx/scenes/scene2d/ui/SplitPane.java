/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class SplitPane
extends WidgetGroup {
    SplitPaneStyle style;
    private Actor firstWidget;
    private Actor secondWidget;
    boolean vertical;
    float splitAmount = 0.5f;
    float minAmount;
    float maxAmount = 1.0f;
    private float oldSplitAmount;
    private Rectangle firstWidgetBounds = new Rectangle();
    private Rectangle secondWidgetBounds = new Rectangle();
    Rectangle handleBounds = new Rectangle();
    private Rectangle firstScissors = new Rectangle();
    private Rectangle secondScissors = new Rectangle();
    Vector2 lastPoint = new Vector2();
    Vector2 handlePosition = new Vector2();

    public SplitPane(Actor firstWidget, Actor secondWidget, boolean vertical, Skin skin) {
        this(firstWidget, secondWidget, vertical, skin, "default-" + (vertical ? "vertical" : "horizontal"));
    }

    public SplitPane(Actor firstWidget, Actor secondWidget, boolean vertical, Skin skin, String styleName) {
        this(firstWidget, secondWidget, vertical, skin.get(styleName, SplitPaneStyle.class));
    }

    public SplitPane(Actor firstWidget, Actor secondWidget, boolean vertical, SplitPaneStyle style) {
        this.firstWidget = firstWidget;
        this.secondWidget = secondWidget;
        this.vertical = vertical;
        this.setStyle(style);
        this.setFirstWidget(firstWidget);
        this.setSecondWidget(secondWidget);
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
        this.initialize();
    }

    private void initialize() {
        this.addListener(new InputListener(){
            int draggingPointer = -1;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (this.draggingPointer != -1) {
                    return false;
                }
                if (pointer == 0 && button != 0) {
                    return false;
                }
                if (SplitPane.this.handleBounds.contains(x, y)) {
                    this.draggingPointer = pointer;
                    SplitPane.this.lastPoint.set(x, y);
                    SplitPane.this.handlePosition.set(SplitPane.this.handleBounds.x, SplitPane.this.handleBounds.y);
                    return true;
                }
                return false;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer == this.draggingPointer) {
                    this.draggingPointer = -1;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer != this.draggingPointer) {
                    return;
                }
                Drawable handle = SplitPane.this.style.handle;
                if (!SplitPane.this.vertical) {
                    float dragX;
                    float delta = x - SplitPane.this.lastPoint.x;
                    float availWidth = SplitPane.this.getWidth() - handle.getMinWidth();
                    SplitPane.this.handlePosition.x = dragX = SplitPane.this.handlePosition.x + delta;
                    dragX = Math.max(0.0f, dragX);
                    dragX = Math.min(availWidth, dragX);
                    SplitPane.this.splitAmount = dragX / availWidth;
                    if (SplitPane.this.splitAmount < SplitPane.this.minAmount) {
                        SplitPane.this.splitAmount = SplitPane.this.minAmount;
                    }
                    if (SplitPane.this.splitAmount > SplitPane.this.maxAmount) {
                        SplitPane.this.splitAmount = SplitPane.this.maxAmount;
                    }
                    SplitPane.this.lastPoint.set(x, y);
                } else {
                    float dragY;
                    float delta = y - SplitPane.this.lastPoint.y;
                    float availHeight = SplitPane.this.getHeight() - handle.getMinHeight();
                    SplitPane.this.handlePosition.y = dragY = SplitPane.this.handlePosition.y + delta;
                    dragY = Math.max(0.0f, dragY);
                    dragY = Math.min(availHeight, dragY);
                    SplitPane.this.splitAmount = 1.0f - dragY / availHeight;
                    if (SplitPane.this.splitAmount < SplitPane.this.minAmount) {
                        SplitPane.this.splitAmount = SplitPane.this.minAmount;
                    }
                    if (SplitPane.this.splitAmount > SplitPane.this.maxAmount) {
                        SplitPane.this.splitAmount = SplitPane.this.maxAmount;
                    }
                    SplitPane.this.lastPoint.set(x, y);
                }
                SplitPane.this.invalidate();
            }
        });
    }

    public void setStyle(SplitPaneStyle style) {
        this.style = style;
        this.invalidateHierarchy();
    }

    public SplitPaneStyle getStyle() {
        return this.style;
    }

    @Override
    public void layout() {
        Actor secondWidget;
        if (!this.vertical) {
            this.calculateHorizBoundsAndPositions();
        } else {
            this.calculateVertBoundsAndPositions();
        }
        Actor firstWidget = this.firstWidget;
        if (firstWidget != null) {
            Rectangle firstWidgetBounds = this.firstWidgetBounds;
            firstWidget.setBounds(firstWidgetBounds.x, firstWidgetBounds.y, firstWidgetBounds.width, firstWidgetBounds.height);
            if (firstWidget instanceof Layout) {
                ((Layout)((Object)firstWidget)).validate();
            }
        }
        if ((secondWidget = this.secondWidget) != null) {
            Rectangle secondWidgetBounds = this.secondWidgetBounds;
            secondWidget.setBounds(secondWidgetBounds.x, secondWidgetBounds.y, secondWidgetBounds.width, secondWidgetBounds.height);
            if (secondWidget instanceof Layout) {
                ((Layout)((Object)secondWidget)).validate();
            }
        }
    }

    @Override
    public float getPrefWidth() {
        float second;
        float first;
        float f = this.firstWidget == null ? 0.0f : (first = this.firstWidget instanceof Layout ? ((Layout)((Object)this.firstWidget)).getPrefWidth() : this.firstWidget.getWidth());
        float f2 = this.secondWidget == null ? 0.0f : (second = this.secondWidget instanceof Layout ? ((Layout)((Object)this.secondWidget)).getPrefWidth() : this.secondWidget.getWidth());
        if (this.vertical) {
            return Math.max(first, second);
        }
        return first + this.style.handle.getMinWidth() + second;
    }

    @Override
    public float getPrefHeight() {
        float second;
        float first;
        float f = this.firstWidget == null ? 0.0f : (first = this.firstWidget instanceof Layout ? ((Layout)((Object)this.firstWidget)).getPrefHeight() : this.firstWidget.getHeight());
        float f2 = this.secondWidget == null ? 0.0f : (second = this.secondWidget instanceof Layout ? ((Layout)((Object)this.secondWidget)).getPrefHeight() : this.secondWidget.getHeight());
        if (!this.vertical) {
            return Math.max(first, second);
        }
        return first + this.style.handle.getMinHeight() + second;
    }

    @Override
    public float getMinWidth() {
        return 0.0f;
    }

    @Override
    public float getMinHeight() {
        return 0.0f;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    private void calculateHorizBoundsAndPositions() {
        Drawable handle = this.style.handle;
        float height = this.getHeight();
        float availWidth = this.getWidth() - handle.getMinWidth();
        float leftAreaWidth = (int)(availWidth * this.splitAmount);
        float rightAreaWidth = availWidth - leftAreaWidth;
        float handleWidth = handle.getMinWidth();
        this.firstWidgetBounds.set(0.0f, 0.0f, leftAreaWidth, height);
        this.secondWidgetBounds.set(leftAreaWidth + handleWidth, 0.0f, rightAreaWidth, height);
        this.handleBounds.set(leftAreaWidth, 0.0f, handleWidth, height);
    }

    private void calculateVertBoundsAndPositions() {
        Drawable handle = this.style.handle;
        float width = this.getWidth();
        float height = this.getHeight();
        float availHeight = height - handle.getMinHeight();
        float topAreaHeight = (int)(availHeight * this.splitAmount);
        float bottomAreaHeight = availHeight - topAreaHeight;
        float handleHeight = handle.getMinHeight();
        this.firstWidgetBounds.set(0.0f, height - topAreaHeight, width, topAreaHeight);
        this.secondWidgetBounds.set(0.0f, 0.0f, width, bottomAreaHeight);
        this.handleBounds.set(0.0f, bottomAreaHeight, width, handleHeight);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.validate();
        Color color = this.getColor();
        Drawable handle = this.style.handle;
        this.applyTransform(batch, this.computeTransform());
        Matrix4 transform = batch.getTransformMatrix();
        if (this.firstWidget != null) {
            batch.flush();
            this.getStage().calculateScissors(this.firstWidgetBounds, this.firstScissors);
            if (ScissorStack.pushScissors(this.firstScissors)) {
                if (this.firstWidget.isVisible()) {
                    this.firstWidget.draw(batch, parentAlpha * color.a);
                }
                batch.flush();
                ScissorStack.popScissors();
            }
        }
        if (this.secondWidget != null) {
            batch.flush();
            this.getStage().calculateScissors(this.secondWidgetBounds, this.secondScissors);
            if (ScissorStack.pushScissors(this.secondScissors)) {
                if (this.secondWidget.isVisible()) {
                    this.secondWidget.draw(batch, parentAlpha * color.a);
                }
                batch.flush();
                ScissorStack.popScissors();
            }
        }
        batch.setColor(color.r, color.g, color.b, parentAlpha * color.a);
        handle.draw(batch, this.handleBounds.x, this.handleBounds.y, this.handleBounds.width, this.handleBounds.height);
        this.resetTransform(batch);
    }

    public void setSplitAmount(float split) {
        this.splitAmount = Math.max(Math.min(this.maxAmount, split), this.minAmount);
        this.invalidate();
    }

    public float getSplit() {
        return this.splitAmount;
    }

    public void setMinSplitAmount(float minAmount) {
        if (minAmount < 0.0f) {
            throw new GdxRuntimeException("minAmount has to be >= 0");
        }
        if (minAmount >= this.maxAmount) {
            throw new GdxRuntimeException("minAmount has to be < maxAmount");
        }
        this.minAmount = minAmount;
    }

    public void setMaxSplitAmount(float maxAmount) {
        if (maxAmount > 1.0f) {
            throw new GdxRuntimeException("maxAmount has to be <= 1");
        }
        if (maxAmount <= this.minAmount) {
            throw new GdxRuntimeException("maxAmount has to be > minAmount");
        }
        this.maxAmount = maxAmount;
    }

    public void setFirstWidget(Actor widget) {
        if (this.firstWidget != null) {
            super.removeActor(this.firstWidget);
        }
        this.firstWidget = widget;
        if (widget != null) {
            super.addActor(widget);
        }
        this.invalidate();
    }

    public void setSecondWidget(Actor widget) {
        if (this.secondWidget != null) {
            super.removeActor(this.secondWidget);
        }
        this.secondWidget = widget;
        if (widget != null) {
            super.addActor(widget);
        }
        this.invalidate();
    }

    @Override
    public void addActor(Actor actor) {
        throw new UnsupportedOperationException("Use ScrollPane#setWidget.");
    }

    @Override
    public void addActorAt(int index, Actor actor) {
        throw new UnsupportedOperationException("Use ScrollPane#setWidget.");
    }

    @Override
    public void addActorBefore(Actor actorBefore, Actor actor) {
        throw new UnsupportedOperationException("Use ScrollPane#setWidget.");
    }

    @Override
    public boolean removeActor(Actor actor) {
        throw new UnsupportedOperationException("Use ScrollPane#setWidget(null).");
    }

    public static class SplitPaneStyle {
        public Drawable handle;

        public SplitPaneStyle() {
        }

        public SplitPaneStyle(Drawable handle) {
            this.handle = handle;
        }

        public SplitPaneStyle(SplitPaneStyle style) {
            this.handle = style.handle;
        }
    }
}

