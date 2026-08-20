/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;

public class ProgressBar
extends Widget
implements Disableable {
    private ProgressBarStyle style;
    private float min;
    private float max;
    private float stepSize;
    private float value;
    private float animateFromValue;
    float position;
    final boolean vertical;
    private float animateDuration;
    private float animateTime;
    private Interpolation animateInterpolation = Interpolation.linear;
    boolean disabled;
    private Interpolation visualInterpolation = Interpolation.linear;
    private boolean round = true;

    public ProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin) {
        this(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), ProgressBarStyle.class));
    }

    public ProgressBar(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        this(min, max, stepSize, vertical, skin.get(styleName, ProgressBarStyle.class));
    }

    public ProgressBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style) {
        if (min > max) {
            throw new IllegalArgumentException("max must be > min. min,max: " + min + ", " + max);
        }
        if (stepSize <= 0.0f) {
            throw new IllegalArgumentException("stepSize must be > 0: " + stepSize);
        }
        this.setStyle(style);
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.vertical = vertical;
        this.value = min;
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
    }

    public void setStyle(ProgressBarStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        this.style = style;
        this.invalidateHierarchy();
    }

    public ProgressBarStyle getStyle() {
        return this.style;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (this.animateTime > 0.0f) {
            this.animateTime -= delta;
            Stage stage = this.getStage();
            if (stage != null && stage.getActionsRequestRendering()) {
                Gdx.graphics.requestRendering();
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        ProgressBarStyle style = this.style;
        boolean disabled = this.disabled;
        Drawable knob = this.getKnobDrawable();
        Drawable bg = disabled && style.disabledBackground != null ? style.disabledBackground : style.background;
        Drawable knobBefore = disabled && style.disabledKnobBefore != null ? style.disabledKnobBefore : style.knobBefore;
        Drawable knobAfter = disabled && style.disabledKnobAfter != null ? style.disabledKnobAfter : style.knobAfter;
        Color color = this.getColor();
        float x = this.getX();
        float y = this.getY();
        float width = this.getWidth();
        float height = this.getHeight();
        float knobHeight = knob == null ? 0.0f : knob.getMinHeight();
        float knobWidth = knob == null ? 0.0f : knob.getMinWidth();
        float percent = this.getVisualPercent();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        if (this.vertical) {
            float positionHeight = height;
            float bgTopHeight = 0.0f;
            if (bg != null) {
                if (this.round) {
                    bg.draw(batch, Math.round(x + (width - bg.getMinWidth()) * 0.5f), y, Math.round(bg.getMinWidth()), height);
                } else {
                    bg.draw(batch, x + width - bg.getMinWidth() * 0.5f, y, bg.getMinWidth(), height);
                }
                bgTopHeight = bg.getTopHeight();
                positionHeight -= bgTopHeight + bg.getBottomHeight();
            }
            float knobHeightHalf = 0.0f;
            if (this.min != this.max) {
                if (knob == null) {
                    knobHeightHalf = knobBefore == null ? 0.0f : knobBefore.getMinHeight() * 0.5f;
                    this.position = (positionHeight - knobHeightHalf) * percent;
                    this.position = Math.min(positionHeight - knobHeightHalf, this.position);
                } else {
                    knobHeightHalf = knobHeight * 0.5f;
                    this.position = (positionHeight - knobHeight) * percent;
                    this.position = Math.min(positionHeight - knobHeight, this.position) + bg.getBottomHeight();
                }
                this.position = Math.max(0.0f, this.position);
            }
            if (knobBefore != null) {
                float offset = 0.0f;
                if (bg != null) {
                    offset = bgTopHeight;
                }
                if (this.round) {
                    knobBefore.draw(batch, Math.round(x + (width - knobBefore.getMinWidth()) * 0.5f), Math.round(y + offset), Math.round(knobBefore.getMinWidth()), Math.round(this.position + knobHeightHalf));
                } else {
                    knobBefore.draw(batch, x + (width - knobBefore.getMinWidth()) * 0.5f, y + offset, knobBefore.getMinWidth(), this.position + knobHeightHalf);
                }
            }
            if (knobAfter != null) {
                if (this.round) {
                    knobAfter.draw(batch, Math.round(x + (width - knobAfter.getMinWidth()) * 0.5f), Math.round(y + this.position + knobHeightHalf), Math.round(knobAfter.getMinWidth()), Math.round(height - this.position - knobHeightHalf));
                } else {
                    knobAfter.draw(batch, x + (width - knobAfter.getMinWidth()) * 0.5f, y + this.position + knobHeightHalf, knobAfter.getMinWidth(), height - this.position - knobHeightHalf);
                }
            }
            if (knob != null) {
                if (this.round) {
                    knob.draw(batch, Math.round(x + (width - knobWidth) * 0.5f), Math.round(y + this.position), Math.round(knobWidth), Math.round(knobHeight));
                } else {
                    knob.draw(batch, x + (width - knobWidth) * 0.5f, y + this.position, knobWidth, knobHeight);
                }
            }
        } else {
            float positionWidth = width;
            float bgLeftWidth = 0.0f;
            if (bg != null) {
                if (this.round) {
                    bg.draw(batch, x, Math.round(y + (height - bg.getMinHeight()) * 0.5f), width, Math.round(bg.getMinHeight()));
                } else {
                    bg.draw(batch, x, y + (height - bg.getMinHeight()) * 0.5f, width, bg.getMinHeight());
                }
                bgLeftWidth = bg.getLeftWidth();
                positionWidth -= bgLeftWidth + bg.getRightWidth();
            }
            float knobWidthHalf = 0.0f;
            if (this.min != this.max) {
                if (knob == null) {
                    knobWidthHalf = knobBefore == null ? 0.0f : knobBefore.getMinWidth() * 0.5f;
                    this.position = (positionWidth - knobWidthHalf) * percent;
                    this.position = Math.min(positionWidth - knobWidthHalf, this.position);
                } else {
                    knobWidthHalf = knobWidth * 0.5f;
                    this.position = (positionWidth - knobWidth) * percent;
                    this.position = Math.min(positionWidth - knobWidth, this.position) + bgLeftWidth;
                }
                this.position = Math.max(0.0f, this.position);
            }
            if (knobBefore != null) {
                float offset = 0.0f;
                if (bg != null) {
                    offset = bgLeftWidth;
                }
                if (this.round) {
                    knobBefore.draw(batch, Math.round(x + offset), Math.round(y + (height - knobBefore.getMinHeight()) * 0.5f), Math.round(this.position + knobWidthHalf), Math.round(knobBefore.getMinHeight()));
                } else {
                    knobBefore.draw(batch, x + offset, y + (height - knobBefore.getMinHeight()) * 0.5f, this.position + knobWidthHalf, knobBefore.getMinHeight());
                }
            }
            if (knobAfter != null) {
                if (this.round) {
                    knobAfter.draw(batch, Math.round(x + this.position + knobWidthHalf), Math.round(y + (height - knobAfter.getMinHeight()) * 0.5f), Math.round(width - this.position - knobWidthHalf), Math.round(knobAfter.getMinHeight()));
                } else {
                    knobAfter.draw(batch, x + this.position + knobWidthHalf, y + (height - knobAfter.getMinHeight()) * 0.5f, width - this.position - knobWidthHalf, knobAfter.getMinHeight());
                }
            }
            if (knob != null) {
                if (this.round) {
                    knob.draw(batch, Math.round(x + this.position), Math.round(y + (height - knobHeight) * 0.5f), Math.round(knobWidth), Math.round(knobHeight));
                } else {
                    knob.draw(batch, x + this.position, y + (height - knobHeight) * 0.5f, knobWidth, knobHeight);
                }
            }
        }
    }

    public float getValue() {
        return this.value;
    }

    public float getVisualValue() {
        if (this.animateTime > 0.0f) {
            return this.animateInterpolation.apply(this.animateFromValue, this.value, 1.0f - this.animateTime / this.animateDuration);
        }
        return this.value;
    }

    public float getPercent() {
        return (this.value - this.min) / (this.max - this.min);
    }

    public float getVisualPercent() {
        return this.visualInterpolation.apply((this.getVisualValue() - this.min) / (this.max - this.min));
    }

    protected Drawable getKnobDrawable() {
        return this.disabled && this.style.disabledKnob != null ? this.style.disabledKnob : this.style.knob;
    }

    protected float getKnobPosition() {
        return this.position;
    }

    public boolean setValue(float value) {
        float oldValue;
        if ((value = this.clamp((float)Math.round(value / this.stepSize) * this.stepSize)) == (oldValue = this.value)) {
            return false;
        }
        float oldVisualValue = this.getVisualValue();
        this.value = value;
        ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
        boolean cancelled = this.fire(changeEvent);
        if (cancelled) {
            this.value = oldValue;
        } else if (this.animateDuration > 0.0f) {
            this.animateFromValue = oldVisualValue;
            this.animateTime = this.animateDuration;
        }
        Pools.free(changeEvent);
        return !cancelled;
    }

    protected float clamp(float value) {
        return MathUtils.clamp(value, this.min, this.max);
    }

    public void setRange(float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        this.min = min;
        this.max = max;
        if (this.value < min) {
            this.setValue(min);
        } else if (this.value > max) {
            this.setValue(max);
        }
    }

    public void setStepSize(float stepSize) {
        if (stepSize <= 0.0f) {
            throw new IllegalArgumentException("steps must be > 0: " + stepSize);
        }
        this.stepSize = stepSize;
    }

    @Override
    public float getPrefWidth() {
        if (this.vertical) {
            Drawable knob = this.getKnobDrawable();
            Drawable bg = this.disabled && this.style.disabledBackground != null ? this.style.disabledBackground : this.style.background;
            return Math.max(knob == null ? 0.0f : knob.getMinWidth(), bg.getMinWidth());
        }
        return 140.0f;
    }

    @Override
    public float getPrefHeight() {
        if (this.vertical) {
            return 140.0f;
        }
        Drawable knob = this.getKnobDrawable();
        Drawable bg = this.disabled && this.style.disabledBackground != null ? this.style.disabledBackground : this.style.background;
        return Math.max(knob == null ? 0.0f : knob.getMinHeight(), bg == null ? 0.0f : bg.getMinHeight());
    }

    public float getMinValue() {
        return this.min;
    }

    public float getMaxValue() {
        return this.max;
    }

    public float getStepSize() {
        return this.stepSize;
    }

    public void setAnimateDuration(float duration) {
        this.animateDuration = duration;
    }

    public void setAnimateInterpolation(Interpolation animateInterpolation) {
        if (animateInterpolation == null) {
            throw new IllegalArgumentException("animateInterpolation cannot be null.");
        }
        this.animateInterpolation = animateInterpolation;
    }

    public void setVisualInterpolation(Interpolation interpolation) {
        this.visualInterpolation = interpolation;
    }

    public void setRound(boolean round) {
        this.round = round;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean isDisabled() {
        return this.disabled;
    }

    public static class ProgressBarStyle {
        public Drawable background;
        public Drawable disabledBackground;
        public Drawable knob;
        public Drawable disabledKnob;
        public Drawable knobBefore;
        public Drawable knobAfter;
        public Drawable disabledKnobBefore;
        public Drawable disabledKnobAfter;

        public ProgressBarStyle() {
        }

        public ProgressBarStyle(Drawable background, Drawable knob) {
            this.background = background;
            this.knob = knob;
        }

        public ProgressBarStyle(ProgressBarStyle style) {
            this.background = style.background;
            this.disabledBackground = style.disabledBackground;
            this.knob = style.knob;
            this.disabledKnob = style.disabledKnob;
            this.knobBefore = style.knobBefore;
            this.knobAfter = style.knobAfter;
            this.disabledKnobBefore = style.disabledKnobBefore;
            this.disabledKnobAfter = style.disabledKnobAfter;
        }
    }
}

