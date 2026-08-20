/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Clipboard;

public interface Application {
    public static final int LOG_NONE = 0;
    public static final int LOG_DEBUG = 3;
    public static final int LOG_INFO = 2;
    public static final int LOG_ERROR = 1;

    public ApplicationListener getApplicationListener();

    public Graphics getGraphics();

    public Audio getAudio();

    public Input getInput();

    public Files getFiles();

    public Net getNet();

    public void log(String var1, String var2);

    public void log(String var1, String var2, Throwable var3);

    public void error(String var1, String var2);

    public void error(String var1, String var2, Throwable var3);

    public void debug(String var1, String var2);

    public void debug(String var1, String var2, Throwable var3);

    public void setLogLevel(int var1);

    public int getLogLevel();

    public void setApplicationLogger(ApplicationLogger var1);

    public ApplicationLogger getApplicationLogger();

    public ApplicationType getType();

    public int getVersion();

    public long getJavaHeap();

    public long getNativeHeap();

    public Preferences getPreferences(String var1);

    public Clipboard getClipboard();

    public void postRunnable(Runnable var1);

    public void exit();

    public void addLifecycleListener(LifecycleListener var1);

    public void removeLifecycleListener(LifecycleListener var1);

    public static enum ApplicationType {
        Android,
        Desktop,
        HeadlessDesktop,
        Applet,
        WebGL,
        iOS;

    }
}

