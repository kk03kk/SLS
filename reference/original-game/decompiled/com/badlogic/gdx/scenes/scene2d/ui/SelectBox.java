/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedSet;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class SelectBox<T>
extends Widget
implements Disableable {
    static final Vector2 temp = new Vector2();
    SelectBoxStyle style;
    final Array<T> items = new Array();
    final ArraySelection<T> selection = new ArraySelection<T>(this.items);
    SelectBoxList<T> selectBoxList;
    private float prefWidth;
    private float prefHeight;
    private ClickListener clickListener;
    boolean disabled;
    private GlyphLayout layout = new GlyphLayout();

    public SelectBox(Skin skin) {
        this(skin.get(SelectBoxStyle.class));
    }

    public SelectBox(Skin skin, String styleName) {
        this(skin.get(styleName, SelectBoxStyle.class));
    }

    public SelectBox(SelectBoxStyle style) {
        this.setStyle(style);
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
        this.selection.setActor(this);
        this.selection.setRequired(true);
        this.selectBoxList = new SelectBoxList(this);
        this.clickListener = new ClickListener(){

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer == 0 && button != 0) {
                    return false;
                }
                if (SelectBox.this.disabled) {
                    return false;
                }
                if (SelectBox.this.selectBoxList.hasParent()) {
                    SelectBox.this.hideList();
                } else {
                    SelectBox.this.showList();
                }
                return true;
            }
        };
        this.addListener(this.clickListener);
    }

    public void setMaxListCount(int maxListCount) {
        this.selectBoxList.maxListCount = maxListCount;
    }

    public int getMaxListCount() {
        return this.selectBoxList.maxListCount;
    }

    @Override
    protected void setStage(Stage stage) {
        if (stage == null) {
            this.selectBoxList.hide();
        }
        super.setStage(stage);
    }

    public void setStyle(SelectBoxStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        this.style = style;
        if (this.selectBoxList != null) {
            this.selectBoxList.setStyle(style.scrollStyle);
            this.selectBoxList.list.setStyle(style.listStyle);
        }
        this.invalidateHierarchy();
    }

    public SelectBoxStyle getStyle() {
        return this.style;
    }

    public void setItems(T ... newItems) {
        if (newItems == null) {
            throw new IllegalArgumentException("newItems cannot be null.");
        }
        float oldPrefWidth = this.getPrefWidth();
        this.items.clear();
        this.items.addAll(newItems);
        this.selection.validate();
        this.selectBoxList.list.setItems(this.items);
        this.invalidate();
        if (oldPrefWidth != this.getPrefWidth()) {
            this.invalidateHierarchy();
        }
    }

    public void setItems(Array<T> newItems) {
        if (newItems == null) {
            throw new IllegalArgumentException("newItems cannot be null.");
        }
        float oldPrefWidth = this.getPrefWidth();
        this.items.clear();
        this.items.addAll(newItems);
        this.selection.validate();
        this.selectBoxList.list.setItems(this.items);
        this.invalidate();
        if (oldPrefWidth != this.getPrefWidth()) {
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

    @Override
    public void layout() {
        Drawable bg = this.style.background;
        BitmapFont font = this.style.font;
        this.prefHeight = bg != null ? Math.max(bg.getTopHeight() + bg.getBottomHeight() + font.getCapHeight() - font.getDescent() * 2.0f, bg.getMinHeight()) : font.getCapHeight() - font.getDescent() * 2.0f;
        float maxItemWidth = 0.0f;
        Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
        GlyphLayout layout = layoutPool.obtain();
        for (int i = 0; i < this.items.size; ++i) {
            layout.setText(font, this.toString(this.items.get(i)));
            maxItemWidth = Math.max(layout.width, maxItemWidth);
        }
        layoutPool.free(layout);
        this.prefWidth = maxItemWidth;
        if (bg != null) {
            this.prefWidth += bg.getLeftWidth() + bg.getRightWidth();
        }
        List.ListStyle listStyle = this.style.listStyle;
        ScrollPane.ScrollPaneStyle scrollStyle = this.style.scrollStyle;
        float listWidth = maxItemWidth + listStyle.selection.getLeftWidth() + listStyle.selection.getRightWidth();
        if (scrollStyle.background != null) {
            listWidth += scrollStyle.background.getLeftWidth() + scrollStyle.background.getRightWidth();
        }
        if (this.selectBoxList == null || !this.selectBoxList.disableY) {
            listWidth += Math.max(this.style.scrollStyle.vScroll != null ? this.style.scrollStyle.vScroll.getMinWidth() : 0.0f, this.style.scrollStyle.vScrollKnob != null ? this.style.scrollStyle.vScrollKnob.getMinWidth() : 0.0f);
        }
        this.prefWidth = Math.max(this.prefWidth, listWidth);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Object selected;
        this.validate();
        Drawable background = this.disabled && this.style.backgroundDisabled != null ? this.style.backgroundDisabled : (this.selectBoxList.hasParent() && this.style.backgroundOpen != null ? this.style.backgroundOpen : (this.clickListener.isOver() && this.style.backgroundOver != null ? this.style.backgroundOver : (this.style.background != null ? this.style.background : null)));
        BitmapFont font = this.style.font;
        Color fontColor = this.disabled && this.style.disabledFontColor != null ? this.style.disabledFontColor : this.style.fontColor;
        Color color = this.getColor();
        float x = this.getX();
        float y = this.getY();
        float width = this.getWidth();
        float height = this.getHeight();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        if (background != null) {
            background.draw(batch, x, y, width, height);
        }
        if ((selected = this.selection.first()) != null) {
            String string = this.toString(selected);
            if (background != null) {
                width -= background.getLeftWidth() + background.getRightWidth();
                x += background.getLeftWidth();
                y += (float)((int)((height -= background.getBottomHeight() + background.getTopHeight()) / 2.0f + background.getBottomHeight() + font.getData().capHeight / 2.0f));
            } else {
                y += (float)((int)(height / 2.0f + font.getData().capHeight / 2.0f));
            }
            font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha);
            this.layout.setText(font, string, 0, string.length(), font.getColor(), width, 8, false, "...");
            font.draw(batch, this.layout, x, y);
        }
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
        } else if (this.items.size > 0) {
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
        this.selection.set(this.items.get(index));
    }

    @Override
    public void setDisabled(boolean disabled) {
        if (disabled && !this.disabled) {
            this.hideList();
        }
        this.disabled = disabled;
    }

    @Override
    public boolean isDisabled() {
        return this.disabled;
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

    public void showList() {
        if (this.items.size == 0) {
            return;
        }
        this.selectBoxList.show(this.getStage());
    }

    public void hideList() {
        this.selectBoxList.hide();
    }

    public List<T> getList() {
        return this.selectBoxList.list;
    }

    public void setScrollingDisabled(boolean y) {
        this.selectBoxList.setScrollingDisabled(true, y);
        this.invalidateHierarchy();
    }

    public ScrollPane getScrollPane() {
        return this.selectBoxList;
    }

    protected void onShow(Actor selectBoxList, boolean below) {
        selectBoxList.getColor().a = 0.0f;
        selectBoxList.addAction(Actions.fadeIn(0.3f, Interpolation.fade));
    }

    protected void onHide(Actor selectBoxList) {
        selectBoxList.getColor().a = 1.0f;
        selectBoxList.addAction(Actions.sequence((Action)Actions.fadeOut(0.15f, Interpolation.fade), (Action)Actions.removeActor()));
    }

    public static class SelectBoxStyle {
        public BitmapFont font;
        public Color fontColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        public Color disabledFontColor;
        public Drawable background;
        public ScrollPane.ScrollPaneStyle scrollStyle;
        public List.ListStyle listStyle;
        public Drawable backgroundOver;
        public Drawable backgroundOpen;
        public Drawable backgroundDisabled;

        public SelectBoxStyle() {
        }

        public SelectBoxStyle(BitmapFont font, Color fontColor, Drawable background, ScrollPane.ScrollPaneStyle scrollStyle, List.ListStyle listStyle) {
            this.font = font;
            this.fontColor.set(fontColor);
            this.background = background;
            this.scrollStyle = scrollStyle;
            this.listStyle = listStyle;
        }

        public SelectBoxStyle(SelectBoxStyle style) {
            this.font = style.font;
            this.fontColor.set(style.fontColor);
            if (style.disabledFontColor != null) {
                this.disabledFontColor = new Color(style.disabledFontColor);
            }
            this.background = style.background;
            this.backgroundOver = style.backgroundOver;
            this.backgroundOpen = style.backgroundOpen;
            this.backgroundDisabled = style.backgroundDisabled;
            this.scrollStyle = new ScrollPane.ScrollPaneStyle(style.scrollStyle);
            this.listStyle = new List.ListStyle(style.listStyle);
        }
    }

    static class SelectBoxList<T>
    extends ScrollPane {
        private final SelectBox<T> selectBox;
        int maxListCount;
        private final Vector2 screenPosition = new Vector2();
        final List<T> list;
        private InputListener hideListener;
        private Actor previousScrollFocus;

        public SelectBoxList(final SelectBox<T> selectBox) {
            super(null, selectBox.style.scrollStyle);
            this.selectBox = selectBox;
            this.setOverscroll(false, false);
            this.setFadeScrollBars(false);
            this.setScrollingDisabled(true, false);
            this.list = new List<T>(selectBox.style.listStyle){

                @Override
                protected String toString(T obj) {
                    return selectBox.toString(obj);
                }
            };
            this.list.setTouchable(Touchable.disabled);
            this.setWidget(this.list);
            this.list.addListener(new ClickListener(){

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectBox.selection.choose(SelectBoxList.this.list.getSelected());
                    SelectBoxList.this.hide();
                }

                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    SelectBoxList.this.list.setSelectedIndex(Math.min(selectBox.items.size - 1, (int)((SelectBoxList.this.list.getHeight() - y) / SelectBoxList.this.list.getItemHeight())));
                    return true;
                }
            });
            this.addListener(new InputListener(){

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (toActor == null || !SelectBoxList.this.isAscendantOf(toActor)) {
                        SelectBoxList.this.list.selection.set(selectBox.getSelected());
                    }
                }
            });
            this.hideListener = new InputListener(){

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    Actor target = event.getTarget();
                    if (SelectBoxList.this.isAscendantOf(target)) {
                        return false;
                    }
                    SelectBoxList.this.list.selection.set(selectBox.getSelected());
                    SelectBoxList.this.hide();
                    return false;
                }

                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == 131) {
                        SelectBoxList.this.hide();
                    }
                    return false;
                }
            };
        }

        public void show(Stage stage) {
            Drawable listBackground;
            if (this.list.isTouchable()) {
                return;
            }
            stage.removeCaptureListener(this.hideListener);
            stage.addCaptureListener(this.hideListener);
            stage.addActor(this);
            this.selectBox.localToStageCoordinates(this.screenPosition.set(0.0f, 0.0f));
            float itemHeight = this.list.getItemHeight();
            float height = itemHeight * (float)(this.maxListCount <= 0 ? this.selectBox.items.size : Math.min(this.maxListCount, this.selectBox.items.size));
            Drawable scrollPaneBackground = this.getStyle().background;
            if (scrollPaneBackground != null) {
                height += scrollPaneBackground.getTopHeight() + scrollPaneBackground.getBottomHeight();
            }
            if ((listBackground = this.list.getStyle().background) != null) {
                height += listBackground.getTopHeight() + listBackground.getBottomHeight();
            }
            float heightBelow = this.screenPosition.y;
            float heightAbove = stage.getCamera().viewportHeight - this.screenPosition.y - this.selectBox.getHeight();
            boolean below = true;
            if (height > heightBelow) {
                if (heightAbove > heightBelow) {
                    below = false;
                    height = Math.min(height, heightAbove);
                } else {
                    height = heightBelow;
                }
            }
            if (below) {
                this.setY(this.screenPosition.y - height);
            } else {
                this.setY(this.screenPosition.y + this.selectBox.getHeight());
            }
            this.setX(this.screenPosition.x);
            this.setHeight(height);
            this.validate();
            float width = Math.max(this.getPrefWidth(), this.selectBox.getWidth());
            if (this.getPrefHeight() > height && !this.disableY) {
                width += this.getScrollBarWidth();
            }
            this.setWidth(width);
            this.validate();
            this.scrollTo(0.0f, this.list.getHeight() - (float)this.selectBox.getSelectedIndex() * itemHeight - itemHeight / 2.0f, 0.0f, 0.0f, true, true);
            this.updateVisualScroll();
            this.previousScrollFocus = null;
            Actor actor = stage.getScrollFocus();
            if (actor != null && !actor.isDescendantOf(this)) {
                this.previousScrollFocus = actor;
            }
            stage.setScrollFocus(this);
            this.list.selection.set(this.selectBox.getSelected());
            this.list.setTouchable(Touchable.enabled);
            this.clearActions();
            this.selectBox.onShow(this, below);
        }

        public void hide() {
            if (!this.list.isTouchable() || !this.hasParent()) {
                return;
            }
            this.list.setTouchable(Touchable.disabled);
            Stage stage = this.getStage();
            if (stage != null) {
                Actor actor;
                stage.removeCaptureListener(this.hideListener);
                if (this.previousScrollFocus != null && this.previousScrollFocus.getStage() == null) {
                    this.previousScrollFocus = null;
                }
                if ((actor = stage.getScrollFocus()) == null || this.isAscendantOf(actor)) {
                    stage.setScrollFocus(this.previousScrollFocus);
                }
            }
            this.clearActions();
            this.selectBox.onHide(this);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            this.selectBox.localToStageCoordinates(temp.set(0.0f, 0.0f));
            if (!temp.equals(this.screenPosition)) {
                this.hide();
            }
            super.draw(batch, parentAlpha);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            this.toFront();
        }
    }
}

