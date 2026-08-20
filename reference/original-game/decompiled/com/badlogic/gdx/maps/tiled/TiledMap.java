/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class TiledMap
extends Map {
    private TiledMapTileSets tilesets = new TiledMapTileSets();
    private Array<? extends Disposable> ownedResources;

    public TiledMapTileSets getTileSets() {
        return this.tilesets;
    }

    public void setOwnedResources(Array<? extends Disposable> resources) {
        this.ownedResources = resources;
    }

    @Override
    public void dispose() {
        if (this.ownedResources != null) {
            for (Disposable disposable : this.ownedResources) {
                disposable.dispose();
            }
        }
    }
}

