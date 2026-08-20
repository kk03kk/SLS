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
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.Iterator;

public abstract class ModelLoader<P extends ModelParameters>
extends AsynchronousAssetLoader<Model, P> {
    protected Array<ObjectMap.Entry<String, ModelData>> items = new Array();
    protected ModelParameters defaultParameters = new ModelParameters();

    public ModelLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    public abstract ModelData loadModelData(FileHandle var1, P var2);

    public ModelData loadModelData(FileHandle fileHandle) {
        return this.loadModelData(fileHandle, null);
    }

    public Model loadModel(FileHandle fileHandle, TextureProvider textureProvider, P parameters) {
        ModelData data = this.loadModelData(fileHandle, parameters);
        return data == null ? null : new Model(data, textureProvider);
    }

    public Model loadModel(FileHandle fileHandle, P parameters) {
        return this.loadModel(fileHandle, new TextureProvider.FileTextureProvider(), parameters);
    }

    public Model loadModel(FileHandle fileHandle, TextureProvider textureProvider) {
        return this.loadModel(fileHandle, textureProvider, null);
    }

    public Model loadModel(FileHandle fileHandle) {
        return this.loadModel(fileHandle, new TextureProvider.FileTextureProvider(), null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, P parameters) {
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>();
        ModelData data = this.loadModelData(file, parameters);
        if (data == null) {
            return deps;
        }
        ObjectMap.Entry item = new ObjectMap.Entry();
        item.key = fileName;
        item.value = data;
        Array<ObjectMap.Entry<String, ModelData>> array = this.items;
        synchronized (array) {
            this.items.add(item);
        }
        TextureLoader.TextureParameter textureParameter = parameters != null ? ((ModelParameters)parameters).textureParameter : this.defaultParameters.textureParameter;
        for (ModelMaterial modelMaterial : data.materials) {
            if (modelMaterial.textures == null) continue;
            for (ModelTexture modelTexture : modelMaterial.textures) {
                deps.add(new AssetDescriptor<Texture>(modelTexture.fileName, Texture.class, textureParameter));
            }
        }
        return deps;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, P parameters) {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Model loadSync(AssetManager manager, String fileName, FileHandle file, P parameters) {
        ModelData data = null;
        Array<ObjectMap.Entry<String, ModelData>> array = this.items;
        synchronized (array) {
            for (int i = 0; i < this.items.size; ++i) {
                if (!((String)this.items.get((int)i).key).equals(fileName)) continue;
                data = (ModelData)this.items.get((int)i).value;
                this.items.removeIndex(i);
            }
        }
        if (data == null) {
            return null;
        }
        Model result = new Model(data, new TextureProvider.AssetTextureProvider(manager));
        Iterator<Disposable> disposables = result.getManagedDisposables().iterator();
        while (disposables.hasNext()) {
            Disposable disposable = disposables.next();
            if (!(disposable instanceof Texture)) continue;
            disposables.remove();
        }
        data = null;
        return result;
    }

    public static class ModelParameters
    extends AssetLoaderParameters<Model> {
        public TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();

        public ModelParameters() {
            this.textureParameter.minFilter = this.textureParameter.magFilter = Texture.TextureFilter.Linear;
            this.textureParameter.wrapU = this.textureParameter.wrapV = Texture.TextureWrap.Repeat;
        }
    }
}

