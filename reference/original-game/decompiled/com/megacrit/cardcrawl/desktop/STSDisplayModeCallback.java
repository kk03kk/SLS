/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class STSDisplayModeCallback
extends LwjglApplicationConfiguration
implements LwjglGraphics.SetDisplayModeCallback {
    private static final Logger logger = LogManager.getLogger(STSDisplayModeCallback.class.getName());

    @Override
    public LwjglApplicationConfiguration onFailure(LwjglApplicationConfiguration initialConfig) {
        logger.error("Failure to display LwjglApplication. InitialConfig=" + initialConfig.toString());
        initialConfig.width = 1280;
        initialConfig.height = 720;
        initialConfig.fullscreen = false;
        return initialConfig;
    }
}

