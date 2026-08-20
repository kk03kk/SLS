/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class BlendingAttribute
extends Attribute {
    public static final String Alias = "blended";
    public static final long Type = BlendingAttribute.register("blended");
    public boolean blended;
    public int sourceFunction;
    public int destFunction;
    public float opacity = 1.0f;

    public static final boolean is(long mask) {
        return (mask & Type) == mask;
    }

    public BlendingAttribute() {
        this(null);
    }

    public BlendingAttribute(boolean blended, int sourceFunc, int destFunc, float opacity) {
        super(Type);
        this.blended = blended;
        this.sourceFunction = sourceFunc;
        this.destFunction = destFunc;
        this.opacity = opacity;
    }

    public BlendingAttribute(int sourceFunc, int destFunc, float opacity) {
        this(true, sourceFunc, destFunc, opacity);
    }

    public BlendingAttribute(int sourceFunc, int destFunc) {
        this(sourceFunc, destFunc, 1.0f);
    }

    public BlendingAttribute(boolean blended, float opacity) {
        this(blended, 770, 771, opacity);
    }

    public BlendingAttribute(float opacity) {
        this(true, opacity);
    }

    public BlendingAttribute(BlendingAttribute copyFrom) {
        this(copyFrom == null ? true : copyFrom.blended, copyFrom == null ? 770 : copyFrom.sourceFunction, copyFrom == null ? 771 : copyFrom.destFunction, copyFrom == null ? 1.0f : copyFrom.opacity);
    }

    @Override
    public BlendingAttribute copy() {
        return new BlendingAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 947 * result + (this.blended ? 1 : 0);
        result = 947 * result + this.sourceFunction;
        result = 947 * result + this.destFunction;
        result = 947 * result + NumberUtils.floatToRawIntBits(this.opacity);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return (int)(this.type - o.type);
        }
        BlendingAttribute other = (BlendingAttribute)o;
        if (this.blended != other.blended) {
            return this.blended ? 1 : -1;
        }
        if (this.sourceFunction != other.sourceFunction) {
            return this.sourceFunction - other.sourceFunction;
        }
        if (this.destFunction != other.destFunction) {
            return this.destFunction - other.destFunction;
        }
        return MathUtils.isEqual(this.opacity, other.opacity) ? 0 : (this.opacity < other.opacity ? 1 : -1);
    }
}

