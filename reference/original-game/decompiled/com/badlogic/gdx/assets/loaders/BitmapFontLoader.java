/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class BitmapFontLoader
extends AsynchronousAssetLoader<BitmapFont, BitmapFontParameter> {
    BitmapFont.BitmapFontData data;

    public BitmapFontLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, BitmapFontParameter parameter) {
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>();
        if (parameter != null && parameter.bitmapFontData != null) {
            this.data = parameter.bitmapFontData;
            return deps;
        }
        this.data = new BitmapFont.BitmapFontData(file, parameter != null ? parameter.flip : false);
        if (parameter != null && parameter.atlasName != null) {
            deps.add(new AssetDescriptor<TextureAtlas>(parameter.atlasName, TextureAtlas.class));
        } else {
            for (int i = 0; i < this.data.getImagePaths().length; ++i) {
                String path = this.data.getImagePath(i);
                FileHandle resolved = this.resolve(path);
                TextureLoader.TextureParameter textureParams = new TextureLoader.TextureParameter();
                if (parameter != null) {
                    textureParams.genMipMaps = parameter.genMipMaps;
                    textureParams.minFilter = parameter.minFilter;
                    textureParams.magFilter = parameter.magFilter;
                }
                AssetDescriptor<Texture> descriptor = new AssetDescriptor<Texture>(resolved, Texture.class, textureParams);
                deps.add(descriptor);
            }
        }
        return deps;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, BitmapFontParameter parameter) {
    }

    @Override
    public BitmapFont loadSync(AssetManager manager, String fileName, FileHandle file, BitmapFontParameter parameter) {
        if (parameter != null && parameter.atlasName != null) {
            String name;
            TextureAtlas atlas = manager.get(parameter.atlasName, TextureAtlas.class);
            TextureAtlas.AtlasRegion region = atlas.findRegion(name = file.sibling(this.data.imagePaths[0]).nameWithoutExtension().toString());
            if (region == null) {
                throw new GdxRuntimeException("Could not find font region " + name + " in atlas " + parameter.atlasName);
            }
            return new BitmapFont(file, region);
        }
        int n = this.data.getImagePaths().length;
        Array<TextureRegion> regs = new Array<TextureRegion>(n);
        for (int i = 0; i < n; ++i) {
            regs.add(new TextureRegion(manager.get(this.data.getImagePath(i), Texture.class)));
        }
        return new BitmapFont(this.data, regs, true);
    }

    public static class BitmapFontParameter
    extends AssetLoaderParameters<BitmapFont> {
        public boolean flip = false;
        public boolean genMipMaps = false;
        public Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;
        public BitmapFont.BitmapFontData bitmapFontData = null;
        public String atlasName = null;
    }
}

