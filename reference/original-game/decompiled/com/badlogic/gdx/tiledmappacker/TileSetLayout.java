/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tiledmappacker;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TileSetLayout {
    public final BufferedImage image;
    private final IntMap<Vector2> imageTilePositions;
    private int numRows;
    private int numCols;
    public final int numTiles;
    public final int firstgid;

    protected TileSetLayout(int firstgid, TiledMapTileSet tileset, FileHandle baseDir) throws IOException {
        int tileWidth = tileset.getProperties().get("tilewidth", Integer.class);
        int tileHeight = tileset.getProperties().get("tileheight", Integer.class);
        int margin = tileset.getProperties().get("margin", Integer.class);
        int spacing = tileset.getProperties().get("spacing", Integer.class);
        this.firstgid = firstgid;
        this.image = ImageIO.read(baseDir.child(tileset.getProperties().get("imagesource", String.class)).read());
        this.imageTilePositions = new IntMap();
        int tile = 0;
        this.numRows = 0;
        this.numCols = 0;
        int stopWidth = this.image.getWidth() - tileWidth;
        int stopHeight = this.image.getHeight() - tileHeight;
        for (int y = margin; y <= stopHeight; y += tileHeight + spacing) {
            for (int x = margin; x <= stopWidth; x += tileWidth + spacing) {
                if (y == margin) {
                    ++this.numCols;
                }
                this.imageTilePositions.put(tile, new Vector2(x, y));
                ++tile;
            }
            ++this.numRows;
        }
        this.numTiles = this.numRows * this.numCols;
    }

    public int getNumRows() {
        return this.numRows;
    }

    public int getNumCols() {
        return this.numCols;
    }

    public Vector2 getLocation(int tile) {
        return this.imageTilePositions.get(tile - this.firstgid);
    }
}

