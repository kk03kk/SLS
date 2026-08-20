/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.viewport;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

public class FillViewport
extends ScalingViewport {
    public FillViewport(float worldWidth, float worldHeight) {
        super(Scaling.fill, worldWidth, worldHeight);
    }

    public FillViewport(float worldWidth, float worldHeight, Camera camera) {
        super(Scaling.fill, worldWidth, worldHeight, camera);
    }
}

