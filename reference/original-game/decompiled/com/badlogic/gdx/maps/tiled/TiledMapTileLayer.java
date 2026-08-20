/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;

public class TiledMapTileLayer
extends MapLayer {
    private int width;
    private int height;
    private float tileWidth;
    private float tileHeight;
    private Cell[][] cells;

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public float getTileWidth() {
        return this.tileWidth;
    }

    public float getTileHeight() {
        return this.tileHeight;
    }

    public TiledMapTileLayer(int width, int height, int tileWidth, int tileHeight) {
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.cells = new Cell[width][height];
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= this.width) {
            return null;
        }
        if (y < 0 || y >= this.height) {
            return null;
        }
        return this.cells[x][y];
    }

    public void setCell(int x, int y, Cell cell) {
        if (x < 0 || x >= this.width) {
            return;
        }
        if (y < 0 || y >= this.height) {
            return;
        }
        this.cells[x][y] = cell;
    }

    public static class Cell {
        private TiledMapTile tile;
        private boolean flipHorizontally;
        private boolean flipVertically;
        private int rotation;
        public static final int ROTATE_0 = 0;
        public static final int ROTATE_90 = 1;
        public static final int ROTATE_180 = 2;
        public static final int ROTATE_270 = 3;

        public TiledMapTile getTile() {
            return this.tile;
        }

        public Cell setTile(TiledMapTile tile) {
            this.tile = tile;
            return this;
        }

        public boolean getFlipHorizontally() {
            return this.flipHorizontally;
        }

        public Cell setFlipHorizontally(boolean flipHorizontally) {
            this.flipHorizontally = flipHorizontally;
            return this;
        }

        public boolean getFlipVertically() {
            return this.flipVertically;
        }

        public Cell setFlipVertically(boolean flipVertically) {
            this.flipVertically = flipVertically;
            return this;
        }

        public int getRotation() {
            return this.rotation;
        }

        public Cell setRotation(int rotation) {
            this.rotation = rotation;
            return this;
        }
    }
}

