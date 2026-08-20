/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import java.util.Iterator;

public class TiledMapTileSets
implements Iterable<TiledMapTileSet> {
    private Array<TiledMapTileSet> tilesets = new Array();

    public TiledMapTileSet getTileSet(int index) {
        return this.tilesets.get(index);
    }

    public TiledMapTileSet getTileSet(String name) {
        for (TiledMapTileSet tileset : this.tilesets) {
            if (!name.equals(tileset.getName())) continue;
            return tileset;
        }
        return null;
    }

    public void addTileSet(TiledMapTileSet tileset) {
        this.tilesets.add(tileset);
    }

    public void removeTileSet(int index) {
        this.tilesets.removeIndex(index);
    }

    public void removeTileSet(TiledMapTileSet tileset) {
        this.tilesets.removeValue(tileset, true);
    }

    public TiledMapTile getTile(int id) {
        for (int i = this.tilesets.size - 1; i >= 0; --i) {
            TiledMapTileSet tileset = this.tilesets.get(i);
            TiledMapTile tile = tileset.getTile(id);
            if (tile == null) continue;
            return tile;
        }
        return null;
    }

    @Override
    public Iterator<TiledMapTileSet> iterator() {
        return this.tilesets.iterator();
    }
}

