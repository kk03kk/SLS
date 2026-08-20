/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;

public class ParticleEffectLoader
extends SynchronousAssetLoader<ParticleEffect, ParticleEffectParameter> {
    public ParticleEffectLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public ParticleEffect load(AssetManager am, String fileName, FileHandle file, ParticleEffectParameter param) {
        ParticleEffect effect = new ParticleEffect();
        if (param != null && param.atlasFile != null) {
            effect.load(file, am.get(param.atlasFile, TextureAtlas.class), param.atlasPrefix);
        } else if (param != null && param.imagesDir != null) {
            effect.load(file, param.imagesDir);
        } else {
            effect.load(file, file.parent());
        }
        return effect;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, ParticleEffectParameter param) {
        Array<AssetDescriptor<TextureAtlas>> deps = null;
        if (param != null && param.atlasFile != null) {
            deps = new Array<AssetDescriptor<TextureAtlas>>();
            deps.add(new AssetDescriptor<TextureAtlas>(param.atlasFile, TextureAtlas.class));
        }
        return deps;
    }

    public static class ParticleEffectParameter
    extends AssetLoaderParameters<ParticleEffect> {
        public String atlasFile;
        public String atlasPrefix;
        public FileHandle imagesDir;
    }
}

