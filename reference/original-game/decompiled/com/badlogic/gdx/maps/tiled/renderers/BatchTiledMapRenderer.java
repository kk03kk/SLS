/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled.renderers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

public abstract class BatchTiledMapRenderer
implements TiledMapRenderer,
Disposable {
    protected static final int NUM_VERTICES = 20;
    protected TiledMap map;
    protected float unitScale;
    protected Batch batch;
    protected Rectangle viewBounds;
    protected Rectangle imageBounds = new Rectangle();
    protected boolean ownsBatch;
    protected float[] vertices = new float[20];

    public TiledMap getMap() {
        return this.map;
    }

    public void setMap(TiledMap map) {
        this.map = map;
    }

    public float getUnitScale() {
        return this.unitScale;
    }

    public Batch getBatch() {
        return this.batch;
    }

    public Rectangle getViewBounds() {
        return this.viewBounds;
    }

    public BatchTiledMapRenderer(TiledMap map) {
        this(map, 1.0f);
    }

    public BatchTiledMapRenderer(TiledMap map, float unitScale) {
        this.map = map;
        this.unitScale = unitScale;
        this.viewBounds = new Rectangle();
        this.batch = new SpriteBatch();
        this.ownsBatch = true;
    }

    public BatchTiledMapRenderer(TiledMap map, Batch batch) {
        this(map, 1.0f, batch);
    }

    public BatchTiledMapRenderer(TiledMap map, float unitScale, Batch batch) {
        this.map = map;
        this.unitScale = unitScale;
        this.viewBounds = new Rectangle();
        this.batch = batch;
        this.ownsBatch = false;
    }

    @Override
    public void setView(OrthographicCamera camera) {
        this.batch.setProjectionMatrix(camera.combined);
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        this.viewBounds.set(camera.position.x - w / 2.0f, camera.position.y - h / 2.0f, w, h);
    }

    @Override
    public void setView(Matrix4 projection, float x, float y, float width, float height) {
        this.batch.setProjectionMatrix(projection);
        this.viewBounds.set(x, y, width, height);
    }

    @Override
    public void render() {
        this.beginRender();
        for (MapLayer layer : this.map.getLayers()) {
            if (!layer.isVisible()) continue;
            if (layer instanceof TiledMapTileLayer) {
                this.renderTileLayer((TiledMapTileLayer)layer);
                continue;
            }
            if (layer instanceof TiledMapImageLayer) {
                this.renderImageLayer((TiledMapImageLayer)layer);
                continue;
            }
            this.renderObjects(layer);
        }
        this.endRender();
    }

    @Override
    public void render(int[] layers) {
        this.beginRender();
        for (int layerIdx : layers) {
            MapLayer layer = this.map.getLayers().get(layerIdx);
            if (!layer.isVisible()) continue;
            if (layer instanceof TiledMapTileLayer) {
                this.renderTileLayer((TiledMapTileLayer)layer);
                continue;
            }
            if (layer instanceof TiledMapImageLayer) {
                this.renderImageLayer((TiledMapImageLayer)layer);
                continue;
            }
            this.renderObjects(layer);
        }
        this.endRender();
    }

    @Override
    public void renderObjects(MapLayer layer) {
        for (MapObject object : layer.getObjects()) {
            this.renderObject(object);
        }
    }

    @Override
    public void renderObject(MapObject object) {
    }

    @Override
    public void renderImageLayer(TiledMapImageLayer layer) {
        Color batchColor = this.batch.getColor();
        float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.getOpacity());
        float[] vertices = this.vertices;
        TextureRegion region = layer.getTextureRegion();
        if (region == null) {
            return;
        }
        float x = layer.getX();
        float y = layer.getY();
        float x1 = x * this.unitScale;
        float y1 = y * this.unitScale;
        float x2 = x1 + (float)region.getRegionWidth() * this.unitScale;
        float y2 = y1 + (float)region.getRegionHeight() * this.unitScale;
        this.imageBounds.set(x1, y1, x2 - x1, y2 - y1);
        if (this.viewBounds.contains(this.imageBounds) || this.viewBounds.overlaps(this.imageBounds)) {
            float u1 = region.getU();
            float v1 = region.getV2();
            float u2 = region.getU2();
            float v2 = region.getV();
            vertices[0] = x1;
            vertices[1] = y1;
            vertices[2] = color;
            vertices[3] = u1;
            vertices[4] = v1;
            vertices[5] = x1;
            vertices[6] = y2;
            vertices[7] = color;
            vertices[8] = u1;
            vertices[9] = v2;
            vertices[10] = x2;
            vertices[11] = y2;
            vertices[12] = color;
            vertices[13] = u2;
            vertices[14] = v2;
            vertices[15] = x2;
            vertices[16] = y1;
            vertices[17] = color;
            vertices[18] = u2;
            vertices[19] = v1;
            this.batch.draw(region.getTexture(), vertices, 0, 20);
        }
    }

    protected void beginRender() {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        this.batch.begin();
    }

    protected void endRender() {
        this.batch.end();
    }

    @Override
    public void dispose() {
        if (this.ownsBatch) {
            this.batch.dispose();
        }
    }
}

