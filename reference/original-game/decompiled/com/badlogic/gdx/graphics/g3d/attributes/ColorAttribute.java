/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ColorAttribute
extends Attribute {
    public static final String DiffuseAlias = "diffuseColor";
    public static final long Diffuse = ColorAttribute.register("diffuseColor");
    public static final String SpecularAlias = "specularColor";
    public static final long Specular = ColorAttribute.register("specularColor");
    public static final String AmbientAlias = "ambientColor";
    public static final long Ambient = ColorAttribute.register("ambientColor");
    public static final String EmissiveAlias = "emissiveColor";
    public static final long Emissive = ColorAttribute.register("emissiveColor");
    public static final String ReflectionAlias = "reflectionColor";
    public static final long Reflection = ColorAttribute.register("reflectionColor");
    public static final String AmbientLightAlias = "ambientLightColor";
    public static final long AmbientLight = ColorAttribute.register("ambientLightColor");
    public static final String FogAlias = "fogColor";
    public static final long Fog = ColorAttribute.register("fogColor");
    protected static long Mask = Ambient | Diffuse | Specular | Emissive | Reflection | AmbientLight | Fog;
    public final Color color = new Color();

    public static final boolean is(long mask) {
        return (mask & Mask) != 0L;
    }

    public static final ColorAttribute createAmbient(Color color) {
        return new ColorAttribute(Ambient, color);
    }

    public static final ColorAttribute createAmbient(float r, float g, float b, float a) {
        return new ColorAttribute(Ambient, r, g, b, a);
    }

    public static final ColorAttribute createDiffuse(Color color) {
        return new ColorAttribute(Diffuse, color);
    }

    public static final ColorAttribute createDiffuse(float r, float g, float b, float a) {
        return new ColorAttribute(Diffuse, r, g, b, a);
    }

    public static final ColorAttribute createSpecular(Color color) {
        return new ColorAttribute(Specular, color);
    }

    public static final ColorAttribute createSpecular(float r, float g, float b, float a) {
        return new ColorAttribute(Specular, r, g, b, a);
    }

    public static final ColorAttribute createReflection(Color color) {
        return new ColorAttribute(Reflection, color);
    }

    public static final ColorAttribute createReflection(float r, float g, float b, float a) {
        return new ColorAttribute(Reflection, r, g, b, a);
    }

    public ColorAttribute(long type) {
        super(type);
        if (!ColorAttribute.is(type)) {
            throw new GdxRuntimeException("Invalid type specified");
        }
    }

    public ColorAttribute(long type, Color color) {
        this(type);
        if (color != null) {
            this.color.set(color);
        }
    }

    public ColorAttribute(long type, float r, float g, float b, float a) {
        this(type);
        this.color.set(r, g, b, a);
    }

    public ColorAttribute(ColorAttribute copyFrom) {
        this(copyFrom.type, copyFrom.color);
    }

    @Override
    public Attribute copy() {
        return new ColorAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 953 * result + this.color.toIntBits();
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return (int)(this.type - o.type);
        }
        return ((ColorAttribute)o).color.toIntBits() - this.color.toIntBits();
    }
}

