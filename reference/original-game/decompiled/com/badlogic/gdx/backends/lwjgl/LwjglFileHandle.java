/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.File;

public final class LwjglFileHandle
extends FileHandle {
    public LwjglFileHandle(String fileName, Files.FileType type) {
        super(fileName, type);
    }

    public LwjglFileHandle(File file, Files.FileType type) {
        super(file, type);
    }

    @Override
    public FileHandle child(String name) {
        if (this.file.getPath().length() == 0) {
            return new LwjglFileHandle(new File(name), this.type);
        }
        return new LwjglFileHandle(new File(this.file, name), this.type);
    }

    @Override
    public FileHandle sibling(String name) {
        if (this.file.getPath().length() == 0) {
            throw new GdxRuntimeException("Cannot get the sibling of the root.");
        }
        return new LwjglFileHandle(new File(this.file.getParent(), name), this.type);
    }

    @Override
    public FileHandle parent() {
        File parent = this.file.getParentFile();
        if (parent == null) {
            parent = this.type == Files.FileType.Absolute ? new File("/") : new File("");
        }
        return new LwjglFileHandle(parent, this.type);
    }

    @Override
    public File file() {
        if (this.type == Files.FileType.External) {
            return new File(LwjglFiles.externalPath, this.file.getPath());
        }
        if (this.type == Files.FileType.Local) {
            return new File(LwjglFiles.localPath, this.file.getPath());
        }
        return this.file;
    }
}

