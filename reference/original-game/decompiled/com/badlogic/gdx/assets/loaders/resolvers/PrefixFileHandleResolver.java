/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders.resolvers;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

public class PrefixFileHandleResolver
implements FileHandleResolver {
    private String prefix;
    private FileHandleResolver baseResolver;

    public PrefixFileHandleResolver(FileHandleResolver baseResolver, String prefix) {
        this.baseResolver = baseResolver;
        this.prefix = prefix;
    }

    public void setBaseResolver(FileHandleResolver baseResolver) {
        this.baseResolver = baseResolver;
    }

    public FileHandleResolver getBaseResolver() {
        return this.baseResolver;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public FileHandle resolve(String fileName) {
        return this.baseResolver.resolve(this.prefix + fileName);
    }
}

