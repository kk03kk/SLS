/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.megacrit.cardcrawl.helpers.AsyncSaver;
import java.util.HashMap;
import java.util.Map;

public class Prefs {
    public Map<String, String> data = new HashMap<String, String>();
    public String filepath;

    public String getString(String key) {
        return this.data.getOrDefault(key, "");
    }

    public String getString(String key, String def) {
        return this.data.getOrDefault(key, def);
    }

    public void putString(String key, String value) {
        this.data.put(key, value);
    }

    public int getInteger(String key) {
        if (this.data.containsKey(key)) {
            return Integer.valueOf(this.data.get(key).trim());
        }
        return -999;
    }

    public int getInteger(String key, int def) {
        if (this.data.containsKey(key)) {
            return Integer.valueOf(this.data.get(key).trim());
        }
        return def;
    }

    public void putInteger(String key, int value) {
        this.data.put(key, Integer.toString(value));
    }

    public float getFloat(String key, float def) {
        if (this.data.containsKey(key)) {
            return Float.valueOf(this.data.get(key).trim()).floatValue();
        }
        return def;
    }

    public void putFloat(String key, float value) {
        this.data.put(key, Float.toString(value));
    }

    public long getLong(String key, long def) {
        if (this.data.containsKey(key)) {
            return Long.valueOf(this.data.get(key).trim());
        }
        return def;
    }

    public void putLong(String key, long value) {
        this.data.put(key, Long.toString(value));
    }

    public boolean getBoolean(String key, boolean def) {
        if (this.data.containsKey(key)) {
            return Boolean.valueOf(this.data.get(key).trim());
        }
        return def;
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(this.data.get(key).trim());
    }

    public void putBoolean(String key, boolean value) {
        this.data.put(key, Boolean.toString(value));
    }

    public void flush() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        AsyncSaver.save(this.filepath, gson.toJson(this.data));
    }

    public Map<String, String> get() {
        return this.data;
    }
}

