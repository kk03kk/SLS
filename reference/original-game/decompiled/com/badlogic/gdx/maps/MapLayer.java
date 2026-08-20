/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps;

import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;

public class MapLayer {
    private String name = "";
    private float opacity = 1.0f;
    private boolean visible = true;
    private MapObjects objects = new MapObjects();
    private MapProperties properties = new MapProperties();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getOpacity() {
        return this.opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public MapObjects getObjects() {
        return this.objects;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public MapProperties getProperties() {
        return this.properties;
    }
}

