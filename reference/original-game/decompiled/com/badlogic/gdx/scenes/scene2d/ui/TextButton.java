/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class TextButton
extends Button {
    private final Label label;
    private TextButtonStyle style;

    public TextButton(String text, Skin skin) {
        this(text, skin.get(TextButtonStyle.class));
        this.setSkin(skin);
    }

    public TextButton(String text, Skin skin, String styleName) {
        this(text, skin.get(styleName, TextButtonStyle.class));
        this.setSkin(skin);
    }

    public TextButton(String text, TextButtonStyle style) {
        this.setStyle(style);
        this.style = style;
        this.label = new Label((CharSequence)text, new Label.LabelStyle(style.font, style.fontColor));
        this.label.setAlignment(1);
        this.add(this.label).expand().fill();
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
    }

    @Override
    public void setStyle(Button.ButtonStyle style) {
        if (style == null) {
            throw new NullPointerException("style cannot be null");
        }
        if (!(style instanceof TextButtonStyle)) {
            throw new IllegalArgumentException("style must be a TextButtonStyle.");
        }
        super.setStyle(style);
        this.style = (TextButtonStyle)style;
        if (this.label != null) {
            TextButtonStyle textButtonStyle = (TextButtonStyle)style;
            Label.LabelStyle labelStyle = this.label.getStyle();
            labelStyle.font = textButtonStyle.font;
            labelStyle.fontColor = textButtonStyle.fontColor;
            this.label.setStyle(labelStyle);
        }
    }

    @Override
    public TextButtonStyle getStyle() {
        return this.style;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color fontColor = this.isDisabled() && this.style.disabledFontColor != null ? this.style.disabledFontColor : (this.isPressed() && this.style.downFontColor != null ? this.style.downFontColor : (this.isChecked && this.style.checkedFontColor != null ? (this.isOver() && this.style.checkedOverFontColor != null ? this.style.checkedOverFontColor : this.style.checkedFontColor) : (this.isOver() && this.style.overFontColor != null ? this.style.overFontColor : this.style.fontColor)));
        if (fontColor != null) {
            this.label.getStyle().fontColor = fontColor;
        }
        super.draw(batch, parentAlpha);
    }

    public Label getLabel() {
        return this.label;
    }

    public Cell<Label> getLabelCell() {
        return this.getCell(this.label);
    }

    public void setText(String text) {
        this.label.setText(text);
    }

    public CharSequence getText() {
        return this.label.getText();
    }

    public static class TextButtonStyle
    extends Button.ButtonStyle {
        public BitmapFont font;
        public Color fontColor;
        public Color downFontColor;
        public Color overFontColor;
        public Color checkedFontColor;
        public Color checkedOverFontColor;
        public Color disabledFontColor;

        public TextButtonStyle() {
        }

        public TextButtonStyle(Drawable up, Drawable down, Drawable checked, BitmapFont font) {
            super(up, down, checked);
            this.font = font;
        }

        public TextButtonStyle(TextButtonStyle style) {
            super(style);
            this.font = style.font;
            if (style.fontColor != null) {
                this.fontColor = new Color(style.fontColor);
            }
            if (style.downFontColor != null) {
                this.downFontColor = new Color(style.downFontColor);
            }
            if (style.overFontColor != null) {
                this.overFontColor = new Color(style.overFontColor);
            }
            if (style.checkedFontColor != null) {
                this.checkedFontColor = new Color(style.checkedFontColor);
            }
            if (style.checkedOverFontColor != null) {
                this.checkedFontColor = new Color(style.checkedOverFontColor);
            }
            if (style.disabledFontColor != null) {
                this.disabledFontColor = new Color(style.disabledFontColor);
            }
        }
    }
}

