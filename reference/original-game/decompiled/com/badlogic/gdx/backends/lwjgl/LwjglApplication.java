/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationLogger;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglInput;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.backends.lwjgl.LwjglNet;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.SnapshotArray;
import java.awt.Canvas;
import java.io.File;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

public class LwjglApplication
implements Application {
    protected final LwjglGraphics graphics;
    protected OpenALAudio audio;
    protected final LwjglFiles files;
    protected final LwjglInput input;
    protected final LwjglNet net;
    protected final ApplicationListener listener;
    protected Thread mainLoopThread;
    protected boolean running = true;
    protected final Array<Runnable> runnables = new Array();
    protected final Array<Runnable> executedRunnables = new Array();
    protected final SnapshotArray<LifecycleListener> lifecycleListeners = new SnapshotArray(LifecycleListener.class);
    protected int logLevel = 2;
    protected ApplicationLogger applicationLogger;
    protected String preferencesdir;
    protected Files.FileType preferencesFileType;
    ObjectMap<String, Preferences> preferences = new ObjectMap();

    public LwjglApplication(ApplicationListener listener, String title, int width, int height) {
        this(listener, LwjglApplication.createConfig(title, width, height));
    }

    public LwjglApplication(ApplicationListener listener) {
        this(listener, null, 640, 480);
    }

    public LwjglApplication(ApplicationListener listener, LwjglApplicationConfiguration config) {
        this(listener, config, new LwjglGraphics(config));
    }

    public LwjglApplication(ApplicationListener listener, Canvas canvas) {
        this(listener, new LwjglApplicationConfiguration(), new LwjglGraphics(canvas));
    }

    public LwjglApplication(ApplicationListener listener, LwjglApplicationConfiguration config, Canvas canvas) {
        this(listener, config, new LwjglGraphics(canvas, config));
    }

    public LwjglApplication(ApplicationListener listener, LwjglApplicationConfiguration config, LwjglGraphics graphics) {
        LwjglNativesLoader.load();
        this.setApplicationLogger(new LwjglApplicationLogger());
        if (config.title == null) {
            config.title = listener.getClass().getSimpleName();
        }
        this.graphics = graphics;
        if (!LwjglApplicationConfiguration.disableAudio) {
            try {
                this.audio = new OpenALAudio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount, config.audioDeviceBufferSize);
            }
            catch (Throwable t) {
                this.log("LwjglApplication", "Couldn't initialize audio, disabling audio", t);
                LwjglApplicationConfiguration.disableAudio = true;
            }
        }
        this.files = new LwjglFiles();
        this.input = new LwjglInput();
        this.net = new LwjglNet();
        this.listener = listener;
        this.preferencesdir = config.preferencesDirectory;
        this.preferencesFileType = config.preferencesFileType;
        Gdx.app = this;
        Gdx.graphics = graphics;
        Gdx.audio = this.audio;
        Gdx.files = this.files;
        Gdx.input = this.input;
        Gdx.net = this.net;
        this.initialize();
    }

    private static LwjglApplicationConfiguration createConfig(String title, int width, int height) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        config.vSyncEnabled = true;
        return config;
    }

    private void initialize() {
        this.mainLoopThread = new Thread("LWJGL Application"){

            @Override
            public void run() {
                LwjglApplication.this.graphics.setVSync(LwjglApplication.this.graphics.config.vSyncEnabled);
                try {
                    LwjglApplication.this.mainLoop();
                }
                catch (Throwable t) {
                    if (LwjglApplication.this.audio != null) {
                        LwjglApplication.this.audio.dispose();
                    }
                    Gdx.input.setCursorCatched(false);
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
        SnapshotArray<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
        try {
            this.graphics.setupDisplay();
        }
        catch (LWJGLException e) {
            throw new GdxRuntimeException(e);
        }
        this.listener.create();
        this.graphics.resize = true;
        int lastWidth = this.graphics.getWidth();
        int lastHeight = this.graphics.getHeight();
        this.graphics.lastTime = System.nanoTime();
        boolean wasActive = true;
        while (this.running) {
            int frameRate;
            int i;
            int n;
            LifecycleListener[] listeners;
            Display.processMessages();
            if (Display.isCloseRequested()) {
                this.exit();
            }
            boolean isActive = Display.isActive();
            if (wasActive && !isActive) {
                wasActive = false;
                SnapshotArray<LifecycleListener> snapshotArray = lifecycleListeners;
                synchronized (snapshotArray) {
                    listeners = lifecycleListeners.begin();
                    n = lifecycleListeners.size;
                    for (i = 0; i < n; ++i) {
                        listeners[i].pause();
                    }
                    lifecycleListeners.end();
                }
                this.listener.pause();
            }
            if (!wasActive && isActive) {
                wasActive = true;
                SnapshotArray<LifecycleListener> snapshotArray = lifecycleListeners;
                synchronized (snapshotArray) {
                    listeners = lifecycleListeners.begin();
                    n = lifecycleListeners.size;
                    for (i = 0; i < n; ++i) {
                        listeners[i].resume();
                    }
                    lifecycleListeners.end();
                }
                this.listener.resume();
            }
            boolean shouldRender = false;
            if (this.graphics.canvas != null) {
                int width = this.graphics.canvas.getWidth();
                int height = this.graphics.canvas.getHeight();
                if (lastWidth != width || lastHeight != height) {
                    lastWidth = width;
                    lastHeight = height;
                    Gdx.gl.glViewport(0, 0, lastWidth, lastHeight);
                    this.listener.resize(lastWidth, lastHeight);
                    shouldRender = true;
                }
            } else {
                this.graphics.config.x = Display.getX();
                this.graphics.config.y = Display.getY();
                if (this.graphics.resize || Display.wasResized() || (int)((float)Display.getWidth() * Display.getPixelScaleFactor()) != this.graphics.config.width || (int)((float)Display.getHeight() * Display.getPixelScaleFactor()) != this.graphics.config.height) {
                    this.graphics.resize = false;
                    this.graphics.config.width = (int)((float)Display.getWidth() * Display.getPixelScaleFactor());
                    this.graphics.config.height = (int)((float)Display.getHeight() * Display.getPixelScaleFactor());
                    Gdx.gl.glViewport(0, 0, this.graphics.config.width, this.graphics.config.height);
                    if (this.listener != null) {
                        this.listener.resize(this.graphics.config.width, this.graphics.config.height);
                    }
                    this.graphics.requestRendering();
                }
            }
            if (this.executeRunnables()) {
                shouldRender = true;
            }
            if (!this.running) break;
            this.input.update();
            shouldRender |= this.graphics.shouldRender();
            this.input.processEvents();
            if (this.audio != null) {
                this.audio.update();
            }
            if (!isActive && this.graphics.config.backgroundFPS == -1) {
                shouldRender = false;
            }
            int n2 = frameRate = isActive ? this.graphics.config.foregroundFPS : this.graphics.config.backgroundFPS;
            if (shouldRender) {
                this.graphics.updateTime();
                ++this.graphics.frameId;
                this.listener.render();
                Display.update(false);
            } else {
                if (frameRate == -1) {
                    frameRate = 10;
                }
                if (frameRate == 0) {
                    frameRate = this.graphics.config.backgroundFPS;
                }
                if (frameRate == 0) {
                    frameRate = 30;
                }
            }
            if (frameRate <= 0) continue;
            Display.sync(frameRate);
        }
        SnapshotArray<LifecycleListener> snapshotArray = lifecycleListeners;
        synchronized (snapshotArray) {
            LifecycleListener[] listeners = lifecycleListeners.begin();
            int n = lifecycleListeners.size;
            for (int i = 0; i < n; ++i) {
                listeners[i].pause();
                listeners[i].dispose();
            }
            lifecycleListeners.end();
        }
        this.listener.pause();
        this.listener.dispose();
        Display.destroy();
        if (this.audio != null) {
            this.audio.dispose();
        }
        if (this.graphics.config.forceExit) {
            System.exit(-1);
        }
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
        do {
            this.executedRunnables.pop().run();
        } while (this.executedRunnables.size > 0);
        return true;
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return this.listener;
    }

    @Override
    public Audio getAudio() {
        return this.audio;
    }

    @Override
    public Files getFiles() {
        return this.files;
    }

    @Override
    public LwjglGraphics getGraphics() {
        return this.graphics;
    }

    @Override
    public Input getInput() {
        return this.input;
    }

    @Override
    public Net getNet() {
        return this.net;
    }

    @Override
    public Application.ApplicationType getType() {
        return Application.ApplicationType.Desktop;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    public void stop() {
        this.running = false;
        try {
            this.mainLoopThread.join();
        }
        catch (Exception exception) {
            // empty catch block
        }
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
        LwjglPreferences prefs = new LwjglPreferences(new LwjglFileHandle(new File(this.preferencesdir, name), this.preferencesFileType));
        this.preferences.put(name, prefs);
        return prefs;
    }

    @Override
    public Clipboard getClipboard() {
        return new LwjglClipboard();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void postRunnable(Runnable runnable) {
        Array<Runnable> array = this.runnables;
        synchronized (array) {
            this.runnables.add(runnable);
            Gdx.graphics.requestRendering();
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
                LwjglApplication.this.running = false;
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        SnapshotArray<LifecycleListener> snapshotArray = this.lifecycleListeners;
        synchronized (snapshotArray) {
            this.lifecycleListeners.add(listener);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        SnapshotArray<LifecycleListener> snapshotArray = this.lifecycleListeners;
        synchronized (snapshotArray) {
            this.lifecycleListeners.removeValue(listener, true);
        }
    }
}

