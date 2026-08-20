/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedSet;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class List<T>
extends Widget
implements Cullable {
    private ListStyle style;
    final Array<T> items = new Array();
    final ArraySelection<T> selection = new ArraySelection<T>(this.items);
    private Rectangle cullingArea;
    private float prefWidth;
    private float prefHeight;
    private float itemHeight;
    private float textOffsetX;
    private float textOffsetY;

    public List(Skin skin) {
        this(skin.get(ListStyle.class));
    }

    public List(Skin skin, String styleName) {
        this(skin.get(styleName, ListStyle.class));
    }

    public List(ListStyle style) {
        this.selection.setActor(this);
        this.selection.setRequired(true);
        this.setStyle(style);
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
        this.addListener(new InputListener(){

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == 29 && UIUtils.ctrl() && List.this.selection.getMultiple()) {
                    List.this.selection.clear();
                    List.this.selection.addAll(List.this.items);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer == 0 && button != 0) {
                    return false;
                }
                if (List.this.selection.isDisabled()) {
                    return false;
                }
                if (List.this.selection.getMultiple()) {
                    List.this.getStage().setKeyboardFocus(List.this);
                }
                List.this.touchDown(y);
                return true;
            }
        });
    }

    void touchDown(float y) {
        if (this.items.size == 0) {
            return;
        }
        float height = this.getHeight();
        if (this.style.background != null) {
            height -= this.style.background.getTopHeight() + this.style.background.getBottomHeight();
            y -= this.style.background.getBottomHeight();
        }
        int index = (int)((height - y) / this.itemHeight);
        index = Math.max(0, index);
        index = Math.min(this.items.size - 1, index);
        this.selection.choose(this.items.get(index));
    }

    public void setStyle(ListStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        this.style = style;
        this.invalidateHierarchy();
    }

    public ListStyle getStyle() {
        return this.style;
    }

    @Override
    public void layout() {
        BitmapFont font = this.style.font;
        Drawable selectedDrawable = this.style.selection;
        this.itemHeight = font.getCapHeight() - font.getDescent() * 2.0f;
        this.itemHeight += selectedDrawable.getTopHeight() + selectedDrawable.getBottomHeight();
        this.textOffsetX = selectedDrawable.getLeftWidth();
        this.textOffsetY = selectedDrawable.getTopHeight() - font.getDescent();
        this.prefWidth = 0.0f;
        Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
        GlyphLayout layout = layoutPool.obtain();
        for (int i = 0; i < this.items.size; ++i) {
            layout.setText(font, this.toString(this.items.get(i)));
            this.prefWidth = Math.max(layout.width, this.prefWidth);
        }
        layoutPool.free(layout);
        this.prefWidth += selectedDrawable.getLeftWidth() + selectedDrawable.getRightWidth();
        this.prefHeight = (float)this.items.size * this.itemHeight;
        Drawable background = this.style.background;
        if (background != null) {
            this.prefWidth += background.getLeftWidth() + background.getRightWidth();
            this.prefHeight += background.getTopHeight() + background.getBottomHeight();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float height;
        this.validate();
        BitmapFont font = this.style.font;
        Drawable selectedDrawable = this.style.selection;
        Color fontColorSelected = this.style.fontColorSelected;
        Color fontColorUnselected = this.style.fontColorUnselected;
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float x = this.getX();
        float y = this.getY();
        float width = this.getWidth();
        float itemY = height = this.getHeight();
        Drawable background = this.style.background;
        if (background != null) {
            background.draw(batch, x, y, width, height);
            float leftWidth = background.getLeftWidth();
            x += leftWidth;
            itemY -= background.getTopHeight();
            width -= leftWidth + background.getRightWidth();
        }
        font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha);
        for (int i = 0; i < this.items.size; ++i) {
            if (this.cullingArea == null || itemY - this.itemHeight <= this.cullingArea.y + this.cullingArea.height && itemY >= this.cullingArea.y) {
                T item = this.items.get(i);
                boolean selected = this.selection.contains(item);
                if (selected) {
                    selectedDrawable.draw(batch, x, y + itemY - this.itemHeight, width, this.itemHeight);
                    font.setColor(fontColorSelected.r, fontColorSelected.g, fontColorSelected.b, fontColorSelected.a * parentAlpha);
                }
                this.drawItem(batch, font, i, item, x + this.textOffsetX, y + itemY - this.textOffsetY);
                if (selected) {
                    font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha);
                }
            } else if (itemY < this.cullingArea.y) break;
            itemY -= this.itemHeight;
        }
    }

    protected GlyphLayout drawItem(Batch batch, BitmapFont font, int index, T item, float x, float y) {
        return font.draw(batch, this.toString(item), x, y);
    }

    public ArraySelection<T> getSelection() {
        return this.selection;
    }

    public T getSelected() {
        return this.selection.first();
    }

    public void setSelected(T item) {
        if (this.items.contains(item, false)) {
            this.selection.set(item);
        } else if (this.selection.getRequired() && this.items.size > 0) {
            this.selection.set(this.items.first());
        } else {
            this.selection.clear();
        }
    }

    public int getSelectedIndex() {
        OrderedSet selected = this.selection.items();
        return selected.size == 0 ? -1 : this.items.indexOf(selected.first(), false);
    }

    public void setSelectedIndex(int index) {
        if (index < -1 || index >= this.items.size) {
            throw new IllegalArgumentException("index must be >= -1 and < " + this.items.size + ": " + index);
        }
        if (index == -1) {
            this.selection.clear();
        } else {
            this.selection.set(this.items.get(index));
        }
    }

    public void setItems(T ... newItems) {
        if (newItems == null) {
            throw new IllegalArgumentException("newItems cannot be null.");
        }
        float oldPrefWidth = this.getPrefWidth();
        float oldPrefHeight = this.getPrefHeight();
        this.items.clear();
        this.items.addAll(newItems);
        this.selection.validate();
        this.invalidate();
        if (oldPrefWidth != this.getPrefWidth() || oldPrefHeight != this.getPrefHeight()) {
            this.invalidateHierarchy();
        }
    }

    public void setItems(Array newItems) {
        if (newItems == null) {
            throw new IllegalArgumentException("newItems cannot be null.");
        }
        float oldPrefWidth = this.getPrefWidth();
        float oldPrefHeight = this.getPrefHeight();
        this.items.clear();
        this.items.addAll(newItems);
        this.selection.validate();
        this.invalidate();
        if (oldPrefWidth != this.getPrefWidth() || oldPrefHeight != this.getPrefHeight()) {
            this.invalidateHierarchy();
        }
    }

    public void clearItems() {
        if (this.items.size == 0) {
            return;
        }
        this.items.clear();
        this.selection.clear();
        this.invalidateHierarchy();
    }

    public Array<T> getItems() {
        return this.items;
    }

    public float getItemHeight() {
        return this.itemHeight;
    }

    @Override
    public float getPrefWidth() {
        this.validate();
        return this.prefWidth;
    }

    @Override
    public float getPrefHeight() {
        this.validate();
        return this.prefHeight;
    }

    protected String toString(T obj) {
        return obj.toString();
    }

    @Override
    public void setCullingArea(Rectangle cullingArea) {
        this.cullingArea = cullingArea;
    }

    public static class ListStyle {
        public BitmapFont font;
        public Color fontColorSelected = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        public Color fontColorUnselected = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        public Drawable selection;
        public Drawable background;

        public ListStyle() {
        }

        public ListStyle(BitmapFont font, Color fontColorSelected, Color fontColorUnselected, Drawable selection) {
            this.font = font;
            this.fontColorSelected.set(fontColorSelected);
            this.fontColorUnselected.set(fontColorUnselected);
            this.selection = selection;
        }

        public ListStyle(ListStyle style) {
            this.font = style.font;
            this.fontColorSelected.set(style.fontColorSelected);
            this.fontColorUnselected.set(style.fontColorUnselected);
            this.selection = style.selection;
        }
    }
}

