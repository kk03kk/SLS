/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.viewport;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

public class FitViewport
extends ScalingViewport {
    public FitViewport(float worldWidth, float worldHeight) {
        super(Scaling.fit, worldWidth, worldHeight);
    }

    public FitViewport(float worldWidth, float worldHeight, Camera camera) {
        super(Scaling.fit, worldWidth, worldHeight, camera);
    }
}

