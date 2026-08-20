/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.files.FileHandle;
import java.io.File;

public final class LwjglFiles
implements Files {
    public static final String externalPath = System.getProperty("user.home") + File.separator;
    public static final String localPath = new File("").getAbsolutePath() + File.separator;

    @Override
    public FileHandle getFileHandle(String fileName, Files.FileType type) {
        return new LwjglFileHandle(fileName, type);
    }

    @Override
    public FileHandle classpath(String path) {
        return new LwjglFileHandle(path, Files.FileType.Classpath);
    }

    @Override
    public FileHandle internal(String path) {
        return new LwjglFileHandle(path, Files.FileType.Internal);
    }

    @Override
    public FileHandle external(String path) {
        return new LwjglFileHandle(path, Files.FileType.External);
    }

    @Override
    public FileHandle absolute(String path) {
        return new LwjglFileHandle(path, Files.FileType.Absolute);
    }

    @Override
    public FileHandle local(String path) {
        return new LwjglFileHandle(path, Files.FileType.Local);
    }

    @Override
    public String getExternalStoragePath() {
        return externalPath;
    }

    @Override
    public boolean isExternalStorageAvailable() {
        return true;
    }

    @Override
    public String getLocalStoragePath() {
        return localPath;
    }

    @Override
    public boolean isLocalStorageAvailable() {
        return true;
    }
}

