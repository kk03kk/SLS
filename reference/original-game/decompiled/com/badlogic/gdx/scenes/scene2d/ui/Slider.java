/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;

public class Slider
extends ProgressBar {
    int draggingPointer = -1;
    boolean mouseOver;
    private Interpolation visualInterpolationInverse = Interpolation.linear;
    private float[] snapValues;
    private float threshold;

    public Slider(float min, float max, float stepSize, boolean vertical, Skin skin) {
        this(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), SliderStyle.class));
    }

    public Slider(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        this(min, max, stepSize, vertical, skin.get(styleName, SliderStyle.class));
    }

    public Slider(float min, float max, float stepSize, boolean vertical, SliderStyle style) {
        super(min, max, stepSize, vertical, style);
        this.addListener(new InputListener(){

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (Slider.this.disabled) {
                    return false;
                }
                if (Slider.this.draggingPointer != -1) {
                    return false;
                }
                Slider.this.draggingPointer = pointer;
                Slider.this.calculatePositionAndValue(x, y);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != Slider.this.draggingPointer) {
                    return;
                }
                Slider.this.draggingPointer = -1;
                if (!Slider.this.calculatePositionAndValue(x, y)) {
                    ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
                    Slider.this.fire(changeEvent);
                    Pools.free(changeEvent);
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                Slider.this.calculatePositionAndValue(x, y);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    Slider.this.mouseOver = true;
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) {
                    Slider.this.mouseOver = false;
                }
            }
        });
    }

    public void setStyle(SliderStyle style) {
        if (style == null) {
            throw new NullPointerException("style cannot be null");
        }
        if (!(style instanceof SliderStyle)) {
            throw new IllegalArgumentException("style must be a SliderStyle.");
        }
        super.setStyle(style);
    }

    @Override
    public SliderStyle getStyle() {
        return (SliderStyle)super.getStyle();
    }

    @Override
    protected Drawable getKnobDrawable() {
        SliderStyle style = this.getStyle();
        return this.disabled && style.disabledKnob != null ? style.disabledKnob : (this.isDragging() && style.knobDown != null ? style.knobDown : (this.mouseOver && style.knobOver != null ? style.knobOver : style.knob));
    }

    boolean calculatePositionAndValue(float x, float y) {
        float value;
        SliderStyle style = this.getStyle();
        Drawable knob = this.getKnobDrawable();
        Drawable bg = this.disabled && style.disabledBackground != null ? style.disabledBackground : style.background;
        float oldPosition = this.position;
        float min = this.getMinValue();
        float max = this.getMaxValue();
        if (this.vertical) {
            float height = this.getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0.0f : knob.getMinHeight();
            this.position = y - bg.getBottomHeight() - knobHeight * 0.5f;
            value = min + (max - min) * this.visualInterpolationInverse.apply(this.position / (height - knobHeight));
            this.position = Math.max(0.0f, this.position);
            this.position = Math.min(height - knobHeight, this.position);
        } else {
            float width = this.getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0.0f : knob.getMinWidth();
            this.position = x - bg.getLeftWidth() - knobWidth * 0.5f;
            value = min + (max - min) * this.visualInterpolationInverse.apply(this.position / (width - knobWidth));
            this.position = Math.max(0.0f, this.position);
            this.position = Math.min(width - knobWidth, this.position);
        }
        float oldValue = value;
        if (!Gdx.input.isKeyPressed(59) && !Gdx.input.isKeyPressed(60)) {
            value = this.snap(value);
        }
        boolean valueSet = this.setValue(value);
        if (value == oldValue) {
            this.position = oldPosition;
        }
        return valueSet;
    }

    protected float snap(float value) {
        if (this.snapValues == null) {
            return value;
        }
        for (int i = 0; i < this.snapValues.length; ++i) {
            if (!(Math.abs(value - this.snapValues[i]) <= this.threshold)) continue;
            return this.snapValues[i];
        }
        return value;
    }

    public void setSnapToValues(float[] values, float threshold) {
        this.snapValues = values;
        this.threshold = threshold;
    }

    public boolean isDragging() {
        return this.draggingPointer != -1;
    }

    public void setVisualInterpolationInverse(Interpolation interpolation) {
        this.visualInterpolationInverse = interpolation;
    }

    public static class SliderStyle
    extends ProgressBar.ProgressBarStyle {
        public Drawable knobOver;
        public Drawable knobDown;

        public SliderStyle() {
        }

        public SliderStyle(Drawable background, Drawable knob) {
            super(background, knob);
        }

        public SliderStyle(SliderStyle style) {
            super(style);
            this.knobOver = style.knobOver;
            this.knobDown = style.knobDown;
        }
    }
}

