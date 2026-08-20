/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;

public class FreeTypeFontGeneratorLoader
extends SynchronousAssetLoader<FreeTypeFontGenerator, FreeTypeFontGeneratorParameters> {
    public FreeTypeFontGeneratorLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public FreeTypeFontGenerator load(AssetManager assetManager, String fileName, FileHandle file, FreeTypeFontGeneratorParameters parameter) {
        FreeTypeFontGenerator generator = null;
        generator = file.extension().equals("gen") ? new FreeTypeFontGenerator(file.sibling(file.nameWithoutExtension())) : new FreeTypeFontGenerator(file);
        return generator;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FreeTypeFontGeneratorParameters parameter) {
        return null;
    }

    public static class FreeTypeFontGeneratorParameters
    extends AssetLoaderParameters<FreeTypeFontGenerator> {
    }
}

