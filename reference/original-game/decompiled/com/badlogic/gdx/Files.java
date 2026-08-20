/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import com.badlogic.gdx.files.FileHandle;

public interface Files {
    public FileHandle getFileHandle(String var1, FileType var2);

    public FileHandle classpath(String var1);

    public FileHandle internal(String var1);

    public FileHandle external(String var1);

    public FileHandle absolute(String var1);

    public FileHandle local(String var1);

    public String getExternalStoragePath();

    public boolean isExternalStorageAvailable();

    public String getLocalStoragePath();

    public boolean isLocalStorageAvailable();

    public static enum FileType {
        Classpath,
        Internal,
        External,
        Absolute,
        Local;

    }
}

