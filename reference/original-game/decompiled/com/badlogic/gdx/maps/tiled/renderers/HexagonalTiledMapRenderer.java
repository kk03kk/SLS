/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled.renderers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;

public class HexagonalTiledMapRenderer
extends BatchTiledMapRenderer {
    private boolean staggerAxisX = true;
    private boolean staggerIndexEven = false;
    private float hexSideLength = 0.0f;

    public HexagonalTiledMapRenderer(TiledMap map) {
        super(map);
        this.init(map);
    }

    public HexagonalTiledMapRenderer(TiledMap map, float unitScale) {
        super(map, unitScale);
        this.init(map);
    }

    public HexagonalTiledMapRenderer(TiledMap map, Batch batch) {
        super(map, batch);
        this.init(map);
    }

    public HexagonalTiledMapRenderer(TiledMap map, float unitScale, Batch batch) {
        super(map, unitScale, batch);
        this.init(map);
    }

    private void init(TiledMap map) {
        Integer length;
        String index;
        String axis = map.getProperties().get("staggeraxis", String.class);
        if (axis != null) {
            this.staggerAxisX = axis.equals("x");
        }
        if ((index = map.getProperties().get("staggerindex", String.class)) != null) {
            this.staggerIndexEven = index.equals("even");
        }
        if ((length = map.getProperties().get("hexsidelength", Integer.class)) != null) {
            this.hexSideLength = length.intValue();
        } else if (this.staggerAxisX) {
            length = map.getProperties().get("tilewidth", Integer.class);
            if (length != null) {
                this.hexSideLength = 0.5f * (float)length.intValue();
            } else {
                TiledMapTileLayer tmtl = (TiledMapTileLayer)map.getLayers().get(0);
                this.hexSideLength = 0.5f * tmtl.getTileWidth();
            }
        } else {
            length = map.getProperties().get("tileheight", Integer.class);
            if (length != null) {
                this.hexSideLength = 0.5f * (float)length.intValue();
            } else {
                TiledMapTileLayer tmtl = (TiledMapTileLayer)map.getLayers().get(0);
                this.hexSideLength = 0.5f * tmtl.getTileHeight();
            }
        }
    }

    @Override
    public void renderTileLayer(TiledMapTileLayer layer) {
        Color batchColor = this.batch.getColor();
        float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity());
        int layerWidth = layer.getWidth();
        int layerHeight = layer.getHeight();
        float layerTileWidth = layer.getTileWidth() * this.unitScale;
        float layerTileHeight = layer.getTileHeight() * this.unitScale;
        float layerHexLength = this.hexSideLength * this.unitScale;
        if (this.staggerAxisX) {
            float tileWidthLowerCorner = (layerTileWidth - layerHexLength) / 2.0f;
            float tileWidthUpperCorner = (layerTileWidth + layerHexLength) / 2.0f;
            float layerTileHeight50 = layerTileHeight * 0.5f;
            int row1 = Math.max(0, (int)((this.viewBounds.y - layerTileHeight50) / layerTileHeight));
            int row2 = Math.min(layerHeight, (int)((this.viewBounds.y + this.viewBounds.height + layerTileHeight) / layerTileHeight));
            int col1 = Math.max(0, (int)((this.viewBounds.x - tileWidthLowerCorner) / tileWidthUpperCorner));
            int col2 = Math.min(layerWidth, (int)((this.viewBounds.x + this.viewBounds.width + tileWidthUpperCorner) / tileWidthUpperCorner));
            int colA = this.staggerIndexEven == (col1 % 2 == 0) ? col1 + 1 : col1;
            int colB = this.staggerIndexEven == (col1 % 2 == 0) ? col1 : col1 + 1;
            for (int row = row2 - 1; row >= row1; --row) {
                int col;
                for (col = colA; col < col2; col += 2) {
                    this.renderCell(layer.getCell(col, row), tileWidthUpperCorner * (float)col, layerTileHeight50 + layerTileHeight * (float)row, color);
                }
                for (col = colB; col < col2; col += 2) {
                    this.renderCell(layer.getCell(col, row), tileWidthUpperCorner * (float)col, layerTileHeight * (float)row, color);
                }
            }
        } else {
            float tileHeightLowerCorner = (layerTileHeight - layerHexLength) / 2.0f;
            float tileHeightUpperCorner = (layerTileHeight + layerHexLength) / 2.0f;
            float layerTileWidth50 = layerTileWidth * 0.5f;
            int row1 = Math.max(0, (int)((this.viewBounds.y - tileHeightLowerCorner) / tileHeightUpperCorner));
            int row2 = Math.min(layerHeight, (int)((this.viewBounds.y + this.viewBounds.height + tileHeightUpperCorner) / tileHeightUpperCorner));
            int col1 = Math.max(0, (int)((this.viewBounds.x - layerTileWidth50) / layerTileWidth));
            int col2 = Math.min(layerWidth, (int)((this.viewBounds.x + this.viewBounds.width + layerTileWidth) / layerTileWidth));
            float shiftX = 0.0f;
            for (int row = row2 - 1; row >= row1; --row) {
                shiftX = row % 2 == 0 == this.staggerIndexEven ? 0.0f : layerTileWidth50;
                for (int col = col1; col < col2; ++col) {
                    this.renderCell(layer.getCell(col, row), layerTileWidth * (float)col + shiftX, tileHeightUpperCorner * (float)row, color);
                }
            }
        }
    }

    private void renderCell(TiledMapTileLayer.Cell cell, float x, float y, float color) {
        TiledMapTile tile;
        if (cell != null && (tile = cell.getTile()) != null) {
            float temp;
            if (tile instanceof AnimatedTiledMapTile) {
                return;
            }
            boolean flipX = cell.getFlipHorizontally();
            boolean flipY = cell.getFlipVertically();
            int rotations = cell.getRotation();
            TextureRegion region = tile.getTextureRegion();
            float x1 = x + tile.getOffsetX() * this.unitScale;
            float y1 = y + tile.getOffsetY() * this.unitScale;
            float x2 = x1 + (float)region.getRegionWidth() * this.unitScale;
            float y2 = y1 + (float)region.getRegionHeight() * this.unitScale;
            float u1 = region.getU();
            float v1 = region.getV2();
            float u2 = region.getU2();
            float v2 = region.getV();
            this.vertices[0] = x1;
            this.vertices[1] = y1;
            this.vertices[2] = color;
            this.vertices[3] = u1;
            this.vertices[4] = v1;
            this.vertices[5] = x1;
            this.vertices[6] = y2;
            this.vertices[7] = color;
            this.vertices[8] = u1;
            this.vertices[9] = v2;
            this.vertices[10] = x2;
            this.vertices[11] = y2;
            this.vertices[12] = color;
            this.vertices[13] = u2;
            this.vertices[14] = v2;
            this.vertices[15] = x2;
            this.vertices[16] = y1;
            this.vertices[17] = color;
            this.vertices[18] = u2;
            this.vertices[19] = v1;
            if (flipX) {
                temp = this.vertices[3];
                this.vertices[3] = this.vertices[13];
                this.vertices[13] = temp;
                temp = this.vertices[8];
                this.vertices[8] = this.vertices[18];
                this.vertices[18] = temp;
            }
            if (flipY) {
                temp = this.vertices[4];
                this.vertices[4] = this.vertices[14];
                this.vertices[14] = temp;
                temp = this.vertices[9];
                this.vertices[9] = this.vertices[19];
                this.vertices[19] = temp;
            }
            if (rotations == 2) {
                float tempU = this.vertices[3];
                this.vertices[3] = this.vertices[13];
                this.vertices[13] = tempU;
                tempU = this.vertices[8];
                this.vertices[8] = this.vertices[18];
                this.vertices[18] = tempU;
                float tempV = this.vertices[4];
                this.vertices[4] = this.vertices[14];
                this.vertices[14] = tempV;
                tempV = this.vertices[9];
                this.vertices[9] = this.vertices[19];
                this.vertices[19] = tempV;
            }
            this.batch.draw(region.getTexture(), this.vertices, 0, 20);
        }
    }
}

