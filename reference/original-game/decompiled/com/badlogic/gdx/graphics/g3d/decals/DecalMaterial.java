/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.decals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class DecalMaterial {
    public static final int NO_BLEND = -1;
    protected TextureRegion textureRegion;
    protected int srcBlendFactor;
    protected int dstBlendFactor;

    public void set() {
        this.textureRegion.getTexture().bind(0);
        if (!this.isOpaque()) {
            Gdx.gl.glBlendFunc(this.srcBlendFactor, this.dstBlendFactor);
        }
    }

    public boolean isOpaque() {
        return this.srcBlendFactor == -1;
    }

    public int getSrcBlendFactor() {
        return this.srcBlendFactor;
    }

    public int getDstBlendFactor() {
        return this.dstBlendFactor;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        DecalMaterial material = (DecalMaterial)o;
        return this.dstBlendFactor == material.dstBlendFactor && this.srcBlendFactor == material.srcBlendFactor && this.textureRegion.getTexture() == material.textureRegion.getTexture();
    }

    public int hashCode() {
        int result = this.textureRegion.getTexture() != null ? this.textureRegion.getTexture().hashCode() : 0;
        result = 31 * result + this.srcBlendFactor;
        result = 31 * result + this.dstBlendFactor;
        return result;
    }
}

