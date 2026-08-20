/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.io.File;
import java.lang.reflect.Method;

public final class LwjglNativesLoader {
    public static boolean load = true;

    public static void load() {
        GdxNativesLoader.load();
        if (GdxNativesLoader.disableNativesLoading) {
            return;
        }
        if (!load) {
            return;
        }
        SharedLibraryLoader loader = new SharedLibraryLoader();
        File nativesDir = null;
        try {
            if (SharedLibraryLoader.isWindows) {
                nativesDir = loader.extractFile(SharedLibraryLoader.is64Bit ? "lwjgl64.dll" : "lwjgl.dll", null).getParentFile();
                if (!LwjglApplicationConfiguration.disableAudio) {
                    loader.extractFileTo(SharedLibraryLoader.is64Bit ? "OpenAL64.dll" : "OpenAL32.dll", nativesDir);
                }
            } else if (SharedLibraryLoader.isMac) {
                nativesDir = loader.extractFile("liblwjgl.dylib", null).getParentFile();
                if (!LwjglApplicationConfiguration.disableAudio) {
                    loader.extractFileTo("openal.dylib", nativesDir);
                }
            } else if (SharedLibraryLoader.isLinux) {
                nativesDir = loader.extractFile(SharedLibraryLoader.is64Bit ? "liblwjgl64.so" : "liblwjgl.so", null).getParentFile();
                if (!LwjglApplicationConfiguration.disableAudio) {
                    loader.extractFileTo(SharedLibraryLoader.is64Bit ? "libopenal64.so" : "libopenal.so", nativesDir);
                }
            }
        }
        catch (Throwable ex) {
            throw new GdxRuntimeException("Unable to extract LWJGL natives.", ex);
        }
        System.setProperty("org.lwjgl.librarypath", nativesDir.getAbsolutePath());
        load = false;
    }

    static {
        System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
        try {
            Method method = Class.forName("javax.jnlp.ServiceManager").getDeclaredMethod("lookup", String.class);
            method.invoke(null, "javax.jnlp.PersistenceService");
            load = false;
        }
        catch (Throwable ex) {
            load = true;
        }
    }
}

