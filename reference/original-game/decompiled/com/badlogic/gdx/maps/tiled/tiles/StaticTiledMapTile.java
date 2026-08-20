/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled.tiles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;

public class StaticTiledMapTile
implements TiledMapTile {
    private int id;
    private TiledMapTile.BlendMode blendMode = TiledMapTile.BlendMode.ALPHA;
    private MapProperties properties;
    private TextureRegion textureRegion;
    private float offsetX;
    private float offsetY;

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public TiledMapTile.BlendMode getBlendMode() {
        return this.blendMode;
    }

    @Override
    public void setBlendMode(TiledMapTile.BlendMode blendMode) {
        this.blendMode = blendMode;
    }

    @Override
    public MapProperties getProperties() {
        if (this.properties == null) {
            this.properties = new MapProperties();
        }
        return this.properties;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return this.textureRegion;
    }

    @Override
    public void setTextureRegion(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    @Override
    public float getOffsetX() {
        return this.offsetX;
    }

    @Override
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    @Override
    public float getOffsetY() {
        return this.offsetY;
    }

    @Override
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public StaticTiledMapTile(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    public StaticTiledMapTile(StaticTiledMapTile copy) {
        if (copy.properties != null) {
            this.getProperties().putAll(copy.properties);
        }
        this.textureRegion = copy.textureRegion;
        this.id = copy.id;
    }
}

