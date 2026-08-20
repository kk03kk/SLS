/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class FloatAttribute
extends Attribute {
    public static final String ShininessAlias = "shininess";
    public static final long Shininess = FloatAttribute.register("shininess");
    public static final String AlphaTestAlias = "alphaTest";
    public static final long AlphaTest = FloatAttribute.register("alphaTest");
    public float value;

    public static FloatAttribute createShininess(float value) {
        return new FloatAttribute(Shininess, value);
    }

    public static FloatAttribute createAlphaTest(float value) {
        return new FloatAttribute(AlphaTest, value);
    }

    public FloatAttribute(long type) {
        super(type);
    }

    public FloatAttribute(long type, float value) {
        super(type);
        this.value = value;
    }

    @Override
    public Attribute copy() {
        return new FloatAttribute(this.type, this.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 977 * result + NumberUtils.floatToRawIntBits(this.value);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return (int)(this.type - o.type);
        }
        float v = ((FloatAttribute)o).value;
        return MathUtils.isEqual(this.value, v) ? 0 : (this.value < v ? -1 : 1);
    }
}

