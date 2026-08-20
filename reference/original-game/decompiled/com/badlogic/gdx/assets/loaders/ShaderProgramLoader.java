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
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

public class ShaderProgramLoader
extends AsynchronousAssetLoader<ShaderProgram, ShaderProgramParameter> {
    private String vertexFileSuffix = ".vert";
    private String fragmentFileSuffix = ".frag";

    public ShaderProgramLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    public ShaderProgramLoader(FileHandleResolver resolver, String vertexFileSuffix, String fragmentFileSuffix) {
        super(resolver);
        this.vertexFileSuffix = vertexFileSuffix;
        this.fragmentFileSuffix = fragmentFileSuffix;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, ShaderProgramParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
    }

    @Override
    public ShaderProgram loadSync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
        String fragmentCode;
        String vertFileName = null;
        String fragFileName = null;
        if (parameter != null) {
            if (parameter.vertexFile != null) {
                vertFileName = parameter.vertexFile;
            }
            if (parameter.fragmentFile != null) {
                fragFileName = parameter.fragmentFile;
            }
        }
        if (vertFileName == null && fileName.endsWith(this.fragmentFileSuffix)) {
            vertFileName = fileName.substring(0, fileName.length() - this.fragmentFileSuffix.length()) + this.vertexFileSuffix;
        }
        if (fragFileName == null && fileName.endsWith(this.vertexFileSuffix)) {
            fragFileName = fileName.substring(0, fileName.length() - this.vertexFileSuffix.length()) + this.fragmentFileSuffix;
        }
        FileHandle vertexFile = vertFileName == null ? file : this.resolve(vertFileName);
        FileHandle fragmentFile = fragFileName == null ? file : this.resolve(fragFileName);
        String vertexCode = vertexFile.readString();
        String string = fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : fragmentFile.readString();
        if (parameter != null) {
            if (parameter.prependVertexCode != null) {
                vertexCode = parameter.prependVertexCode + vertexCode;
            }
            if (parameter.prependFragmentCode != null) {
                fragmentCode = parameter.prependFragmentCode + fragmentCode;
            }
        }
        ShaderProgram shaderProgram = new ShaderProgram(vertexCode, fragmentCode);
        if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
            manager.getLogger().error("ShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog());
        }
        return shaderProgram;
    }

    public static class ShaderProgramParameter
    extends AssetLoaderParameters<ShaderProgram> {
        public String vertexFile;
        public String fragmentFile;
        public boolean logOnCompileFailure = true;
        public String prependVertexCode;
        public String prependFragmentCode;
    }
}

