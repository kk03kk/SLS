/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.StringBuilder;

public class Label
extends Widget {
    private static final Color tempColor = new Color();
    private static final GlyphLayout prefSizeLayout = new GlyphLayout();
    private LabelStyle style;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 prefSize = new Vector2();
    private final StringBuilder text = new StringBuilder();
    private BitmapFontCache cache;
    private int labelAlign = 8;
    private int lineAlign = 8;
    private boolean wrap;
    private float lastPrefHeight;
    private boolean prefSizeInvalid = true;
    private float fontScaleX = 1.0f;
    private float fontScaleY = 1.0f;
    private boolean fontScaleChanged = false;
    private String ellipsis;

    public Label(CharSequence text, Skin skin) {
        this(text, skin.get(LabelStyle.class));
    }

    public Label(CharSequence text, Skin skin, String styleName) {
        this(text, skin.get(styleName, LabelStyle.class));
    }

    public Label(CharSequence text, Skin skin, String fontName, Color color) {
        this(text, new LabelStyle(skin.getFont(fontName), color));
    }

    public Label(CharSequence text, Skin skin, String fontName, String colorName) {
        this(text, new LabelStyle(skin.getFont(fontName), skin.getColor(colorName)));
    }

    public Label(CharSequence text, LabelStyle style) {
        if (text != null) {
            this.text.append(text);
        }
        this.setStyle(style);
        if (text != null && text.length() > 0) {
            this.setSize(this.getPrefWidth(), this.getPrefHeight());
        }
    }

    public void setStyle(LabelStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        if (style.font == null) {
            throw new IllegalArgumentException("Missing LabelStyle font.");
        }
        this.style = style;
        this.cache = style.font.newFontCache();
        this.invalidateHierarchy();
    }

    public LabelStyle getStyle() {
        return this.style;
    }

    public void setText(CharSequence newText) {
        if (newText == null) {
            newText = "";
        }
        if (newText instanceof StringBuilder) {
            if (this.text.equals(newText)) {
                return;
            }
            this.text.setLength(0);
            this.text.append((StringBuilder)newText);
        } else {
            if (this.textEquals(newText)) {
                return;
            }
            this.text.setLength(0);
            this.text.append(newText);
        }
        this.invalidateHierarchy();
    }

    public boolean textEquals(CharSequence other) {
        int length = this.text.length;
        char[] chars = this.text.chars;
        if (length != other.length()) {
            return false;
        }
        for (int i = 0; i < length; ++i) {
            if (chars[i] == other.charAt(i)) continue;
            return false;
        }
        return true;
    }

    public StringBuilder getText() {
        return this.text;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.prefSizeInvalid = true;
    }

    private void scaleAndComputePrefSize() {
        BitmapFont font = this.cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if (this.fontScaleChanged) {
            font.getData().setScale(this.fontScaleX, this.fontScaleY);
        }
        this.computePrefSize();
        if (this.fontScaleChanged) {
            font.getData().setScale(oldScaleX, oldScaleY);
        }
    }

    private void computePrefSize() {
        this.prefSizeInvalid = false;
        GlyphLayout prefSizeLayout = Label.prefSizeLayout;
        if (this.wrap && this.ellipsis == null) {
            float width = this.getWidth();
            if (this.style.background != null) {
                width -= this.style.background.getLeftWidth() + this.style.background.getRightWidth();
            }
            prefSizeLayout.setText(this.cache.getFont(), this.text, Color.WHITE, width, 8, true);
        } else {
            prefSizeLayout.setText(this.cache.getFont(), this.text);
        }
        this.prefSize.set(prefSizeLayout.width, prefSizeLayout.height);
    }

    @Override
    public void layout() {
        float textHeight;
        float textWidth;
        float prefHeight;
        boolean wrap;
        BitmapFont font = this.cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if (this.fontScaleChanged) {
            font.getData().setScale(this.fontScaleX, this.fontScaleY);
        }
        boolean bl = wrap = this.wrap && this.ellipsis == null;
        if (wrap && (prefHeight = this.getPrefHeight()) != this.lastPrefHeight) {
            this.lastPrefHeight = prefHeight;
            this.invalidateHierarchy();
        }
        float width = this.getWidth();
        float height = this.getHeight();
        Drawable background = this.style.background;
        float x = 0.0f;
        float y = 0.0f;
        if (background != null) {
            x = background.getLeftWidth();
            y = background.getBottomHeight();
            width -= background.getLeftWidth() + background.getRightWidth();
            height -= background.getBottomHeight() + background.getTopHeight();
        }
        GlyphLayout layout = this.layout;
        if (wrap || this.text.indexOf("\n") != -1) {
            layout.setText(font, this.text, 0, this.text.length, Color.WHITE, width, this.lineAlign, wrap, this.ellipsis);
            textWidth = layout.width;
            textHeight = layout.height;
            if ((this.labelAlign & 8) == 0) {
                x = (this.labelAlign & 0x10) != 0 ? (x += width - textWidth) : (x += (width - textWidth) / 2.0f);
            }
        } else {
            textWidth = width;
            textHeight = font.getData().capHeight;
        }
        if ((this.labelAlign & 2) != 0) {
            y += this.cache.getFont().isFlipped() ? 0.0f : height - textHeight;
            y += this.style.font.getDescent();
        } else if ((this.labelAlign & 4) != 0) {
            y += this.cache.getFont().isFlipped() ? height - textHeight : 0.0f;
            y -= this.style.font.getDescent();
        } else {
            y += (height - textHeight) / 2.0f;
        }
        if (!this.cache.getFont().isFlipped()) {
            y += textHeight;
        }
        layout.setText(font, this.text, 0, this.text.length, Color.WHITE, textWidth, this.lineAlign, wrap, this.ellipsis);
        this.cache.setText(layout, x, y);
        if (this.fontScaleChanged) {
            font.getData().setScale(oldScaleX, oldScaleY);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.validate();
        Color color = tempColor.set(this.getColor());
        color.a *= parentAlpha;
        if (this.style.background != null) {
            batch.setColor(color.r, color.g, color.b, color.a);
            this.style.background.draw(batch, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
        if (this.style.fontColor != null) {
            color.mul(this.style.fontColor);
        }
        this.cache.tint(color);
        this.cache.setPosition(this.getX(), this.getY());
        this.cache.draw(batch);
    }

    @Override
    public float getPrefWidth() {
        if (this.wrap) {
            return 0.0f;
        }
        if (this.prefSizeInvalid) {
            this.scaleAndComputePrefSize();
        }
        float width = this.prefSize.x;
        Drawable background = this.style.background;
        if (background != null) {
            width += background.getLeftWidth() + background.getRightWidth();
        }
        return width;
    }

    @Override
    public float getPrefHeight() {
        if (this.prefSizeInvalid) {
            this.scaleAndComputePrefSize();
        }
        float height = this.prefSize.y - this.style.font.getDescent() * this.fontScaleY * 2.0f;
        Drawable background = this.style.background;
        if (background != null) {
            height += background.getTopHeight() + background.getBottomHeight();
        }
        return height;
    }

    public GlyphLayout getGlyphLayout() {
        return this.layout;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
        this.invalidateHierarchy();
    }

    public int getLabelAlign() {
        return this.labelAlign;
    }

    public int getLineAlign() {
        return this.lineAlign;
    }

    public void setAlignment(int alignment) {
        this.setAlignment(alignment, alignment);
    }

    public void setAlignment(int labelAlign, int lineAlign) {
        this.labelAlign = labelAlign;
        this.lineAlign = (lineAlign & 8) != 0 ? 8 : ((lineAlign & 0x10) != 0 ? 16 : 1);
        this.invalidate();
    }

    public void setFontScale(float fontScale) {
        this.setFontScale(fontScale, fontScale);
    }

    public void setFontScale(float fontScaleX, float fontScaleY) {
        this.fontScaleChanged = true;
        this.fontScaleX = fontScaleX;
        this.fontScaleY = fontScaleY;
        this.invalidateHierarchy();
    }

    public float getFontScaleX() {
        return this.fontScaleX;
    }

    public void setFontScaleX(float fontScaleX) {
        this.setFontScale(fontScaleX, this.fontScaleY);
    }

    public float getFontScaleY() {
        return this.fontScaleY;
    }

    public void setFontScaleY(float fontScaleY) {
        this.setFontScale(this.fontScaleX, fontScaleY);
    }

    public void setEllipsis(String ellipsis) {
        this.ellipsis = ellipsis;
    }

    public void setEllipsis(boolean ellipsis) {
        this.ellipsis = ellipsis ? "..." : null;
    }

    protected BitmapFontCache getBitmapFontCache() {
        return this.cache;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + this.text;
    }

    public static class LabelStyle {
        public BitmapFont font;
        public Color fontColor;
        public Drawable background;

        public LabelStyle() {
        }

        public LabelStyle(BitmapFont font, Color fontColor) {
            this.font = font;
            this.fontColor = fontColor;
        }

        public LabelStyle(LabelStyle style) {
            this.font = style.font;
            if (style.fontColor != null) {
                this.fontColor = new Color(style.fontColor);
            }
            this.background = style.background;
        }
    }
}

