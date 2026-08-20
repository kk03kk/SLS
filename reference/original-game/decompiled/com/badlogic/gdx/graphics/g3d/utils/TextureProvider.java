/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public interface TextureProvider {
    public Texture load(String var1);

    public static class AssetTextureProvider
    implements TextureProvider {
        public final AssetManager assetManager;

        public AssetTextureProvider(AssetManager assetManager) {
            this.assetManager = assetManager;
        }

        @Override
        public Texture load(String fileName) {
            return this.assetManager.get(fileName, Texture.class);
        }
    }

    public static class FileTextureProvider
    implements TextureProvider {
        private Texture.TextureFilter minFilter;
        private Texture.TextureFilter magFilter;
        private Texture.TextureWrap uWrap;
        private Texture.TextureWrap vWrap;
        private boolean useMipMaps;

        public FileTextureProvider() {
            this.minFilter = this.magFilter = Texture.TextureFilter.Linear;
            this.uWrap = this.vWrap = Texture.TextureWrap.Repeat;
            this.useMipMaps = false;
        }

        public FileTextureProvider(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, Texture.TextureWrap uWrap, Texture.TextureWrap vWrap, boolean useMipMaps) {
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.uWrap = uWrap;
            this.vWrap = vWrap;
            this.useMipMaps = useMipMaps;
        }

        @Override
        public Texture load(String fileName) {
            Texture result = new Texture(Gdx.files.internal(fileName), this.useMipMaps);
            result.setFilter(this.minFilter, this.magFilter);
            result.setWrap(this.uWrap, this.vWrap);
            return result;
        }
    }
}

