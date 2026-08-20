/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.viewport;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScalingViewport
extends Viewport {
    private Scaling scaling;

    public ScalingViewport(Scaling scaling, float worldWidth, float worldHeight) {
        this(scaling, worldWidth, worldHeight, new OrthographicCamera());
    }

    public ScalingViewport(Scaling scaling, float worldWidth, float worldHeight, Camera camera) {
        this.scaling = scaling;
        this.setWorldSize(worldWidth, worldHeight);
        this.setCamera(camera);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        Vector2 scaled = this.scaling.apply(this.getWorldWidth(), this.getWorldHeight(), screenWidth, screenHeight);
        int viewportWidth = Math.round(scaled.x);
        int viewportHeight = Math.round(scaled.y);
        this.setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);
        this.apply(centerCamera);
    }

    public Scaling getScaling() {
        return this.scaling;
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }
}

