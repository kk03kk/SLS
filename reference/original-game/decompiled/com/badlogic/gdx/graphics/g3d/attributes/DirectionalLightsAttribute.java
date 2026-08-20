/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.utils.Array;

public class DirectionalLightsAttribute
extends Attribute {
    public static final String Alias = "directionalLights";
    public static final long Type = DirectionalLightsAttribute.register("directionalLights");
    public final Array<DirectionalLight> lights = new Array(1);

    public static final boolean is(long mask) {
        return (mask & Type) == mask;
    }

    public DirectionalLightsAttribute() {
        super(Type);
    }

    public DirectionalLightsAttribute(DirectionalLightsAttribute copyFrom) {
        this();
        this.lights.addAll(copyFrom.lights);
    }

    @Override
    public DirectionalLightsAttribute copy() {
        return new DirectionalLightsAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (DirectionalLight light : this.lights) {
            result = 1229 * result + (light == null ? 0 : light.hashCode());
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

