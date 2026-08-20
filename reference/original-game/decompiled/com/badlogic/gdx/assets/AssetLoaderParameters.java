/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets;

import com.badlogic.gdx.assets.AssetManager;

public class AssetLoaderParameters<T> {
    public LoadedCallback loadedCallback;

    public static interface LoadedCallback {
        public void finishedLoading(AssetManager var1, String var2, Class var3);
    }
}

