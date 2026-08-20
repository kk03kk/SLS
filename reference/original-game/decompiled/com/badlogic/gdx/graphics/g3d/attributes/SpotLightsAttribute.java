/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.utils.Array;

public class SpotLightsAttribute
extends Attribute {
    public static final String Alias = "spotLights";
    public static final long Type = SpotLightsAttribute.register("spotLights");
    public final Array<SpotLight> lights = new Array(1);

    public static final boolean is(long mask) {
        return (mask & Type) == mask;
    }

    public SpotLightsAttribute() {
        super(Type);
    }

    public SpotLightsAttribute(SpotLightsAttribute copyFrom) {
        this();
        this.lights.addAll(copyFrom.lights);
    }

    @Override
    public SpotLightsAttribute copy() {
        return new SpotLightsAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (SpotLight light : this.lights) {
            result = 1237 * result + (light == null ? 0 : light.hashCode());
        }
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return this.type < o.type ? -1 : 1;
        }
        return 0;
    }
}

