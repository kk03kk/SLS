/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import java.util.Iterator;

public class MapLayers
implements Iterable<MapLayer> {
    private Array<MapLayer> layers = new Array();

    public MapLayer get(int index) {
        return this.layers.get(index);
    }

    public MapLayer get(String name) {
        int n = this.layers.size;
        for (int i = 0; i < n; ++i) {
            MapLayer layer = this.layers.get(i);
            if (!name.equals(layer.getName())) continue;
            return layer;
        }
        return null;
    }

    public int getIndex(String name) {
        return this.getIndex(this.get(name));
    }

    public int getIndex(MapLayer layer) {
        return this.layers.indexOf(layer, true);
    }

    public int getCount() {
        return this.layers.size;
    }

    public void add(MapLayer layer) {
        this.layers.add(layer);
    }

    public void remove(int index) {
        this.layers.removeIndex(index);
    }

    public void remove(MapLayer layer) {
        this.layers.removeValue(layer, true);
    }

    public <T extends MapLayer> Array<T> getByType(Class<T> type) {
        return this.getByType(type, new Array());
    }

    public <T extends MapLayer> Array<T> getByType(Class<T> type, Array<T> fill) {
        fill.clear();
        int n = this.layers.size;
        for (int i = 0; i < n; ++i) {
            MapLayer layer = this.layers.get(i);
            if (!ClassReflection.isInstance(type, layer)) continue;
            fill.add(layer);
        }
        return fill;
    }

    @Override
    public Iterator<MapLayer> iterator() {
        return this.layers.iterator();
    }
}

