/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;

public class FreetypeFontLoader
extends AsynchronousAssetLoader<BitmapFont, FreeTypeFontLoaderParameter> {
    public FreetypeFontLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
        if (parameter == null) {
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
        }
    }

    @Override
    public BitmapFont loadSync(AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
        if (parameter == null) {
            throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
        }
        FreeTypeFontGenerator generator = manager.get(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class);
        BitmapFont font = generator.generateFont(parameter.fontParameters);
        return font;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>();
        deps.add(new AssetDescriptor<FreeTypeFontGenerator>(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class));
        return deps;
    }

    public static class FreeTypeFontLoaderParameter
    extends AssetLoaderParameters<BitmapFont> {
        public String fontFileName;
        public FreeTypeFontGenerator.FreeTypeFontParameter fontParameters = new FreeTypeFontGenerator.FreeTypeFontParameter();
    }
}

