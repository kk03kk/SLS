/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

public abstract class AssetLoader<T, P extends AssetLoaderParameters<T>> {
    private FileHandleResolver resolver;

    public AssetLoader(FileHandleResolver resolver) {
        this.resolver = resolver;
    }

    public FileHandle resolve(String fileName) {
        return this.resolver.resolve(fileName);
    }

    public abstract Array<AssetDescriptor> getDependencies(String var1, FileHandle var2, P var3);
}

