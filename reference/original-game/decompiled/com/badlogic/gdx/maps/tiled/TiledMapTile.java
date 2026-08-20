/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;

public interface TiledMapTile {
    public int getId();

    public void setId(int var1);

    public BlendMode getBlendMode();

    public void setBlendMode(BlendMode var1);

    public TextureRegion getTextureRegion();

    public void setTextureRegion(TextureRegion var1);

    public float getOffsetX();

    public void setOffsetX(float var1);

    public float getOffsetY();

    public void setOffsetY(float var1);

    public MapProperties getProperties();

    public static enum BlendMode {
        NONE,
        ALPHA;

    }
}

