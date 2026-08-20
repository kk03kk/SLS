/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tiledmappacker;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.AtlasTmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.io.File;

public class TiledMapPackerTestRender
extends ApplicationAdapter {
    private final boolean DELETE_DELETEME_FOLDER_ON_EXIT = false;
    private static final String MAP_PATH = "../../tests/gdx-tests-android/assets/data/maps/tiled-atlas-processed/deleteMe/";
    private final String MAP_NAME = "test.tmx";
    private final String TMX_LOC = "../../tests/gdx-tests-android/assets/data/maps/tiled-atlas-processed/deleteMe/test.tmx";
    private final boolean CENTER_CAM = true;
    private final float WORLD_WIDTH = 32.0f;
    private final float WORLD_HEIGHT = 18.0f;
    private final float PIXELS_PER_METER = 32.0f;
    private final float UNIT_SCALE = 0.03125f;
    private AtlasTmxMapLoader.AtlasTiledMapLoaderParameters params;
    private AtlasTmxMapLoader atlasTmxMapLoader;
    private TiledMap map;
    private Viewport viewport;
    private OrthogonalTiledMapRenderer mapRenderer;
    private OrthographicCamera cam;

    @Override
    public void create() {
        this.atlasTmxMapLoader = new AtlasTmxMapLoader(new InternalFileHandleResolver());
        this.params = new AtlasTmxMapLoader.AtlasTiledMapLoaderParameters();
        this.params.generateMipMaps = false;
        this.params.convertObjectToTileSpace = false;
        this.params.flipY = true;
        this.viewport = new FitViewport(32.0f, 18.0f);
        this.cam = (OrthographicCamera)this.viewport.getCamera();
        this.map = this.atlasTmxMapLoader.load("../../tests/gdx-tests-android/assets/data/maps/tiled-atlas-processed/deleteMe/test.tmx", this.params);
        this.mapRenderer = new OrthogonalTiledMapRenderer(this.map, 0.03125f);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        Gdx.gl.glClear(16384);
        this.viewport.apply();
        this.mapRenderer.setView(this.cam);
        this.mapRenderer.render();
        if (Gdx.input.isKeyPressed(131)) {
            this.dispose();
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        this.viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        this.map.dispose();
    }

    public static void main(String[] args) throws Exception {
        File file = new File(MAP_PATH);
        if (!file.exists()) {
            System.out.println("Please run TiledMapPackerTest.");
            return;
        }
        new LwjglApplication(new TiledMapPackerTestRender(), "", 640, 480);
    }
}

