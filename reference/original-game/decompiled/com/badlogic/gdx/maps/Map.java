/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps;

import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.Disposable;

public class Map
implements Disposable {
    private MapLayers layers = new MapLayers();
    private MapProperties properties = new MapProperties();

    public MapLayers getLayers() {
        return this.layers;
    }

    public MapProperties getProperties() {
        return this.properties;
    }

    @Override
    public void dispose() {
    }
}

