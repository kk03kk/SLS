/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.NumberUtils;

public class TextureAttribute
extends Attribute {
    public static final String DiffuseAlias = "diffuseTexture";
    public static final long Diffuse = TextureAttribute.register("diffuseTexture");
    public static final String SpecularAlias = "specularTexture";
    public static final long Specular = TextureAttribute.register("specularTexture");
    public static final String BumpAlias = "bumpTexture";
    public static final long Bump = TextureAttribute.register("bumpTexture");
    public static final String NormalAlias = "normalTexture";
    public static final long Normal = TextureAttribute.register("normalTexture");
    public static final String AmbientAlias = "ambientTexture";
    public static final long Ambient = TextureAttribute.register("ambientTexture");
    public static final String EmissiveAlias = "emissiveTexture";
    public static final long Emissive = TextureAttribute.register("emissiveTexture");
    public static final String ReflectionAlias = "reflectionTexture";
    public static final long Reflection = TextureAttribute.register("reflectionTexture");
    protected static long Mask = Diffuse | Specular | Bump | Normal | Ambient | Emissive | Reflection;
    public final TextureDescriptor<Texture> textureDescription;
    public float offsetU = 0.0f;
    public float offsetV = 0.0f;
    public float scaleU = 1.0f;
    public float scaleV = 1.0f;
    public int uvIndex = 0;

    public static final boolean is(long mask) {
        return (mask & Mask) != 0L;
    }

    public static TextureAttribute createDiffuse(Texture texture) {
        return new TextureAttribute(Diffuse, texture);
    }

    public static TextureAttribute createDiffuse(TextureRegion region) {
        return new TextureAttribute(Diffuse, region);
    }

    public static TextureAttribute createSpecular(Texture texture) {
        return new TextureAttribute(Specular, texture);
    }

    public static TextureAttribute createSpecular(TextureRegion region) {
        return new TextureAttribute(Specular, region);
    }

    public static TextureAttribute createNormal(Texture texture) {
        return new TextureAttribute(Normal, texture);
    }

    public static TextureAttribute createNormal(TextureRegion region) {
        return new TextureAttribute(Normal, region);
    }

    public static TextureAttribute createBump(Texture texture) {
        return new TextureAttribute(Bump, texture);
    }

    public static TextureAttribute createBump(TextureRegion region) {
        return new TextureAttribute(Bump, region);
    }

    public static TextureAttribute createAmbient(Texture texture) {
        return new TextureAttribute(Ambient, texture);
    }

    public static TextureAttribute createAmbient(TextureRegion region) {
        return new TextureAttribute(Ambient, region);
    }

    public static TextureAttribute createEmissive(Texture texture) {
        return new TextureAttribute(Emissive, texture);
    }

    public static TextureAttribute createEmissive(TextureRegion region) {
        return new TextureAttribute(Emissive, region);
    }

    public static TextureAttribute createReflection(Texture texture) {
        return new TextureAttribute(Reflection, texture);
    }

    public static TextureAttribute createReflection(TextureRegion region) {
        return new TextureAttribute(Reflection, region);
    }

    public TextureAttribute(long type) {
        super(type);
        if (!TextureAttribute.is(type)) {
            throw new GdxRuntimeException("Invalid type specified");
        }
        this.textureDescription = new TextureDescriptor();
    }

    public <T extends Texture> TextureAttribute(long type, TextureDescriptor<T> textureDescription) {
        this(type);
        this.textureDescription.set(textureDescription);
    }

    public <T extends Texture> TextureAttribute(long type, TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV, int uvIndex) {
        this(type, textureDescription);
        this.offsetU = offsetU;
        this.offsetV = offsetV;
        this.scaleU = scaleU;
        this.scaleV = scaleV;
        this.uvIndex = uvIndex;
    }

    public <T extends Texture> TextureAttribute(long type, TextureDescriptor<T> textureDescription, float offsetU, float offsetV, float scaleU, float scaleV) {
        this(type, textureDescription, offsetU, offsetV, scaleU, scaleV, 0);
    }

    public TextureAttribute(long type, Texture texture) {
        this(type);
        this.textureDescription.texture = texture;
    }

    public TextureAttribute(long type, TextureRegion region) {
        this(type);
        this.set(region);
    }

    public TextureAttribute(TextureAttribute copyFrom) {
        this(copyFrom.type, copyFrom.textureDescription, copyFrom.offsetU, copyFrom.offsetV, copyFrom.scaleU, copyFrom.scaleV, copyFrom.uvIndex);
    }

    public void set(TextureRegion region) {
        this.textureDescription.texture = region.getTexture();
        this.offsetU = region.getU();
        this.offsetV = region.getV();
        this.scaleU = region.getU2() - this.offsetU;
        this.scaleV = region.getV2() - this.offsetV;
    }

    @Override
    public Attribute copy() {
        return new TextureAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 991 * result + this.textureDescription.hashCode();
        result = 991 * result + NumberUtils.floatToRawIntBits(this.offsetU);
        result = 991 * result + NumberUtils.floatToRawIntBits(this.offsetV);
        result = 991 * result + NumberUtils.floatToRawIntBits(this.scaleU);
        result = 991 * result + NumberUtils.floatToRawIntBits(this.scaleV);
        result = 991 * result + this.uvIndex;
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return this.type < o.type ? -1 : 1;
        }
        TextureAttribute other = (TextureAttribute)o;
        int c = this.textureDescription.compareTo(other.textureDescription);
        if (c != 0) {
            return c;
        }
        if (this.uvIndex != other.uvIndex) {
            return this.uvIndex - other.uvIndex;
        }
        if (!MathUtils.isEqual(this.scaleU, other.scaleU)) {
            return this.scaleU > other.scaleU ? 1 : -1;
        }
        if (!MathUtils.isEqual(this.scaleV, other.scaleV)) {
            return this.scaleV > other.scaleV ? 1 : -1;
        }
        if (!MathUtils.isEqual(this.offsetU, other.offsetU)) {
            return this.offsetU > other.offsetU ? 1 : -1;
        }
        if (!MathUtils.isEqual(this.offsetV, other.offsetV)) {
            return this.offsetV > other.offsetV ? 1 : -1;
        }
        return 0;
    }
}

