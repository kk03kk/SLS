/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

public class ImageTextButton
extends Button {
    private final Image image;
    private final Label label;
    private ImageTextButtonStyle style;

    public ImageTextButton(String text, Skin skin) {
        this(text, skin.get(ImageTextButtonStyle.class));
        this.setSkin(skin);
    }

    public ImageTextButton(String text, Skin skin, String styleName) {
        this(text, skin.get(styleName, ImageTextButtonStyle.class));
        this.setSkin(skin);
    }

    public ImageTextButton(String text, ImageTextButtonStyle style) {
        super(style);
        this.style = style;
        this.defaults().space(3.0f);
        this.image = new Image();
        this.image.setScaling(Scaling.fit);
        this.label = new Label((CharSequence)text, new Label.LabelStyle(style.font, style.fontColor));
        this.label.setAlignment(1);
        this.add(this.image);
        this.add(this.label);
        this.setStyle(style);
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
    }

    @Override
    public void setStyle(Button.ButtonStyle style) {
        if (!(style instanceof ImageTextButtonStyle)) {
            throw new IllegalArgumentException("style must be a ImageTextButtonStyle.");
        }
        super.setStyle(style);
        this.style = (ImageTextButtonStyle)style;
        if (this.image != null) {
            this.updateImage();
        }
        if (this.label != null) {
            ImageTextButtonStyle textButtonStyle = (ImageTextButtonStyle)style;
            Label.LabelStyle labelStyle = this.label.getStyle();
            labelStyle.font = textButtonStyle.font;
            labelStyle.fontColor = textButtonStyle.fontColor;
            this.label.setStyle(labelStyle);
        }
    }

    @Override
    public ImageTextButtonStyle getStyle() {
        return this.style;
    }

    private void updateImage() {
        Drawable drawable = null;
        if (this.isDisabled() && this.style.imageDisabled != null) {
            drawable = this.style.imageDisabled;
        } else if (this.isPressed() && this.style.imageDown != null) {
            drawable = this.style.imageDown;
        } else if (this.isChecked && this.style.imageChecked != null) {
            drawable = this.style.imageCheckedOver != null && this.isOver() ? this.style.imageCheckedOver : this.style.imageChecked;
        } else if (this.isOver() && this.style.imageOver != null) {
            drawable = this.style.imageOver;
        } else if (this.style.imageUp != null) {
            drawable = this.style.imageUp;
        }
        this.image.setDrawable(drawable);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        this.updateImage();
        Color fontColor = this.isDisabled() && this.style.disabledFontColor != null ? this.style.disabledFontColor : (this.isPressed() && this.style.downFontColor != null ? this.style.downFontColor : (this.isChecked && this.style.checkedFontColor != null ? (this.isOver() && this.style.checkedOverFontColor != null ? this.style.checkedOverFontColor : this.style.checkedFontColor) : (this.isOver() && this.style.overFontColor != null ? this.style.overFontColor : this.style.fontColor)));
        if (fontColor != null) {
            this.label.getStyle().fontColor = fontColor;
        }
        super.draw(batch, parentAlpha);
    }

    public Image getImage() {
        return this.image;
    }

    public Cell getImageCell() {
        return this.getCell(this.image);
    }

    public Label getLabel() {
        return this.label;
    }

    public Cell getLabelCell() {
        return this.getCell(this.label);
    }

    public void setText(CharSequence text) {
        this.label.setText(text);
    }

    public CharSequence getText() {
        return this.label.getText();
    }

    public static class ImageTextButtonStyle
    extends TextButton.TextButtonStyle {
        public Drawable imageUp;
        public Drawable imageDown;
        public Drawable imageOver;
        public Drawable imageChecked;
        public Drawable imageCheckedOver;
        public Drawable imageDisabled;

        public ImageTextButtonStyle() {
        }

        public ImageTextButtonStyle(Drawable up, Drawable down, Drawable checked, BitmapFont font) {
            super(up, down, checked, font);
        }

        public ImageTextButtonStyle(ImageTextButtonStyle style) {
            super(style);
            if (style.imageUp != null) {
                this.imageUp = style.imageUp;
            }
            if (style.imageDown != null) {
                this.imageDown = style.imageDown;
            }
            if (style.imageOver != null) {
                this.imageOver = style.imageOver;
            }
            if (style.imageChecked != null) {
                this.imageChecked = style.imageChecked;
            }
            if (style.imageCheckedOver != null) {
                this.imageCheckedOver = style.imageCheckedOver;
            }
            if (style.imageDisabled != null) {
                this.imageDisabled = style.imageDisabled;
            }
        }

        public ImageTextButtonStyle(TextButton.TextButtonStyle style) {
            super(style);
        }
    }
}

