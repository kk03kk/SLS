/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.SnapshotArray;

public class VerticalGroup
extends WidgetGroup {
    private float prefWidth;
    private float prefHeight;
    private float lastPrefWidth;
    private boolean sizeInvalid = true;
    private FloatArray columnSizes;
    private int align = 2;
    private int columnAlign;
    private boolean reverse;
    private boolean round = true;
    private boolean wrap;
    private boolean expand;
    private float space;
    private float wrapSpace;
    private float fill;
    private float padTop;
    private float padLeft;
    private float padBottom;
    private float padRight;

    public VerticalGroup() {
        this.setTouchable(Touchable.childrenOnly);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.sizeInvalid = true;
    }

    private void computeSize() {
        this.sizeInvalid = false;
        SnapshotArray<Actor> children = this.getChildren();
        int n = children.size;
        this.prefWidth = 0.0f;
        if (this.wrap) {
            this.prefHeight = 0.0f;
            if (this.columnSizes == null) {
                this.columnSizes = new FloatArray();
            } else {
                this.columnSizes.clear();
            }
            FloatArray columnSizes = this.columnSizes;
            float space = this.space;
            float wrapSpace = this.wrapSpace;
            float pad = this.padTop + this.padBottom;
            float groupHeight = this.getHeight() - pad;
            float x = 0.0f;
            float y = 0.0f;
            float columnWidth = 0.0f;
            int i = 0;
            int incr = 1;
            if (this.reverse) {
                i = n - 1;
                n = -1;
                incr = -1;
            }
            while (i != n) {
                float height;
                float width;
                Actor child = (Actor)children.get(i);
                if (child instanceof Layout) {
                    Layout layout = (Layout)((Object)child);
                    width = layout.getPrefWidth();
                    height = layout.getPrefHeight();
                } else {
                    width = child.getWidth();
                    height = child.getHeight();
                }
                float incrY = height + (y > 0.0f ? space : 0.0f);
                if (y + incrY > groupHeight && y > 0.0f) {
                    columnSizes.add(y);
                    columnSizes.add(columnWidth);
                    this.prefHeight = Math.max(this.prefHeight, y + pad);
                    if (x > 0.0f) {
                        x += wrapSpace;
                    }
                    x += columnWidth;
                    columnWidth = 0.0f;
                    y = 0.0f;
                    incrY = height;
                }
                y += incrY;
                columnWidth = Math.max(columnWidth, width);
                i += incr;
            }
            columnSizes.add(y);
            columnSizes.add(columnWidth);
            this.prefHeight = Math.max(this.prefHeight, y + pad);
            if (x > 0.0f) {
                x += wrapSpace;
            }
            this.prefWidth = Math.max(this.prefWidth, x + columnWidth);
        } else {
            this.prefHeight = this.padTop + this.padBottom + this.space * (float)(n - 1);
            for (int i = 0; i < n; ++i) {
                Actor child = (Actor)children.get(i);
                if (child instanceof Layout) {
                    Layout layout = (Layout)((Object)child);
                    this.prefWidth = Math.max(this.prefWidth, layout.getPrefWidth());
                    this.prefHeight += layout.getPrefHeight();
                    continue;
                }
                this.prefWidth = Math.max(this.prefWidth, child.getWidth());
                this.prefHeight += child.getHeight();
            }
        }
        this.prefWidth += this.padLeft + this.padRight;
        if (this.round) {
            this.prefWidth = Math.round(this.prefWidth);
            this.prefHeight = Math.round(this.prefHeight);
        }
    }

    @Override
    public void layout() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        if (this.wrap) {
            this.layoutWrapped();
            return;
        }
        boolean round = this.round;
        int align = this.align;
        float space = this.space;
        float padLeft = this.padLeft;
        float fill = this.fill;
        float columnWidth = (this.expand ? this.getWidth() : this.prefWidth) - padLeft - this.padRight;
        float y = this.prefHeight - this.padTop + space;
        if ((align & 2) != 0) {
            y += this.getHeight() - this.prefHeight;
        } else if ((align & 4) == 0) {
            y += (this.getHeight() - this.prefHeight) / 2.0f;
        }
        float startX = (align & 8) != 0 ? padLeft : ((align & 0x10) != 0 ? this.getWidth() - this.padRight - columnWidth : padLeft + (this.getWidth() - padLeft - this.padRight - columnWidth) / 2.0f);
        align = this.columnAlign;
        SnapshotArray<Actor> children = this.getChildren();
        int i = 0;
        int n = children.size;
        int incr = 1;
        if (this.reverse) {
            i = n - 1;
            n = -1;
            incr = -1;
        }
        boolean r = false;
        while (i != n) {
            float height;
            float width;
            Actor child = (Actor)children.get(i);
            Layout layout = null;
            if (child instanceof Layout) {
                layout = (Layout)((Object)child);
                width = layout.getPrefWidth();
                height = layout.getPrefHeight();
            } else {
                width = child.getWidth();
                height = child.getHeight();
            }
            if (fill > 0.0f) {
                width = columnWidth * fill;
            }
            if (layout != null) {
                width = Math.max(width, layout.getMinWidth());
                float maxWidth = layout.getMaxWidth();
                if (maxWidth > 0.0f && width > maxWidth) {
                    width = maxWidth;
                }
            }
            float x = startX;
            if ((align & 0x10) != 0) {
                x += columnWidth - width;
            } else if ((align & 8) == 0) {
                x += (columnWidth - width) / 2.0f;
            }
            y -= height + space;
            if (round) {
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            } else {
                child.setBounds(x, y, width, height);
            }
            if (layout != null) {
                layout.validate();
            }
            i += incr;
        }
    }

    private void layoutWrapped() {
        float prefWidth = this.getPrefWidth();
        if (prefWidth != this.lastPrefWidth) {
            this.lastPrefWidth = prefWidth;
            this.invalidateHierarchy();
        }
        int align = this.align;
        boolean round = this.round;
        float space = this.space;
        float padLeft = this.padLeft;
        float fill = this.fill;
        float wrapSpace = this.wrapSpace;
        float maxHeight = this.prefHeight - this.padTop - this.padBottom;
        float columnX = padLeft;
        float groupHeight = this.getHeight();
        float yStart = this.prefHeight - this.padTop + space;
        float y = 0.0f;
        float columnWidth = 0.0f;
        if ((align & 0x10) != 0) {
            columnX += this.getWidth() - prefWidth;
        } else if ((align & 8) == 0) {
            columnX += (this.getWidth() - prefWidth) / 2.0f;
        }
        if ((align & 2) != 0) {
            yStart += groupHeight - this.prefHeight;
        } else if ((align & 4) == 0) {
            yStart += (groupHeight - this.prefHeight) / 2.0f;
        }
        groupHeight -= this.padTop;
        align = this.columnAlign;
        FloatArray columnSizes = this.columnSizes;
        SnapshotArray<Actor> children = this.getChildren();
        int i = 0;
        int n = children.size;
        int incr = 1;
        if (this.reverse) {
            i = n - 1;
            n = -1;
            incr = -1;
        }
        int r = 0;
        while (i != n) {
            float height;
            float width;
            Actor child = (Actor)children.get(i);
            Layout layout = null;
            if (child instanceof Layout) {
                layout = (Layout)((Object)child);
                width = layout.getPrefWidth();
                height = layout.getPrefHeight();
            } else {
                width = child.getWidth();
                height = child.getHeight();
            }
            if (y - height - space < this.padBottom || r == 0) {
                y = yStart;
                if ((align & 4) != 0) {
                    y -= maxHeight - columnSizes.get(r);
                } else if ((align & 2) == 0) {
                    y -= (maxHeight - columnSizes.get(r)) / 2.0f;
                }
                if (r > 0) {
                    columnX += wrapSpace;
                    columnX += columnWidth;
                }
                columnWidth = columnSizes.get(r + 1);
                r += 2;
            }
            if (fill > 0.0f) {
                width = columnWidth * fill;
            }
            if (layout != null) {
                width = Math.max(width, layout.getMinWidth());
                float maxWidth = layout.getMaxWidth();
                if (maxWidth > 0.0f && width > maxWidth) {
                    width = maxWidth;
                }
            }
            float x = columnX;
            if ((align & 0x10) != 0) {
                x += columnWidth - width;
            } else if ((align & 8) == 0) {
                x += (columnWidth - width) / 2.0f;
            }
            y -= height + space;
            if (round) {
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            } else {
                child.setBounds(x, y, width, height);
            }
            if (layout != null) {
                layout.validate();
            }
            i += incr;
        }
    }

    @Override
    public float getPrefWidth() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefWidth;
    }

    @Override
    public float getPrefHeight() {
        if (this.wrap) {
            return 0.0f;
        }
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefHeight;
    }

    public void setRound(boolean round) {
        this.round = round;
    }

    public VerticalGroup reverse() {
        this.reverse = true;
        return this;
    }

    public VerticalGroup reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public boolean getReverse() {
        return this.reverse;
    }

    public VerticalGroup space(float space) {
        this.space = space;
        return this;
    }

    public float getSpace() {
        return this.space;
    }

    public VerticalGroup wrapSpace(float wrapSpace) {
        this.wrapSpace = wrapSpace;
        return this;
    }

    public float getWrapSpace() {
        return this.wrapSpace;
    }

    public VerticalGroup pad(float pad) {
        this.padTop = pad;
        this.padLeft = pad;
        this.padBottom = pad;
        this.padRight = pad;
        return this;
    }

    public VerticalGroup pad(float top, float left, float bottom, float right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        return this;
    }

    public VerticalGroup padTop(float padTop) {
        this.padTop = padTop;
        return this;
    }

    public VerticalGroup padLeft(float padLeft) {
        this.padLeft = padLeft;
        return this;
    }

    public VerticalGroup padBottom(float padBottom) {
        this.padBottom = padBottom;
        return this;
    }

    public VerticalGroup padRight(float padRight) {
        this.padRight = padRight;
        return this;
    }

    public float getPadTop() {
        return this.padTop;
    }

    public float getPadLeft() {
        return this.padLeft;
    }

    public float getPadBottom() {
        return this.padBottom;
    }

    public float getPadRight() {
        return this.padRight;
    }

    public VerticalGroup align(int align) {
        this.align = align;
        return this;
    }

    public VerticalGroup center() {
        this.align = 1;
        return this;
    }

    public VerticalGroup top() {
        this.align |= 2;
        this.align &= 0xFFFFFFFB;
        return this;
    }

    public VerticalGroup left() {
        this.align |= 8;
        this.align &= 0xFFFFFFEF;
        return this;
    }

    public VerticalGroup bottom() {
        this.align |= 4;
        this.align &= 0xFFFFFFFD;
        return this;
    }

    public VerticalGroup right() {
        this.align |= 0x10;
        this.align &= 0xFFFFFFF7;
        return this;
    }

    public int getAlign() {
        return this.align;
    }

    public VerticalGroup fill() {
        this.fill = 1.0f;
        return this;
    }

    public VerticalGroup fill(float fill) {
        this.fill = fill;
        return this;
    }

    public float getFill() {
        return this.fill;
    }

    public VerticalGroup expand() {
        this.expand = true;
        return this;
    }

    public VerticalGroup expand(boolean expand) {
        this.expand = expand;
        return this;
    }

    public boolean getExpand() {
        return this.expand;
    }

    public VerticalGroup grow() {
        this.expand = true;
        this.fill = 1.0f;
        return this;
    }

    public VerticalGroup wrap() {
        this.wrap = true;
        return this;
    }

    public VerticalGroup wrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    public boolean getWrap() {
        return this.wrap;
    }

    public VerticalGroup columnAlign(int columnAlign) {
        this.columnAlign = columnAlign;
        return this;
    }

    public VerticalGroup columnCenter() {
        this.columnAlign = 1;
        return this;
    }

    public VerticalGroup columnLeft() {
        this.columnAlign |= 8;
        this.columnAlign &= 0xFFFFFFEF;
        return this;
    }

    public VerticalGroup columnRight() {
        this.columnAlign |= 0x10;
        this.columnAlign &= 0xFFFFFFF7;
        return this;
    }

    @Override
    protected void drawDebugBounds(ShapeRenderer shapes) {
        super.drawDebugBounds(shapes);
        if (!this.getDebug()) {
            return;
        }
        shapes.set(ShapeRenderer.ShapeType.Line);
        shapes.setColor(this.getStage().getDebugColor());
        shapes.rect(this.getX() + this.padLeft, this.getY() + this.padBottom, this.getOriginX(), this.getOriginY(), this.getWidth() - this.padLeft - this.padRight, this.getHeight() - this.padBottom - this.padTop, this.getScaleX(), this.getScaleY(), this.getRotation());
    }
}

