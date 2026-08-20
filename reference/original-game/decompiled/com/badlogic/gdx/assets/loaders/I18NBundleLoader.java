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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;
import java.util.Locale;

public class I18NBundleLoader
extends AsynchronousAssetLoader<I18NBundle, I18NBundleParameter> {
    I18NBundle bundle;

    public I18NBundleLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, I18NBundleParameter parameter) {
        String encoding;
        Locale locale;
        this.bundle = null;
        if (parameter == null) {
            locale = Locale.getDefault();
            encoding = null;
        } else {
            locale = parameter.locale == null ? Locale.getDefault() : parameter.locale;
            encoding = parameter.encoding;
        }
        this.bundle = encoding == null ? I18NBundle.createBundle(file, locale) : I18NBundle.createBundle(file, locale, encoding);
    }

    @Override
    public I18NBundle loadSync(AssetManager manager, String fileName, FileHandle file, I18NBundleParameter parameter) {
        I18NBundle bundle = this.bundle;
        this.bundle = null;
        return bundle;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, I18NBundleParameter parameter) {
        return null;
    }

    public static class I18NBundleParameter
    extends AssetLoaderParameters<I18NBundle> {
        public final Locale locale;
        public final String encoding;

        public I18NBundleParameter() {
            this(null, null);
        }

        public I18NBundleParameter(Locale locale) {
            this(locale, null);
        }

        public I18NBundleParameter(Locale locale, String encoding) {
            this.locale = locale;
            this.encoding = encoding;
        }
    }
}

