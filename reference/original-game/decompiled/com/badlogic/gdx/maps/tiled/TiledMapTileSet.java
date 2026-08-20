/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.utils.IntMap;
import java.util.Iterator;

public class TiledMapTileSet
implements Iterable<TiledMapTile> {
    private String name;
    private IntMap<TiledMapTile> tiles = new IntMap();
    private MapProperties properties = new MapProperties();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MapProperties getProperties() {
        return this.properties;
    }

    public TiledMapTile getTile(int id) {
        return this.tiles.get(id);
    }

    @Override
    public Iterator<TiledMapTile> iterator() {
        return this.tiles.values().iterator();
    }

    public void putTile(int id, TiledMapTile tile) {
        this.tiles.put(id, tile);
    }

    public void removeTile(int id) {
        this.tiles.remove(id);
    }

    public int size() {
        return this.tiles.size;
    }
}

