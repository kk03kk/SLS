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
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.CubemapData;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.utils.Array;

public class CubemapLoader
extends AsynchronousAssetLoader<Cubemap, CubemapParameter> {
    CubemapLoaderInfo info = new CubemapLoaderInfo();

    public CubemapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, CubemapParameter parameter) {
        this.info.filename = fileName;
        if (parameter == null || parameter.cubemapData == null) {
            Object pixmap = null;
            Pixmap.Format format = null;
            boolean genMipMaps = false;
            this.info.cubemap = null;
            if (parameter != null) {
                format = parameter.format;
                this.info.cubemap = parameter.cubemap;
            }
            if (fileName.contains(".ktx") || fileName.contains(".zktx")) {
                this.info.data = new KTXTextureData(file, genMipMaps);
            }
        } else {
            this.info.data = parameter.cubemapData;
            this.info.cubemap = parameter.cubemap;
        }
        if (!this.info.data.isPrepared()) {
            this.info.data.prepare();
        }
    }

    @Override
    public Cubemap loadSync(AssetManager manager, String fileName, FileHandle file, CubemapParameter parameter) {
        if (this.info == null) {
            return null;
        }
        Cubemap cubemap = this.info.cubemap;
        if (cubemap != null) {
            cubemap.load(this.info.data);
        } else {
            cubemap = new Cubemap(this.info.data);
        }
        if (parameter != null) {
            cubemap.setFilter(parameter.minFilter, parameter.magFilter);
            cubemap.setWrap(parameter.wrapU, parameter.wrapV);
        }
        return cubemap;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, CubemapParameter parameter) {
        return null;
    }

    public static class CubemapParameter
    extends AssetLoaderParameters<Cubemap> {
        public Pixmap.Format format = null;
        public Cubemap cubemap = null;
        public CubemapData cubemapData = null;
        public Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;
        public Texture.TextureWrap wrapU = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapV = Texture.TextureWrap.ClampToEdge;
    }

    public static class CubemapLoaderInfo {
        String filename;
        CubemapData data;
        Cubemap cubemap;
    }
}

