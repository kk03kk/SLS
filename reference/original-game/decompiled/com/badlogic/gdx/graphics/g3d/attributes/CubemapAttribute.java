/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.attributes;

import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class CubemapAttribute
extends Attribute {
    public static final String EnvironmentMapAlias = "environmentCubemap";
    public static final long EnvironmentMap;
    protected static long Mask;
    public final TextureDescriptor<Cubemap> textureDescription;

    public static final boolean is(long mask) {
        return (mask & Mask) != 0L;
    }

    public CubemapAttribute(long type) {
        super(type);
        if (!CubemapAttribute.is(type)) {
            throw new GdxRuntimeException("Invalid type specified");
        }
        this.textureDescription = new TextureDescriptor();
    }

    public <T extends Cubemap> CubemapAttribute(long type, TextureDescriptor<T> textureDescription) {
        this(type);
        this.textureDescription.set(textureDescription);
    }

    public CubemapAttribute(long type, Cubemap texture) {
        this(type);
        this.textureDescription.texture = texture;
    }

    public CubemapAttribute(CubemapAttribute copyFrom) {
        this(copyFrom.type, copyFrom.textureDescription);
    }

    @Override
    public Attribute copy() {
        return new CubemapAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 967 * result + this.textureDescription.hashCode();
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (this.type != o.type) {
            return (int)(this.type - o.type);
        }
        return this.textureDescription.compareTo(((CubemapAttribute)o).textureDescription);
    }

    static {
        Mask = EnvironmentMap = CubemapAttribute.register(EnvironmentMapAlias);
    }
}

