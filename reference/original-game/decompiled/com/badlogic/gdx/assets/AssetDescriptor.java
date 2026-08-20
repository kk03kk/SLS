/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.files.FileHandle;

public class AssetDescriptor<T> {
    public final String fileName;
    public final Class<T> type;
    public final AssetLoaderParameters params;
    public FileHandle file;

    public AssetDescriptor(String fileName, Class<T> assetType) {
        this(fileName, assetType, null);
    }

    public AssetDescriptor(FileHandle file, Class<T> assetType) {
        this(file, assetType, null);
    }

    public AssetDescriptor(String fileName, Class<T> assetType, AssetLoaderParameters<T> params) {
        this.fileName = fileName.replaceAll("\\\\", "/");
        this.type = assetType;
        this.params = params;
    }

    public AssetDescriptor(FileHandle file, Class<T> assetType, AssetLoaderParameters<T> params) {
        this.fileName = file.path().replaceAll("\\\\", "/");
        this.file = file;
        this.type = assetType;
        this.params = params;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.fileName);
        buffer.append(", ");
        buffer.append(this.type.getName());
        return buffer.toString();
    }
}

