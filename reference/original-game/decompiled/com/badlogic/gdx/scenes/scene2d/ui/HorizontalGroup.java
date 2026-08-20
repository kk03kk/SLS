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

public class HorizontalGroup
extends WidgetGroup {
    private float prefWidth;
    private float prefHeight;
    private float lastPrefHeight;
    private boolean sizeInvalid = true;
    private FloatArray rowSizes;
    private int align = 8;
    private int rowAlign;
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

    public HorizontalGroup() {
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
        this.prefHeight = 0.0f;
        if (this.wrap) {
            this.prefWidth = 0.0f;
            if (this.rowSizes == null) {
                this.rowSizes = new FloatArray();
            } else {
                this.rowSizes.clear();
            }
            FloatArray rowSizes = this.rowSizes;
            float space = this.space;
            float wrapSpace = this.wrapSpace;
            float pad = this.padLeft + this.padRight;
            float groupWidth = this.getWidth() - pad;
            float x = 0.0f;
            float y = 0.0f;
            float rowHeight = 0.0f;
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
                float incrX = width + (x > 0.0f ? space : 0.0f);
                if (x + incrX > groupWidth && x > 0.0f) {
                    rowSizes.add(x);
                    rowSizes.add(rowHeight);
                    this.prefWidth = Math.max(this.prefWidth, x + pad);
                    if (y > 0.0f) {
                        y += wrapSpace;
                    }
                    y += rowHeight;
                    rowHeight = 0.0f;
                    x = 0.0f;
                    incrX = width;
                }
                x += incrX;
                rowHeight = Math.max(rowHeight, height);
                i += incr;
            }
            rowSizes.add(x);
            rowSizes.add(rowHeight);
            this.prefWidth = Math.max(this.prefWidth, x + pad);
            if (y > 0.0f) {
                y += wrapSpace;
            }
            this.prefHeight = Math.max(this.prefHeight, y + rowHeight);
        } else {
            this.prefWidth = this.padLeft + this.padRight + this.space * (float)(n - 1);
            for (int i = 0; i < n; ++i) {
                Actor child = (Actor)children.get(i);
                if (child instanceof Layout) {
                    Layout layout = (Layout)((Object)child);
                    this.prefWidth += layout.getPrefWidth();
                    this.prefHeight = Math.max(this.prefHeight, layout.getPrefHeight());
                    continue;
                }
                this.prefWidth += child.getWidth();
                this.prefHeight = Math.max(this.prefHeight, child.getHeight());
            }
        }
        this.prefHeight += this.padTop + this.padBottom;
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
        float padBottom = this.padBottom;
        float fill = this.fill;
        float rowHeight = (this.expand ? this.getHeight() : this.prefHeight) - this.padTop - padBottom;
        float x = this.padLeft;
        if ((align & 0x10) != 0) {
            x += this.getWidth() - this.prefWidth;
        } else if ((align & 8) == 0) {
            x += (this.getWidth() - this.prefWidth) / 2.0f;
        }
        float startY = (align & 4) != 0 ? padBottom : ((align & 2) != 0 ? this.getHeight() - this.padTop - rowHeight : padBottom + (this.getHeight() - padBottom - this.padTop - rowHeight) / 2.0f);
        align = this.rowAlign;
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
                height = rowHeight * fill;
            }
            if (layout != null) {
                height = Math.max(height, layout.getMinHeight());
                float maxHeight = layout.getMaxHeight();
                if (maxHeight > 0.0f && height > maxHeight) {
                    height = maxHeight;
                }
            }
            float y = startY;
            if ((align & 2) != 0) {
                y += rowHeight - height;
            } else if ((align & 4) == 0) {
                y += (rowHeight - height) / 2.0f;
            }
            if (round) {
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            } else {
                child.setBounds(x, y, width, height);
            }
            x += width + space;
            if (layout != null) {
                layout.validate();
            }
            i += incr;
        }
    }

    private void layoutWrapped() {
        float prefHeight = this.getPrefHeight();
        if (prefHeight != this.lastPrefHeight) {
            this.lastPrefHeight = prefHeight;
            this.invalidateHierarchy();
        }
        int align = this.align;
        boolean round = this.round;
        float space = this.space;
        float padBottom = this.padBottom;
        float fill = this.fill;
        float wrapSpace = this.wrapSpace;
        float maxWidth = this.prefWidth - this.padLeft - this.padRight;
        float rowY = prefHeight - this.padTop;
        float groupWidth = this.getWidth();
        float xStart = this.padLeft;
        float x = 0.0f;
        float rowHeight = 0.0f;
        if ((align & 2) != 0) {
            rowY += this.getHeight() - prefHeight;
        } else if ((align & 4) == 0) {
            rowY += (this.getHeight() - prefHeight) / 2.0f;
        }
        if ((align & 0x10) != 0) {
            xStart += groupWidth - this.prefWidth;
        } else if ((align & 8) == 0) {
            xStart += (groupWidth - this.prefWidth) / 2.0f;
        }
        groupWidth -= this.padRight;
        align = this.rowAlign;
        FloatArray rowSizes = this.rowSizes;
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
            if (x + width > groupWidth || r == 0) {
                x = xStart;
                if ((align & 0x10) != 0) {
                    x += maxWidth - rowSizes.get(r);
                } else if ((align & 8) == 0) {
                    x += (maxWidth - rowSizes.get(r)) / 2.0f;
                }
                rowHeight = rowSizes.get(r + 1);
                if (r > 0) {
                    rowY -= wrapSpace;
                }
                rowY -= rowHeight;
                r += 2;
            }
            if (fill > 0.0f) {
                height = rowHeight * fill;
            }
            if (layout != null) {
                height = Math.max(height, layout.getMinHeight());
                float maxHeight = layout.getMaxHeight();
                if (maxHeight > 0.0f && height > maxHeight) {
                    height = maxHeight;
                }
            }
            float y = rowY;
            if ((align & 2) != 0) {
                y += rowHeight - height;
            } else if ((align & 4) == 0) {
                y += (rowHeight - height) / 2.0f;
            }
            if (round) {
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            } else {
                child.setBounds(x, y, width, height);
            }
            x += width + space;
            if (layout != null) {
                layout.validate();
            }
            i += incr;
        }
    }

    @Override
    public float getPrefWidth() {
        if (this.wrap) {
            return 0.0f;
        }
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefWidth;
    }

    @Override
    public float getPrefHeight() {
        if (this.sizeInvalid) {
            this.computeSize();
        }
        return this.prefHeight;
    }

    public void setRound(boolean round) {
        this.round = round;
    }

    public HorizontalGroup reverse() {
        this.reverse = true;
        return this;
    }

    public HorizontalGroup reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public boolean getReverse() {
        return this.reverse;
    }

    public HorizontalGroup space(float space) {
        this.space = space;
        return this;
    }

    public float getSpace() {
        return this.space;
    }

    public HorizontalGroup wrapSpace(float wrapSpace) {
        this.wrapSpace = wrapSpace;
        return this;
    }

    public float getWrapSpace() {
        return this.wrapSpace;
    }

    public HorizontalGroup pad(float pad) {
        this.padTop = pad;
        this.padLeft = pad;
        this.padBottom = pad;
        this.padRight = pad;
        return this;
    }

    public HorizontalGroup pad(float top, float left, float bottom, float right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        return this;
    }

    public HorizontalGroup padTop(float padTop) {
        this.padTop = padTop;
        return this;
    }

    public HorizontalGroup padLeft(float padLeft) {
        this.padLeft = padLeft;
        return this;
    }

    public HorizontalGroup padBottom(float padBottom) {
        this.padBottom = padBottom;
        return this;
    }

    public HorizontalGroup padRight(float padRight) {
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

    public HorizontalGroup align(int align) {
        this.align = align;
        return this;
    }

    public HorizontalGroup center() {
        this.align = 1;
        return this;
    }

    public HorizontalGroup top() {
        this.align |= 2;
        this.align &= 0xFFFFFFFB;
        return this;
    }

    public HorizontalGroup left() {
        this.align |= 8;
        this.align &= 0xFFFFFFEF;
        return this;
    }

    public HorizontalGroup bottom() {
        this.align |= 4;
        this.align &= 0xFFFFFFFD;
        return this;
    }

    public HorizontalGroup right() {
        this.align |= 0x10;
        this.align &= 0xFFFFFFF7;
        return this;
    }

    public int getAlign() {
        return this.align;
    }

    public HorizontalGroup fill() {
        this.fill = 1.0f;
        return this;
    }

    public HorizontalGroup fill(float fill) {
        this.fill = fill;
        return this;
    }

    public float getFill() {
        return this.fill;
    }

    public HorizontalGroup expand() {
        this.expand = true;
        return this;
    }

    public HorizontalGroup expand(boolean expand) {
        this.expand = expand;
        return this;
    }

    public boolean getExpand() {
        return this.expand;
    }

    public HorizontalGroup grow() {
        this.expand = true;
        this.fill = 1.0f;
        return this;
    }

    public HorizontalGroup wrap() {
        this.wrap = true;
        return this;
    }

    public HorizontalGroup wrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    public boolean getWrap() {
        return this.wrap;
    }

    public HorizontalGroup rowAlign(int row) {
        this.rowAlign = row;
        return this;
    }

    public HorizontalGroup rowCenter() {
        this.rowAlign = 1;
        return this;
    }

    public HorizontalGroup rowTop() {
        this.rowAlign |= 2;
        this.rowAlign &= 0xFFFFFFFB;
        return this;
    }

    public HorizontalGroup rowBottom() {
        this.rowAlign |= 4;
        this.rowAlign &= 0xFFFFFFFD;
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

