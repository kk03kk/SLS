/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.headless;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessFileHandle;
import com.badlogic.gdx.files.FileHandle;
import java.io.File;

public final class HeadlessFiles
implements Files {
    public static final String externalPath = System.getProperty("user.home") + File.separator;
    public static final String localPath = new File("").getAbsolutePath() + File.separator;

    @Override
    public FileHandle getFileHandle(String fileName, Files.FileType type) {
        return new HeadlessFileHandle(fileName, type);
    }

    @Override
    public FileHandle classpath(String path) {
        return new HeadlessFileHandle(path, Files.FileType.Classpath);
    }

    @Override
    public FileHandle internal(String path) {
        return new HeadlessFileHandle(path, Files.FileType.Internal);
    }

    @Override
    public FileHandle external(String path) {
        return new HeadlessFileHandle(path, Files.FileType.External);
    }

    @Override
    public FileHandle absolute(String path) {
        return new HeadlessFileHandle(path, Files.FileType.Absolute);
    }

    @Override
    public FileHandle local(String path) {
        return new HeadlessFileHandle(path, Files.FileType.Local);
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

