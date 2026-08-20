/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import java.io.InputStream;

public interface BaseJsonReader {
    public JsonValue parse(InputStream var1);

    public JsonValue parse(FileHandle var1);
}

