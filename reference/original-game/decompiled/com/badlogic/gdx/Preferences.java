/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import java.util.Map;

public interface Preferences {
    public Preferences putBoolean(String var1, boolean var2);

    public Preferences putInteger(String var1, int var2);

    public Preferences putLong(String var1, long var2);

    public Preferences putFloat(String var1, float var2);

    public Preferences putString(String var1, String var2);

    public Preferences put(Map<String, ?> var1);

    public boolean getBoolean(String var1);

    public int getInteger(String var1);

    public long getLong(String var1);

    public float getFloat(String var1);

    public String getString(String var1);

    public boolean getBoolean(String var1, boolean var2);

    public int getInteger(String var1, int var2);

    public long getLong(String var1, long var2);

    public float getFloat(String var1, float var2);

    public String getString(String var1, String var2);

    public Map<String, ?> get();

    public boolean contains(String var1);

    public void clear();

    public void remove(String var1);

    public void flush();
}

