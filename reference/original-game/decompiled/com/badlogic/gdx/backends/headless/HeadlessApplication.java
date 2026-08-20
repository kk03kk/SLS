/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.headless;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessApplicationLogger;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.badlogic.gdx.backends.headless.HeadlessNativesLoader;
import com.badlogic.gdx.backends.headless.HeadlessNet;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.backends.headless.mock.audio.MockAudio;
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;

public class HeadlessApplication
implements Application {
    protected final ApplicationListener listener;
    protected Thread mainLoopThread;
    protected final HeadlessFiles files;
    protected final HeadlessNet net;
    protected final MockAudio audio;
    protected final MockInput input;
    protected final MockGraphics graphics;
    protected boolean running = true;
    protected final Array<Runnable> runnables = new Array();
    protected final Array<Runnable> executedRunnables = new Array();
    protected final Array<LifecycleListener> lifecycleListeners = new Array();
    protected int logLevel = 2;
    protected ApplicationLogger applicationLogger;
    private String preferencesdir;
    private final long renderInterval;
    ObjectMap<String, Preferences> preferences = new ObjectMap();

    public HeadlessApplication(ApplicationListener listener) {
        this(listener, null);
    }

    public HeadlessApplication(ApplicationListener listener, HeadlessApplicationConfiguration config) {
        if (config == null) {
            config = new HeadlessApplicationConfiguration();
        }
        HeadlessNativesLoader.load();
        this.setApplicationLogger(new HeadlessApplicationLogger());
        this.listener = listener;
        this.files = new HeadlessFiles();
        this.net = new HeadlessNet();
        this.graphics = new MockGraphics();
        this.audio = new MockAudio();
        this.input = new MockInput();
        this.preferencesdir = config.preferencesDirectory;
        Gdx.app = this;
        Gdx.files = this.files;
        Gdx.net = this.net;
        Gdx.audio = this.audio;
        Gdx.graphics = this.graphics;
        Gdx.input = this.input;
        this.renderInterval = config.renderInterval > 0.0f ? (long)(config.renderInterval * 1.0E9f) : (long)(config.renderInterval < 0.0f ? -1 : 0);
        this.initialize();
    }

    private void initialize() {
        this.mainLoopThread = new Thread("HeadlessApplication"){

            @Override
            public void run() {
                try {
                    HeadlessApplication.this.mainLoop();
                }
                catch (Throwable t) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    }
                    throw new GdxRuntimeException(t);
                }
            }
        };
        this.mainLoopThread.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void mainLoop() {
        Array<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
        this.listener.create();
        long t = TimeUtils.nanoTime() + this.renderInterval;
        if ((float)this.renderInterval >= 0.0f) {
            while (this.running) {
                long n = TimeUtils.nanoTime();
                if (t > n) {
                    try {
                        Thread.sleep((t - n) / 1000000L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    t = TimeUtils.nanoTime() + this.renderInterval;
                } else {
                    t = n + this.renderInterval;
                }
                this.executeRunnables();
                this.graphics.incrementFrameId();
                this.listener.render();
                this.graphics.updateTime();
                if (this.running) continue;
                break;
            }
        }
        Array<LifecycleListener> array = lifecycleListeners;
        synchronized (array) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        this.listener.pause();
        this.listener.dispose();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean executeRunnables() {
        Array<Runnable> array = this.runnables;
        synchronized (array) {
            for (int i = this.runnables.size - 1; i >= 0; --i) {
                this.executedRunnables.add(this.runnables.get(i));
            }
            this.runnables.clear();
        }
        if (this.executedRunnables.size == 0) {
            return false;
        }
        for (int i = this.executedRunnables.size - 1; i >= 0; --i) {
            this.executedRunnables.removeIndex(i).run();
        }
        return true;
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return this.listener;
    }

    @Override
    public Graphics getGraphics() {
        return this.graphics;
    }

    @Override
    public Audio getAudio() {
        return this.audio;
    }

    @Override
    public Input getInput() {
        return this.input;
    }

    @Override
    public Files getFiles() {
        return this.files;
    }

    @Override
    public Net getNet() {
        return this.net;
    }

    @Override
    public Application.ApplicationType getType() {
        return Application.ApplicationType.HeadlessDesktop;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return this.getJavaHeap();
    }

    @Override
    public Preferences getPreferences(String name) {
        if (this.preferences.containsKey(name)) {
            return this.preferences.get(name);
        }
        HeadlessPreferences prefs = new HeadlessPreferences(name, this.preferencesdir);
        this.preferences.put(name, prefs);
        return prefs;
    }

    @Override
    public Clipboard getClipboard() {
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void postRunnable(Runnable runnable) {
        Array<Runnable> array = this.runnables;
        synchronized (array) {
            this.runnables.add(runnable);
        }
    }

    @Override
    public void debug(String tag, String message) {
        if (this.logLevel >= 3) {
            this.getApplicationLogger().debug(tag, message);
        }
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        if (this.logLevel >= 3) {
            this.getApplicationLogger().debug(tag, message, exception);
        }
    }

    @Override
    public void log(String tag, String message) {
        if (this.logLevel >= 2) {
            this.getApplicationLogger().log(tag, message);
        }
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        if (this.logLevel >= 2) {
            this.getApplicationLogger().log(tag, message, exception);
        }
    }

    @Override
    public void error(String tag, String message) {
        if (this.logLevel >= 1) {
            this.getApplicationLogger().error(tag, message);
        }
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        if (this.logLevel >= 1) {
            this.getApplicationLogger().error(tag, message, exception);
        }
    }

    @Override
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public int getLogLevel() {
        return this.logLevel;
    }

    @Override
    public void setApplicationLogger(ApplicationLogger applicationLogger) {
        this.applicationLogger = applicationLogger;
    }

    @Override
    public ApplicationLogger getApplicationLogger() {
        return this.applicationLogger;
    }

    @Override
    public void exit() {
        this.postRunnable(new Runnable(){

            @Override
            public void run() {
                HeadlessApplication.this.running = false;
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        Array<LifecycleListener> array = this.lifecycleListeners;
        synchronized (array) {
            this.lifecycleListeners.add(listener);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        Array<LifecycleListener> array = this.lifecycleListeners;
        synchronized (array) {
            this.lifecycleListeners.removeValue(listener, true);
        }
    }
}

