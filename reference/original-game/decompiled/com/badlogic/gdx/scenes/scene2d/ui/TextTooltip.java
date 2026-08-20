/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class TextTooltip
extends Tooltip<Label> {
    public TextTooltip(String text, Skin skin) {
        this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class));
    }

    public TextTooltip(String text, Skin skin, String styleName) {
        this(text, TooltipManager.getInstance(), skin.get(styleName, TextTooltipStyle.class));
    }

    public TextTooltip(String text, TextTooltipStyle style) {
        this(text, TooltipManager.getInstance(), style);
    }

    public TextTooltip(String text, TooltipManager manager, Skin skin) {
        this(text, manager, skin.get(TextTooltipStyle.class));
    }

    public TextTooltip(String text, TooltipManager manager, Skin skin, String styleName) {
        this(text, manager, skin.get(styleName, TextTooltipStyle.class));
    }

    public TextTooltip(String text, final TooltipManager manager, TextTooltipStyle style) {
        super(null, manager);
        Label label = new Label((CharSequence)text, style.label);
        label.setWrap(true);
        this.container.setActor(label);
        this.container.width(new Value(){

            @Override
            public float get(Actor context) {
                return Math.min(manager.maxWidth, ((Label)TextTooltip.this.container.getActor()).getGlyphLayout().width);
            }
        });
        this.setStyle(style);
    }

    public void setStyle(TextTooltipStyle style) {
        if (style == null) {
            throw new NullPointerException("style cannot be null");
        }
        if (!(style instanceof TextTooltipStyle)) {
            throw new IllegalArgumentException("style must be a TextTooltipStyle.");
        }
        ((Label)this.container.getActor()).setStyle(style.label);
        this.container.setBackground(style.background);
        this.container.maxWidth(style.wrapWidth);
    }

    public static class TextTooltipStyle {
        public Label.LabelStyle label;
        public Drawable background;
        public float wrapWidth;

        public TextTooltipStyle() {
        }

        public TextTooltipStyle(Label.LabelStyle label, Drawable background) {
            this.label = label;
            this.background = background;
        }

        public TextTooltipStyle(TextTooltipStyle style) {
            this.label = new Label.LabelStyle(style.label);
            this.background = style.background;
        }
    }
}

