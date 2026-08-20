/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont.effects;

import com.badlogic.gdx.tools.hiero.unicodefont.effects.ConfigurableEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.EffectUtil;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.OutlineEffect;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.util.List;

public class OutlineZigzagEffect
extends OutlineEffect {
    float amplitude = 1.0f;
    float wavelength = 3.0f;

    public OutlineZigzagEffect() {
        this.setStroke(new ZigzagStroke());
    }

    public OutlineZigzagEffect(int width, Color color) {
        super(width, color);
    }

    @Override
    public String toString() {
        return "Outline (Zigzag)";
    }

    @Override
    public List getValues() {
        List values = super.getValues();
        values.add(EffectUtil.floatValue("Wavelength", this.wavelength, 1.0f, 100.0f, "This setting controls the wavelength of the outline. The smaller the value, the more segments will be used to draw the outline."));
        values.add(EffectUtil.floatValue("Amplitude", this.amplitude, 0.5f, 50.0f, "This setting controls the amplitude of the outline. The bigger the value, the more the zigzags will vary."));
        return values;
    }

    @Override
    public void setValues(List values) {
        super.setValues(values);
        for (ConfigurableEffect.Value value : values) {
            if (value.getName().equals("Wavelength")) {
                this.wavelength = ((Float)value.getObject()).floatValue();
                continue;
            }
            if (!value.getName().equals("Amplitude")) continue;
            this.amplitude = ((Float)value.getObject()).floatValue();
        }
    }

    class ZigzagStroke
    implements Stroke {
        private static final float FLATNESS = 1.0f;

        ZigzagStroke() {
        }

        @Override
        public Shape createStrokedShape(Shape shape) {
            GeneralPath result = new GeneralPath();
            FlatteningPathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), 1.0);
            float[] points = new float[6];
            float moveX = 0.0f;
            float moveY = 0.0f;
            float lastX = 0.0f;
            float lastY = 0.0f;
            float thisX = 0.0f;
            float thisY = 0.0f;
            int type = 0;
            float next = 0.0f;
            int phase = 0;
            while (!it.isDone()) {
                type = it.currentSegment(points);
                switch (type) {
                    case 0: {
                        moveX = lastX = points[0];
                        moveY = lastY = points[1];
                        result.moveTo(moveX, moveY);
                        next = OutlineZigzagEffect.this.wavelength / 2.0f;
                        break;
                    }
                    case 4: {
                        points[0] = moveX;
                        points[1] = moveY;
                    }
                    case 1: {
                        thisX = points[0];
                        thisY = points[1];
                        float dx = thisX - lastX;
                        float dy = thisY - lastY;
                        float distance = (float)Math.sqrt(dx * dx + dy * dy);
                        if (distance >= next) {
                            float r = 1.0f / distance;
                            while (distance >= next) {
                                float x = lastX + next * dx * r;
                                float y = lastY + next * dy * r;
                                if (!(phase & true)) {
                                    result.lineTo(x + OutlineZigzagEffect.this.amplitude * dy * r, y - OutlineZigzagEffect.this.amplitude * dx * r);
                                } else {
                                    result.lineTo(x - OutlineZigzagEffect.this.amplitude * dy * r, y + OutlineZigzagEffect.this.amplitude * dx * r);
                                }
                                next += OutlineZigzagEffect.this.wavelength;
                                ++phase;
                            }
                        }
                        next -= distance;
                        lastX = thisX;
                        lastY = thisY;
                        if (type != 4) break;
                        result.closePath();
                    }
                }
                it.next();
            }
            return new BasicStroke(OutlineZigzagEffect.this.getWidth(), 2, OutlineZigzagEffect.this.getJoin()).createStrokedShape(result);
        }
    }
}

