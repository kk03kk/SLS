/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public interface TiledMapRenderer
extends MapRenderer {
    public void renderObjects(MapLayer var1);

    public void renderObject(MapObject var1);

    public void renderTileLayer(TiledMapTileLayer var1);

    public void renderImageLayer(TiledMapImageLayer var1);
}

