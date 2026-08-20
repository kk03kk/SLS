/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;

public class PixmapLoader
extends AsynchronousAssetLoader<Pixmap, PixmapParameter> {
    Pixmap pixmap;

    public PixmapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
        this.pixmap = null;
        this.pixmap = new Pixmap(file);
    }

    @Override
    public Pixmap loadSync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
        Pixmap pixmap = this.pixmap;
        this.pixmap = null;
        return pixmap;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, PixmapParameter parameter) {
        return null;
    }

    public static class PixmapParameter
    extends AssetLoaderParameters<Pixmap> {
    }
}

