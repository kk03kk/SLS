/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Pool;

public class Cell<T extends Actor>
implements Pool.Poolable {
    private static final Float zerof = Float.valueOf(0.0f);
    private static final Float onef = Float.valueOf(1.0f);
    private static final Integer zeroi = 0;
    private static final Integer onei;
    private static final Integer centeri;
    private static final Integer topi;
    private static final Integer bottomi;
    private static final Integer lefti;
    private static final Integer righti;
    private static Files files;
    private static Cell defaults;
    Value minWidth;
    Value minHeight;
    Value prefWidth;
    Value prefHeight;
    Value maxWidth;
    Value maxHeight;
    Value spaceTop;
    Value spaceLeft;
    Value spaceBottom;
    Value spaceRight;
    Value padTop;
    Value padLeft;
    Value padBottom;
    Value padRight;
    Float fillX;
    Float fillY;
    Integer align;
    Integer expandX;
    Integer expandY;
    Integer colspan;
    Boolean uniformX;
    Boolean uniformY;
    Actor actor;
    float actorX;
    float actorY;
    float actorWidth;
    float actorHeight;
    private Table table;
    boolean endRow;
    int column;
    int row;
    int cellAboveIndex;
    float computedPadTop;
    float computedPadLeft;
    float computedPadBottom;
    float computedPadRight;

    public Cell() {
        this.reset();
    }

    public void setLayout(Table table) {
        this.table = table;
    }

    public <A extends Actor> Cell<A> setActor(A newActor) {
        if (this.actor != newActor) {
            if (this.actor != null) {
                this.actor.remove();
            }
            this.actor = newActor;
            if (newActor != null) {
                this.table.addActor(newActor);
            }
        }
        return this;
    }

    public Cell<T> clearActor() {
        this.setActor(null);
        return this;
    }

    public T getActor() {
        return (T)this.actor;
    }

    public boolean hasActor() {
        return this.actor != null;
    }

    public Cell<T> size(Value size) {
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

    public Cell<T> size(Value width, Value height) {
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

    public Cell<T> size(float size) {
        this.size(new Value.Fixed(size));
        return this;
    }

    public Cell<T> size(float width, float height) {
        this.size(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Cell<T> width(Value width) {
        if (width == null) {
            throw new IllegalArgumentException("width cannot be null.");
        }
        this.minWidth = width;
        this.prefWidth = width;
        this.maxWidth = width;
        return this;
    }

    public Cell<T> width(float width) {
        this.width(new Value.Fixed(width));
        return this;
    }

    public Cell<T> height(Value height) {
        if (height == null) {
            throw new IllegalArgumentException("height cannot be null.");
        }
        this.minHeight = height;
        this.prefHeight = height;
        this.maxHeight = height;
        return this;
    }

    public Cell<T> height(float height) {
        this.height(new Value.Fixed(height));
        return this;
    }

    public Cell<T> minSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.minWidth = size;
        this.minHeight = size;
        return this;
    }

    public Cell<T> minSize(Value width, Value height) {
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

    public Cell<T> minWidth(Value minWidth) {
        if (minWidth == null) {
            throw new IllegalArgumentException("minWidth cannot be null.");
        }
        this.minWidth = minWidth;
        return this;
    }

    public Cell<T> minHeight(Value minHeight) {
        if (minHeight == null) {
            throw new IllegalArgumentException("minHeight cannot be null.");
        }
        this.minHeight = minHeight;
        return this;
    }

    public Cell<T> minSize(float size) {
        this.minSize(new Value.Fixed(size));
        return this;
    }

    public Cell<T> minSize(float width, float height) {
        this.minSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Cell<T> minWidth(float minWidth) {
        this.minWidth = new Value.Fixed(minWidth);
        return this;
    }

    public Cell<T> minHeight(float minHeight) {
        this.minHeight = new Value.Fixed(minHeight);
        return this;
    }

    public Cell<T> prefSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.prefWidth = size;
        this.prefHeight = size;
        return this;
    }

    public Cell<T> prefSize(Value width, Value height) {
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

    public Cell<T> prefWidth(Value prefWidth) {
        if (prefWidth == null) {
            throw new IllegalArgumentException("prefWidth cannot be null.");
        }
        this.prefWidth = prefWidth;
        return this;
    }

    public Cell<T> prefHeight(Value prefHeight) {
        if (prefHeight == null) {
            throw new IllegalArgumentException("prefHeight cannot be null.");
        }
        this.prefHeight = prefHeight;
        return this;
    }

    public Cell<T> prefSize(float width, float height) {
        this.prefSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Cell<T> prefSize(float size) {
        this.prefSize(new Value.Fixed(size));
        return this;
    }

    public Cell<T> prefWidth(float prefWidth) {
        this.prefWidth = new Value.Fixed(prefWidth);
        return this;
    }

    public Cell<T> prefHeight(float prefHeight) {
        this.prefHeight = new Value.Fixed(prefHeight);
        return this;
    }

    public Cell<T> maxSize(Value size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        this.maxWidth = size;
        this.maxHeight = size;
        return this;
    }

    public Cell<T> maxSize(Value width, Value height) {
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

    public Cell<T> maxWidth(Value maxWidth) {
        if (maxWidth == null) {
            throw new IllegalArgumentException("maxWidth cannot be null.");
        }
        this.maxWidth = maxWidth;
        return this;
    }

    public Cell<T> maxHeight(Value maxHeight) {
        if (maxHeight == null) {
            throw new IllegalArgumentException("maxHeight cannot be null.");
        }
        this.maxHeight = maxHeight;
        return this;
    }

    public Cell<T> maxSize(float size) {
        this.maxSize(new Value.Fixed(size));
        return this;
    }

    public Cell<T> maxSize(float width, float height) {
        this.maxSize(new Value.Fixed(width), new Value.Fixed(height));
        return this;
    }

    public Cell<T> maxWidth(float maxWidth) {
        this.maxWidth = new Value.Fixed(maxWidth);
        return this;
    }

    public Cell<T> maxHeight(float maxHeight) {
        this.maxHeight = new Value.Fixed(maxHeight);
        return this;
    }

    public Cell<T> space(Value space) {
        if (space == null) {
            throw new IllegalArgumentException("space cannot be null.");
        }
        this.spaceTop = space;
        this.spaceLeft = space;
        this.spaceBottom = space;
        this.spaceRight = space;
        return this;
    }

    public Cell<T> space(Value top, Value left, Value bottom, Value right) {
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
        this.spaceTop = top;
        this.spaceLeft = left;
        this.spaceBottom = bottom;
        this.spaceRight = right;
        return this;
    }

    public Cell<T> spaceTop(Value spaceTop) {
        if (spaceTop == null) {
            throw new IllegalArgumentException("spaceTop cannot be null.");
        }
        this.spaceTop = spaceTop;
        return this;
    }

    public Cell<T> spaceLeft(Value spaceLeft) {
        if (spaceLeft == null) {
            throw new IllegalArgumentException("spaceLeft cannot be null.");
        }
        this.spaceLeft = spaceLeft;
        return this;
    }

    public Cell<T> spaceBottom(Value spaceBottom) {
        if (spaceBottom == null) {
            throw new IllegalArgumentException("spaceBottom cannot be null.");
        }
        this.spaceBottom = spaceBottom;
        return this;
    }

    public Cell<T> spaceRight(Value spaceRight) {
        if (spaceRight == null) {
            throw new IllegalArgumentException("spaceRight cannot be null.");
        }
        this.spaceRight = spaceRight;
        return this;
    }

    public Cell<T> space(float space) {
        if (space < 0.0f) {
            throw new IllegalArgumentException("space cannot be < 0.");
        }
        this.space(new Value.Fixed(space));
        return this;
    }

    public Cell<T> space(float top, float left, float bottom, float right) {
        if (top < 0.0f) {
            throw new IllegalArgumentException("top cannot be < 0.");
        }
        if (left < 0.0f) {
            throw new IllegalArgumentException("left cannot be < 0.");
        }
        if (bottom < 0.0f) {
            throw new IllegalArgumentException("bottom cannot be < 0.");
        }
        if (right < 0.0f) {
            throw new IllegalArgumentException("right cannot be < 0.");
        }
        this.space(new Value.Fixed(top), new Value.Fixed(left), new Value.Fixed(bottom), new Value.Fixed(right));
        return this;
    }

    public Cell<T> spaceTop(float spaceTop) {
        if (spaceTop < 0.0f) {
            throw new IllegalArgumentException("spaceTop cannot be < 0.");
        }
        this.spaceTop = new Value.Fixed(spaceTop);
        return this;
    }

    public Cell<T> spaceLeft(float spaceLeft) {
        if (spaceLeft < 0.0f) {
            throw new IllegalArgumentException("spaceLeft cannot be < 0.");
        }
        this.spaceLeft = new Value.Fixed(spaceLeft);
        return this;
    }

    public Cell<T> spaceBottom(float spaceBottom) {
        if (spaceBottom < 0.0f) {
            throw new IllegalArgumentException("spaceBottom cannot be < 0.");
        }
        this.spaceBottom = new Value.Fixed(spaceBottom);
        return this;
    }

    public Cell<T> spaceRight(float spaceRight) {
        if (spaceRight < 0.0f) {
            throw new IllegalArgumentException("spaceRight cannot be < 0.");
        }
        this.spaceRight = new Value.Fixed(spaceRight);
        return this;
    }

    public Cell<T> pad(Value pad) {
        if (pad == null) {
            throw new IllegalArgumentException("pad cannot be null.");
        }
        this.padTop = pad;
        this.padLeft = pad;
        this.padBottom = pad;
        this.padRight = pad;
        return this;
    }

    public Cell<T> pad(Value top, Value left, Value bottom, Value right) {
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

    public Cell<T> padTop(Value padTop) {
        if (padTop == null) {
            throw new IllegalArgumentException("padTop cannot be null.");
        }
        this.padTop = padTop;
        return this;
    }

    public Cell<T> padLeft(Value padLeft) {
        if (padLeft == null) {
            throw new IllegalArgumentException("padLeft cannot be null.");
        }
        this.padLeft = padLeft;
        return this;
    }

    public Cell<T> padBottom(Value padBottom) {
        if (padBottom == null) {
            throw new IllegalArgumentException("padBottom cannot be null.");
        }
        this.padBottom = padBottom;
        return this;
    }

    public Cell<T> padRight(Value padRight) {
        if (padRight == null) {
            throw new IllegalArgumentException("padRight cannot be null.");
        }
        this.padRight = padRight;
        return this;
    }

    public Cell<T> pad(float pad) {
        this.pad(new Value.Fixed(pad));
        return this;
    }

    public Cell<T> pad(float top, float left, float bottom, float right) {
        this.pad(new Value.Fixed(top), new Value.Fixed(left), new Value.Fixed(bottom), new Value.Fixed(right));
        return this;
    }

    public Cell<T> padTop(float padTop) {
        this.padTop = new Value.Fixed(padTop);
        return this;
    }

    public Cell<T> padLeft(float padLeft) {
        this.padLeft = new Value.Fixed(padLeft);
        return this;
    }

    public Cell<T> padBottom(float padBottom) {
        this.padBottom = new Value.Fixed(padBottom);
        return this;
    }

    public Cell<T> padRight(float padRight) {
        this.padRight = new Value.Fixed(padRight);
        return this;
    }

    public Cell<T> fill() {
        this.fillX = onef;
        this.fillY = onef;
        return this;
    }

    public Cell<T> fillX() {
        this.fillX = onef;
        return this;
    }

    public Cell<T> fillY() {
        this.fillY = onef;
        return this;
    }

    public Cell<T> fill(float x, float y) {
        this.fillX = Float.valueOf(x);
        this.fillY = Float.valueOf(y);
        return this;
    }

    public Cell<T> fill(boolean x, boolean y) {
        this.fillX = x ? onef : zerof;
        this.fillY = y ? onef : zerof;
        return this;
    }

    public Cell<T> fill(boolean fill) {
        this.fillX = fill ? onef : zerof;
        this.fillY = fill ? onef : zerof;
        return this;
    }

    public Cell<T> align(int align) {
        this.align = align;
        return this;
    }

    public Cell<T> center() {
        this.align = centeri;
        return this;
    }

    public Cell<T> top() {
        this.align = this.align == null ? topi : Integer.valueOf((this.align | 2) & 0xFFFFFFFB);
        return this;
    }

    public Cell<T> left() {
        this.align = this.align == null ? lefti : Integer.valueOf((this.align | 8) & 0xFFFFFFEF);
        return this;
    }

    public Cell<T> bottom() {
        this.align = this.align == null ? bottomi : Integer.valueOf((this.align | 4) & 0xFFFFFFFD);
        return this;
    }

    public Cell<T> right() {
        this.align = this.align == null ? righti : Integer.valueOf((this.align | 0x10) & 0xFFFFFFF7);
        return this;
    }

    public Cell<T> grow() {
        this.expandX = onei;
        this.expandY = onei;
        this.fillX = onef;
        this.fillY = onef;
        return this;
    }

    public Cell<T> growX() {
        this.expandX = onei;
        this.fillX = onef;
        return this;
    }

    public Cell<T> growY() {
        this.expandY = onei;
        this.fillY = onef;
        return this;
    }

    public Cell<T> expand() {
        this.expandX = onei;
        this.expandY = onei;
        return this;
    }

    public Cell<T> expandX() {
        this.expandX = onei;
        return this;
    }

    public Cell<T> expandY() {
        this.expandY = onei;
        return this;
    }

    public Cell<T> expand(int x, int y) {
        this.expandX = x;
        this.expandY = y;
        return this;
    }

    public Cell<T> expand(boolean x, boolean y) {
        this.expandX = x ? onei : zeroi;
        this.expandY = y ? onei : zeroi;
        return this;
    }

    public Cell<T> colspan(int colspan) {
        this.colspan = colspan;
        return this;
    }

    public Cell<T> uniform() {
        this.uniformX = Boolean.TRUE;
        this.uniformY = Boolean.TRUE;
        return this;
    }

    public Cell<T> uniformX() {
        this.uniformX = Boolean.TRUE;
        return this;
    }

    public Cell<T> uniformY() {
        this.uniformY = Boolean.TRUE;
        return this;
    }

    public Cell<T> uniform(boolean x, boolean y) {
        this.uniformX = x;
        this.uniformY = y;
        return this;
    }

    public void setActorBounds(float x, float y, float width, float height) {
        this.actorX = x;
        this.actorY = y;
        this.actorWidth = width;
        this.actorHeight = height;
    }

    public float getActorX() {
        return this.actorX;
    }

    public void setActorX(float actorX) {
        this.actorX = actorX;
    }

    public float getActorY() {
        return this.actorY;
    }

    public void setActorY(float actorY) {
        this.actorY = actorY;
    }

    public float getActorWidth() {
        return this.actorWidth;
    }

    public void setActorWidth(float actorWidth) {
        this.actorWidth = actorWidth;
    }

    public float getActorHeight() {
        return this.actorHeight;
    }

    public void setActorHeight(float actorHeight) {
        this.actorHeight = actorHeight;
    }

    public int getColumn() {
        return this.column;
    }

    public int getRow() {
        return this.row;
    }

    public Value getMinWidthValue() {
        return this.minWidth;
    }

    public float getMinWidth() {
        return this.minWidth.get(this.actor);
    }

    public Value getMinHeightValue() {
        return this.minHeight;
    }

    public float getMinHeight() {
        return this.minHeight.get(this.actor);
    }

    public Value getPrefWidthValue() {
        return this.prefWidth;
    }

    public float getPrefWidth() {
        return this.prefWidth.get(this.actor);
    }

    public Value getPrefHeightValue() {
        return this.prefHeight;
    }

    public float getPrefHeight() {
        return this.prefHeight.get(this.actor);
    }

    public Value getMaxWidthValue() {
        return this.maxWidth;
    }

    public float getMaxWidth() {
        return this.maxWidth.get(this.actor);
    }

    public Value getMaxHeightValue() {
        return this.maxHeight;
    }

    public float getMaxHeight() {
        return this.maxHeight.get(this.actor);
    }

    public Value getSpaceTopValue() {
        return this.spaceTop;
    }

    public float getSpaceTop() {
        return this.spaceTop.get(this.actor);
    }

    public Value getSpaceLeftValue() {
        return this.spaceLeft;
    }

    public float getSpaceLeft() {
        return this.spaceLeft.get(this.actor);
    }

    public Value getSpaceBottomValue() {
        return this.spaceBottom;
    }

    public float getSpaceBottom() {
        return this.spaceBottom.get(this.actor);
    }

    public Value getSpaceRightValue() {
        return this.spaceRight;
    }

    public float getSpaceRight() {
        return this.spaceRight.get(this.actor);
    }

    public Value getPadTopValue() {
        return this.padTop;
    }

    public float getPadTop() {
        return this.padTop.get(this.actor);
    }

    public Value getPadLeftValue() {
        return this.padLeft;
    }

    public float getPadLeft() {
        return this.padLeft.get(this.actor);
    }

    public Value getPadBottomValue() {
        return this.padBottom;
    }

    public float getPadBottom() {
        return this.padBottom.get(this.actor);
    }

    public Value getPadRightValue() {
        return this.padRight;
    }

    public float getPadRight() {
        return this.padRight.get(this.actor);
    }

    public float getPadX() {
        return this.padLeft.get(this.actor) + this.padRight.get(this.actor);
    }

    public float getPadY() {
        return this.padTop.get(this.actor) + this.padBottom.get(this.actor);
    }

    public float getFillX() {
        return this.fillX.floatValue();
    }

    public float getFillY() {
        return this.fillY.floatValue();
    }

    public int getAlign() {
        return this.align;
    }

    public int getExpandX() {
        return this.expandX;
    }

    public int getExpandY() {
        return this.expandY;
    }

    public int getColspan() {
        return this.colspan;
    }

    public boolean getUniformX() {
        return this.uniformX;
    }

    public boolean getUniformY() {
        return this.uniformY;
    }

    public boolean isEndRow() {
        return this.endRow;
    }

    public float getComputedPadTop() {
        return this.computedPadTop;
    }

    public float getComputedPadLeft() {
        return this.computedPadLeft;
    }

    public float getComputedPadBottom() {
        return this.computedPadBottom;
    }

    public float getComputedPadRight() {
        return this.computedPadRight;
    }

    public void row() {
        this.table.row();
    }

    public Table getTable() {
        return this.table;
    }

    void clear() {
        this.minWidth = null;
        this.minHeight = null;
        this.prefWidth = null;
        this.prefHeight = null;
        this.maxWidth = null;
        this.maxHeight = null;
        this.spaceTop = null;
        this.spaceLeft = null;
        this.spaceBottom = null;
        this.spaceRight = null;
        this.padTop = null;
        this.padLeft = null;
        this.padBottom = null;
        this.padRight = null;
        this.fillX = null;
        this.fillY = null;
        this.align = null;
        this.expandX = null;
        this.expandY = null;
        this.colspan = null;
        this.uniformX = null;
        this.uniformY = null;
    }

    @Override
    public void reset() {
        this.actor = null;
        this.table = null;
        this.endRow = false;
        this.cellAboveIndex = -1;
        Cell defaults = Cell.defaults();
        if (defaults != null) {
            this.set(defaults);
        }
    }

    void set(Cell cell) {
        this.minWidth = cell.minWidth;
        this.minHeight = cell.minHeight;
        this.prefWidth = cell.prefWidth;
        this.prefHeight = cell.prefHeight;
        this.maxWidth = cell.maxWidth;
        this.maxHeight = cell.maxHeight;
        this.spaceTop = cell.spaceTop;
        this.spaceLeft = cell.spaceLeft;
        this.spaceBottom = cell.spaceBottom;
        this.spaceRight = cell.spaceRight;
        this.padTop = cell.padTop;
        this.padLeft = cell.padLeft;
        this.padBottom = cell.padBottom;
        this.padRight = cell.padRight;
        this.fillX = cell.fillX;
        this.fillY = cell.fillY;
        this.align = cell.align;
        this.expandX = cell.expandX;
        this.expandY = cell.expandY;
        this.colspan = cell.colspan;
        this.uniformX = cell.uniformX;
        this.uniformY = cell.uniformY;
    }

    void merge(Cell cell) {
        if (cell == null) {
            return;
        }
        if (cell.minWidth != null) {
            this.minWidth = cell.minWidth;
        }
        if (cell.minHeight != null) {
            this.minHeight = cell.minHeight;
        }
        if (cell.prefWidth != null) {
            this.prefWidth = cell.prefWidth;
        }
        if (cell.prefHeight != null) {
            this.prefHeight = cell.prefHeight;
        }
        if (cell.maxWidth != null) {
            this.maxWidth = cell.maxWidth;
        }
        if (cell.maxHeight != null) {
            this.maxHeight = cell.maxHeight;
        }
        if (cell.spaceTop != null) {
            this.spaceTop = cell.spaceTop;
        }
        if (cell.spaceLeft != null) {
            this.spaceLeft = cell.spaceLeft;
        }
        if (cell.spaceBottom != null) {
            this.spaceBottom = cell.spaceBottom;
        }
        if (cell.spaceRight != null) {
            this.spaceRight = cell.spaceRight;
        }
        if (cell.padTop != null) {
            this.padTop = cell.padTop;
        }
        if (cell.padLeft != null) {
            this.padLeft = cell.padLeft;
        }
        if (cell.padBottom != null) {
            this.padBottom = cell.padBottom;
        }
        if (cell.padRight != null) {
            this.padRight = cell.padRight;
        }
        if (cell.fillX != null) {
            this.fillX = cell.fillX;
        }
        if (cell.fillY != null) {
            this.fillY = cell.fillY;
        }
        if (cell.align != null) {
            this.align = cell.align;
        }
        if (cell.expandX != null) {
            this.expandX = cell.expandX;
        }
        if (cell.expandY != null) {
            this.expandY = cell.expandY;
        }
        if (cell.colspan != null) {
            this.colspan = cell.colspan;
        }
        if (cell.uniformX != null) {
            this.uniformX = cell.uniformX;
        }
        if (cell.uniformY != null) {
            this.uniformY = cell.uniformY;
        }
    }

    public String toString() {
        return this.actor != null ? this.actor.toString() : super.toString();
    }

    public static Cell defaults() {
        if (files == null || files != Gdx.files) {
            files = Gdx.files;
            defaults = new Cell();
            Cell.defaults.minWidth = Value.minWidth;
            Cell.defaults.minHeight = Value.minHeight;
            Cell.defaults.prefWidth = Value.prefWidth;
            Cell.defaults.prefHeight = Value.prefHeight;
            Cell.defaults.maxWidth = Value.maxWidth;
            Cell.defaults.maxHeight = Value.maxHeight;
            Cell.defaults.spaceTop = Value.zero;
            Cell.defaults.spaceLeft = Value.zero;
            Cell.defaults.spaceBottom = Value.zero;
            Cell.defaults.spaceRight = Value.zero;
            Cell.defaults.padTop = Value.zero;
            Cell.defaults.padLeft = Value.zero;
            Cell.defaults.padBottom = Value.zero;
            Cell.defaults.padRight = Value.zero;
            Cell.defaults.fillX = zerof;
            Cell.defaults.fillY = zerof;
            Cell.defaults.align = centeri;
            Cell.defaults.expandX = zeroi;
            Cell.defaults.expandY = zeroi;
            Cell.defaults.colspan = onei;
            Cell.defaults.uniformX = null;
            Cell.defaults.uniformY = null;
        }
        return defaults;
    }

    static {
        centeri = onei = Integer.valueOf(1);
        topi = 2;
        bottomi = 4;
        lefti = 8;
        righti = 16;
    }
}

