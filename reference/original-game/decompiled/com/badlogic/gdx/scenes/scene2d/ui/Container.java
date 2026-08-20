/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;

public class Container<T extends Actor>
extends WidgetGroup {
    private T actor;
    private Value minWidth = Value.minWidth;
    private Value minHeight = Value.minHeight;
    private Value prefWidth = Value.prefWidth;
    private Value prefHeight = Value.prefHeight;
    private Value maxWidth = Value.zero;
    private Value maxHeight = Value.zero;
    private Value padTop = Value.zero;
    private Value padLeft = Value.zero;
    private Value padBottom = Value.zero;
    private Value padRight = Value.zero;
    private float fillX;
    private float fillY;
    private int align;
    private Drawable background;
    private boolean clip;
    private boolean round = true;

    public Container() {
        this.setTouchable(Touchable.childrenOnly);
        this.setTransform(false);
    }

    public Container(T actor) {
        this();
        this.setActor(actor);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.validate();
        if (this.isTransform()) {
            this.applyTransform(batch, this.computeTransform());
            this.drawBackground(batch, parentAlpha, 0.0f, 0.0f);
            if (this.clip) {
                batch.flush();
                float padLeft = this.padLeft.get(this);
                float padBottom = this.padBottom.get(this);
                if (this.clipBegin(padLeft, padBottom, this.getWidth() - padLeft - this.padRight.get(this), this.getHeight() - padBottom - this.padTop.get(this))) {
                    this.drawChildren(batch, parentAlpha);
                    batch.flush();
                    this.clipEnd();
                }
            } else {
                this.drawChildren(batch, parentAlpha);
            }
            this.resetTransform(batch);
        } else {
            this.drawBackground(batch, parentAlpha, this.getX(), this.getY());
            super.draw(batch, parentAlpha);
        }
    }

    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        if (this.background == null) {
            return;
        }
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        this.background.draw(batch, x, y, this.getWidth(), this.getHeight());
    }

    public void setBackground(Drawable background) {
        this.setBackground(background, true);
    }

    public void setBackground(Drawable background, boolean adjustPadding) {
        if (this.background == background) {
            return;
        }
        this.background = background;
        if (adjustPadding) {
            if (background == null) {
                this.pad(Value.zero);
            } else {
                this.pad(background.getTopHeight(), background.getLeftWidth(), background.getBottomHeight(), background.getRightWidth());
            }
            this.invalidate();
        }
    }

    public Container<T> background(Drawable background) {
        this.setBackground(background);
        return this;
    }

    public Drawable getBackground() {
        return this.background;
    }

    @Override
    public void layout() {
        float height;
        if (this.actor == null) {
            return;
        }
        float padLeft = this.padLeft.get(this);
        float padBottom = this.padBottom.get(this);
        float containerWidth = this.getWidth() - padLeft - this.padRight.get(this);
        float containerHeight = this.getHeight() - padBottom - this.padTop.get(this);
        float minWidth = this.minWidth.get((Actor)this.actor);
        float minHeight = this.minHeight.get((Actor)this.actor);
        float prefWidth = this.prefWidth.get((Actor)this.actor);
        float prefHeight = this.prefHeight.get((Actor)this.actor);
        float maxWidth = this.maxWidth.get((Actor)this.actor);
        float maxHeight = this.maxHeight.get((Actor)this.actor);
        float width = this.fillX > 0.0f ? containerWidth * this.fillX : Math.min(prefWidth, containerWidth);
        if (width < minWidth) {
            width = minWidth;
        }
        if (maxWidth > 0.0f && width > maxWidth) {
            width = maxWidth;
        }
        if ((height = this.fillY > 0.0f ? containerHeight * this.fillY : Math.min(prefHeight, containerHeight)) < minHeight) {
            height = minHeight;
        }
        if (maxHeight > 0.0f && height > maxHeight) {
            height = maxHeight;
        }
        float x = padLeft;
        if ((this.align & 0x10) != 0) {
            x += containerWidth - width;
        } else if ((this.align & 8) == 0) {
            x += (containerWidth - width) / 2.0f;
        }
        float y = padBottom;
        if ((this.align & 2) != 0) {
            y += containerHeight - height;
        } else if ((this.align & 4) == 0) {
            y += (containerHeight - height) / 2.0f;
        }
        if (this.round) {
            x = Math.round(x);
            y = Math.round(y);
            width = Math.round(width);
            height = Math.round(height);
        }
        ((Actor)this.actor).setBounds(x, y, width, height);
        if (this.actor instanceof Layout) {
            ((Layout)this.actor).validate();
        }
    }

    public void setActor(T actor) {
        if (actor == this) {
            throw new IllegalArgumentException("actor cannot be the Container.");
        }
        if (actor == this.actor) {
            return;
        }
        if (this.actor != null) {
            super.removeActor((Actor)this.actor);
        }
        this.actor = actor;
        if (actor != null) {
            super.addActor((Actor)actor);
        }
    }

    public T getActor() {
        return this.actor;
    }

    @Override
    public void addActor(Actor actor) {
        throw new UnsupportedOperationException("Use Container#setActor.");
    }

    @Override
    public void addActorAt(int index, Actor actor) {
        throw new UnsupportedOperationException("Use Container#setActor.");
    }

    @Override
    public void addActorBefore(Actor actorBefore, Actor actor) {
        throw new UnsupportedOperationException("Use Container#setActor.");
    }

    @Override
    public void addActorAfter(Actor actorAfter, Actor actor) {
        throw new UnsupportedOperationException("Use Container#setActor.");
    }

    @Override
    public boolean removeActor(Actor actor) {
        if (actor != this.actor) {
            return false;
        }
        this.setActor(null);
        return true;
    }

    public Container<T> size(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.minWidth = size;
        this.minHeight = size;
        this.prefWidth = size;
        this.prefHeight = size;
        this.maxWidth = size;
        this.maxHeight = size;
        return this;
    }

    public Container<T> size(Value width, Value height) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.minWidth = width;
        this.minHeight = height;
        this.prefWidth = width;
        this.prefHeight = height;
        this.maxWidth = width;
        this.maxHeight = height;
        return this;
    }

    public Container<T> size(float size) {
        this.size(new Value.Fixed(size));
        return this;
    }

    public Container<T> size(float width, float height) {
        this.size(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Container<T> width(Value width) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        this.minWidth = width;
        this.prefWidth = width;
        this.maxWidth = width;
        return this;
    }

    public Container<T> width(float width) {
        this.width(new Value.Fixed(width));
        return this;
    }

    public Container<T> height(Value height) {
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.minHeight = height;
        this.prefHeight = height;
        this.maxHeight = height;
        return this;
    }

    public Container<T> height(float height) {
        this.height(new Value.Fixed(height));
        return this;
    }

    public Container<T> minSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.minWidth = size;
        this.minHeight = size;
        return this;
    }

    public Container<T> minSize(Value width, Value height) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.minWidth = width;
        this.minHeight = height;
        return this;
    }

    public Container<T> minWidth(Value minWidth) {
        if (minWidth == null) {
            throw new IllegalArgumentException("minWidth cannot be null.");
        }
        this.minWidth = minWidth;
        return this;
    }

    public Container<T> minHeight(Value minHeight) {
        if (minHeight == null) {
            throw new IllegalArgumentException("minHeight cannot be null.");
        }
        this.minHeight = minHeight;
        return this;
    }

    public Container<T> minSize(float size) {
        this.minSize(new Value.Fixed(size));
        return this;
    }

    public Container<T> minSize(float width, float height) {
        this.minSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Container<T> minWidth(float minWidth) {
        this.minWidth = new Value.Fixed(minWidth);
        return this;
    }

    public Container<T> minHeight(float minHeight) {
        this.minHeight = new Value.Fixed(minHeight);
        return this;
    }

    public Container<T> prefSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.prefWidth = size;
        this.prefHeight = size;
        return this;
    }

    public Container<T> prefSize(Value width, Value height) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.prefWidth = width;
        this.prefHeight = height;
        return this;
    }

    public Container<T> prefWidth(Value prefWidth) {
        if (prefWidth == null) {
            throw new IllegalArgumentException("prefWidth cannot be null.");
        }
        this.prefWidth = prefWidth;
        return this;
    }

    public Container<T> prefHeight(Value prefHeight) {
        if (prefHeight == null) {
            throw new IllegalArgumentException("prefHeight cannot be null.");
        }
        this.prefHeight = prefHeight;
        return this;
    }

    public Container<T> prefSize(float width, float height) {
        this.prefSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Container<T> prefSize(float size) {
        this.prefSize(new Value.Fixed(size));
        return this;
    }

    public Container<T> prefWidth(float prefWidth) {
        this.prefWidth = new Value.Fixed(prefWidth);
        return this;
    }

    public Container<T> prefHeight(float prefHeight) {
        this.prefHeight = new Value.Fixed(prefHeight);
        return this;
    }

    public Container<T> maxSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.maxWidth = size;
        this.maxHeight = size;
        return this;
    }

    public Container<T> maxSize(Value width, Value height) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.maxWidth = width;
        this.maxHeight = height;
        return this;
    }

    public Container<T> maxWidth(Value maxWidth) {
        if (maxWidth == null) {
            throw new IllegalArgumentException("maxWidth cannot be null.");
        }
        this.maxWidth = maxWidth;
        return this;
    }

    public Container<T> maxHeight(Value maxHeight) {
        if (maxHeight == null) {
            throw new IllegalArgumentException("maxHeight cannot be null.");
        }
        this.maxHeight = maxHeight;
        return this;
    }

    public Container<T> maxSize(float size) {
        this.maxSize(new Value.Fixed(size));
        return this;
    }

    public Container<T> maxSize(float width, float height) {
        this.maxSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Container<T> maxWidth(float maxWidth) {
        this.maxWidth = new Value.Fixed(maxWidth);
        return this;
    }

    public Container<T> maxHeight(float maxHeight) {
        this.maxHeight = new Value.Fixed(maxHeight);
        return this;
    }

    public Container<T> pad(Value pad) {
        if (pad == null) {
            throw new IllegalArgumentException("pad cannot be null.");
        }
        this.padTop = pad;
        this.padLeft = pad;
        this.padBottom = pad;
        this.padRight = pad;
        return this;
    }

    public Container<T> pad(Value top, Value left, Value bottom, Value right) {
        if (top == null) {
            throw new IllegalArgumentException("top cannot be null.");
        }
        if (left == null) {
            throw new IllegalArgumentException("left cannot be null.");
        }
        if (bottom == null) {
            throw new IllegalArgumentException("bottom cannot be null.");
        }
        if (right == null) {
            throw new IllegalArgumentException("right cannot be null.");
        }
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        return this;
    }

    public Container<T> padTop(Value padTop) {
        if (padTop == null) {
            throw new IllegalArgumentException("padTop cannot be null.");
        }
        this.padTop = padTop;
        return this;
    }

    public Container<T> padLeft(Value padLeft) {
        if (padLeft == null) {
            throw new IllegalArgumentException("padLeft cannot be null.");
        }
        this.padLeft = padLeft;
        return this;
    }

    public Container<T> padBottom(Value padBottom) {
        if (padBottom == null) {
            throw new IllegalArgumentException("padBottom cannot be null.");
        }
        this.padBottom = padBottom;
        return this;
    }

    public Container<T> padRight(Value padRight) {
        if (padRight == null) {
            throw new IllegalArgumentException("padRight cannot be null.");
        }
        this.padRight = padRight;
        return this;
    }

    public Container<T> pad(float pad) {
        Value.Fixed value = new Value.Fixed(pad);
        this.padTop = value;
        this.padLeft = value;
        this.padBottom = value;
        this.padRight = value;
        return this;
    }

    public Container<T> pad(float top, float left, float bottom, float right) {
        this.padTop = new Value.Fixed(top);
        this.padLeft = new Value.Fixed(left);
        this.padBottom = new Value.Fixed(bottom);
        this.padRight = new Value.Fixed(right);
        return this;
    }

    public Container<T> padTop(float padTop) {
        this.padTop = new Value.Fixed(padTop);
        return this;
    }

    public Container<T> padLeft(float padLeft) {
        this.padLeft = new Value.Fixed(padLeft);
        return this;
    }

    public Container<T> padBottom(float padBottom) {
        this.padBottom = new Value.Fixed(padBottom);
        return this;
    }

    public Container<T> padRight(float padRight) {
        this.padRight = new Value.Fixed(padRight);
        return this;
    }

    public Container<T> fill() {
        this.fillX = 1.0f;
        this.fillY = 1.0f;
        return this;
    }

    public Container<T> fillX() {
        this.fillX = 1.0f;
        return this;
    }

    public Container<T> fillY() {
        this.fillY = 1.0f;
        return this;
    }

    public Container<T> fill(float x, float y) {
        this.fillX = x;
        this.fillY = y;
        return this;
    }

    public Container<T> fill(boolean x, boolean y) {
        this.fillX = x ? 1.0f : 0.0f;
        this.fillY = y ? 1.0f : 0.0f;
        return this;
    }

    public Container<T> fill(boolean fill) {
        this.fillX = fill ? 1.0f : 0.0f;
        this.fillY = fill ? 1.0f : 0.0f;
        return this;
    }

    public Container<T> align(int align) {
        this.align = align;
        return this;
    }

    public Container<T> center() {
        this.align = 1;
        return this;
    }

    public Container<T> top() {
        this.align |= 2;
        this.align &= 0xFFFFFFFB;
        return this;
    }

    public Container<T> left() {
        this.align |= 8;
        this.align &= 0xFFFFFFEF;
        return this;
    }

    public Container<T> bottom() {
        this.align |= 4;
        this.align &= 0xFFFFFFFD;
        return this;
    }

    public Container<T> right() {
        this.align |= 0x10;
        this.align &= 0xFFFFFFF7;
        return this;
    }

    @Override
    public float getMinWidth() {
        return this.minWidth.get((Actor)this.actor) + this.padLeft.get(this) + this.padRight.get(this);
    }

    public Value getMinHeightValue() {
        return this.minHeight;
    }

    @Override
    public float getMinHeight() {
        return this.minHeight.get((Actor)this.actor) + this.padTop.get(this) + this.padBottom.get(this);
    }

    public Value getPrefWidthValue() {
        return this.prefWidth;
    }

    @Override
    public float getPrefWidth() {
        float v = this.prefWidth.get((Actor)this.actor);
        if (this.background != null) {
            v = Math.max(v, this.background.getMinWidth());
        }
        return Math.max(this.getMinWidth(), v + this.padLeft.get(this) + this.padRight.get(this));
    }

    public Value getPrefHeightValue() {
        return this.prefHeight;
    }

    @Override
    public float getPrefHeight() {
        float v = this.prefHeight.get((Actor)this.actor);
        if (this.background != null) {
            v = Math.max(v, this.background.getMinHeight());
        }
        return Math.max(this.getMinHeight(), v + this.padTop.get(this) + this.padBottom.get(this));
    }

    public Value getMaxWidthValue() {
        return this.maxWidth;
    }

    @Override
    public float getMaxWidth() {
        float v = this.maxWidth.get((Actor)this.actor);
        if (v > 0.0f) {
            v += this.padLeft.get(this) + this.padRight.get(this);
        }
        return v;
    }

    public Value getMaxHeightValue() {
        return this.maxHeight;
    }

    @Override
    public float getMaxHeight() {
        float v = this.maxHeight.get((Actor)this.actor);
        if (v > 0.0f) {
            v += this.padTop.get(this) + this.padBottom.get(this);
        }
        return v;
    }

    public Value getPadTopValue() {
        return this.padTop;
    }

    public float getPadTop() {
        return this.padTop.get(this);
    }

    public Value getPadLeftValue() {
        return this.padLeft;
    }

    public float getPadLeft() {
        return this.padLeft.get(this);
    }

    public Value getPadBottomValue() {
        return this.padBottom;
    }

    public float getPadBottom() {
        return this.padBottom.get(this);
    }

    public Value getPadRightValue() {
        return this.padRight;
    }

    public float getPadRight() {
        return this.padRight.get(this);
    }

    public float getPadX() {
        return this.padLeft.get(this) + this.padRight.get(this);
    }

    public float getPadY() {
        return this.padTop.get(this) + this.padBottom.get(this);
    }

    public float getFillX() {
        return this.fillX;
    }

    public float getFillY() {
        return this.fillY;
    }

    public int getAlign() {
        return this.align;
    }

    public void setRound(boolean round) {
        this.round = round;
    }

    public void setClip(boolean enabled) {
        this.clip = enabled;
        this.setTransform(enabled);
        this.invalidate();
    }

    public boolean getClip() {
        return this.clip;
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        if (this.clip) {
            if (touchable && this.getTouchable() == Touchable.disabled) {
                return null;
            }
            if (x < 0.0f || x >= this.getWidth() || y < 0.0f || y >= this.getHeight()) {
                return null;
            }
        }
        return super.hit(x, y, touchable);
    }

    @Override
    public void drawDebug(ShapeRenderer shapes) {
        this.validate();
        if (this.isTransform()) {
            this.applyTransform(shapes, this.computeTransform());
            if (this.clip) {
                boolean draw;
                shapes.flush();
                float padLeft = this.padLeft.get(this);
                float padBottom = this.padBottom.get(this);
                boolean bl = draw = this.background == null ? this.clipBegin(0.0f, 0.0f, this.getWidth(), this.getHeight()) : this.clipBegin(padLeft, padBottom, this.getWidth() - padLeft - this.padRight.get(this), this.getHeight() - padBottom - this.padTop.get(this));
                if (draw) {
                    this.drawDebugChildren(shapes);
                    this.clipEnd();
                }
            } else {
                this.drawDebugChildren(shapes);
            }
            this.resetTransform(shapes);
        } else {
            super.drawDebug(shapes);
        }
    }
}

