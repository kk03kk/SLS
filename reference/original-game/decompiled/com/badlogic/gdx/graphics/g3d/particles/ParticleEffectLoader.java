/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import java.io.IOException;

public class ParticleEffectLoader
extends AsynchronousAssetLoader<ParticleEffect, ParticleEffectLoadParameter> {
    protected Array<ObjectMap.Entry<String, ResourceData<ParticleEffect>>> items = new Array();

    public ParticleEffectLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, ParticleEffectLoadParameter parameter) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, ParticleEffectLoadParameter parameter) {
        Json json = new Json();
        ResourceData data = json.fromJson(ResourceData.class, file);
        Array<ResourceData.AssetData> assets = null;
        Array<ObjectMap.Entry<String, ResourceData<ParticleEffect>>> array = this.items;
        synchronized (array) {
            ObjectMap.Entry entry = new ObjectMap.Entry();
            entry.key = fileName;
            entry.value = data;
            this.items.add(entry);
            assets = data.getAssets();
        }
        Array<AssetDescriptor> descriptors = new Array<AssetDescriptor>();
        for (ResourceData.AssetData assetData : assets) {
            if (!this.resolve(assetData.filename).exists()) {
                assetData.filename = file.parent().child(Gdx.files.internal(assetData.filename).name()).path();
            }
            if (assetData.type == ParticleEffect.class) {
                descriptors.add(new AssetDescriptor<ParticleEffect>(assetData.filename, assetData.type, parameter));
                continue;
            }
            descriptors.add(new AssetDescriptor(assetData.filename, assetData.type));
        }
        return descriptors;
    }

    public void save(ParticleEffect effect, ParticleEffectSaveParameter parameter) throws IOException {
        ResourceData<ParticleEffect> data = new ResourceData<ParticleEffect>(effect);
        effect.save(parameter.manager, (ResourceData)data);
        if (parameter.batches != null) {
            for (ParticleBatch<?> batch : parameter.batches) {
                boolean save = false;
                for (ParticleController controller : effect.getControllers()) {
                    if (!controller.renderer.isCompatible(batch)) continue;
                    save = true;
                    break;
                }
                if (!save) continue;
                batch.save(parameter.manager, (ResourceData)data);
            }
        }
        Json json = new Json();
        json.toJson(data, parameter.file);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    @Override
    public ParticleEffect loadSync(AssetManager manager, String fileName, FileHandle file, ParticleEffectLoadParameter parameter) {
        ResourceData effectData = null;
        Array<ObjectMap.Entry<String, ResourceData<ParticleEffect>>> array = this.items;
        synchronized (array) {
            void var7_8;
            boolean bl = false;
            while (var7_8 < this.items.size) {
                ObjectMap.Entry<String, ResourceData<ParticleEffect>> entry = this.items.get((int)var7_8);
                if (((String)entry.key).equals(fileName)) {
                    effectData = (ResourceData)entry.value;
                    this.items.removeIndex((int)var7_8);
                    break;
                }
                ++var7_8;
            }
        }
        ((ParticleEffect)effectData.resource).load(manager, effectData);
        if (parameter != null) {
            if (parameter.batches != null) {
                for (ParticleBatch particleBatch : parameter.batches) {
                    particleBatch.load(manager, effectData);
                }
            }
            ((ParticleEffect)effectData.resource).setBatch(parameter.batches);
        }
        return (ParticleEffect)effectData.resource;
    }

    private <T> T find(Array<?> array, Class<T> type) {
        for (Object object : array) {
            if (!ClassReflection.isAssignableFrom(type, object.getClass())) continue;
            return (T)object;
        }
        return null;
    }

    public static class ParticleEffectSaveParameter
    extends AssetLoaderParameters<ParticleEffect> {
        Array<ParticleBatch<?>> batches;
        FileHandle file;
        AssetManager manager;

        public ParticleEffectSaveParameter(FileHandle file, AssetManager manager, Array<ParticleBatch<?>> batches) {
            this.batches = batches;
            this.file = file;
            this.manager = manager;
        }
    }

    public static class ParticleEffectLoadParameter
    extends AssetLoaderParameters<ParticleEffect> {
        Array<ParticleBatch<?>> batches;

        public ParticleEffectLoadParameter(Array<ParticleBatch<?>> batches) {
            this.batches = batches;
        }
    }
}

