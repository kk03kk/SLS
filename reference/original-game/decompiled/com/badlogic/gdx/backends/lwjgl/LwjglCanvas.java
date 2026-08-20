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
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationLogger;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglInput;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.backends.lwjgl.LwjglNet;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.awt.Canvas;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.Display;

public class LwjglCanvas
implements Application {
    static boolean isWindows = System.getProperty("os.name").contains("Windows");
    LwjglGraphics graphics;
    OpenALAudio audio;
    LwjglFiles files;
    LwjglInput input;
    LwjglNet net;
    ApplicationListener listener;
    Canvas canvas;
    final Array<Runnable> runnables = new Array();
    final Array<Runnable> executedRunnables = new Array();
    final Array<LifecycleListener> lifecycleListeners = new Array();
    boolean running = true;
    int logLevel = 2;
    ApplicationLogger applicationLogger;
    Cursor cursor;
    Map<String, Preferences> preferences = new HashMap<String, Preferences>();

    public LwjglCanvas(ApplicationListener listener) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        this.initialize(listener, config);
    }

    public LwjglCanvas(ApplicationListener listener, LwjglApplicationConfiguration config) {
        this.initialize(listener, config);
    }

    private void initialize(ApplicationListener listener, LwjglApplicationConfiguration config) {
        LwjglNativesLoader.load();
        this.setApplicationLogger(new LwjglApplicationLogger());
        this.canvas = new Canvas(){
            private final Dimension minSize = new Dimension(1, 1);

            @Override
            public final void addNotify() {
                super.addNotify();
                if (SharedLibraryLoader.isMac) {
                    EventQueue.invokeLater(new Runnable(){

                        @Override
                        public void run() {
                            LwjglCanvas.this.create();
                        }
                    });
                } else {
                    LwjglCanvas.this.create();
                }
            }

            @Override
            public final void removeNotify() {
                LwjglCanvas.this.stop();
                super.removeNotify();
            }

            @Override
            public Dimension getMinimumSize() {
                return this.minSize;
            }
        };
        this.canvas.setSize(1, 1);
        this.canvas.setIgnoreRepaint(true);
        this.graphics = new LwjglGraphics(this.canvas, config){

            @Override
            public void setTitle(String title) {
                super.setTitle(title);
                LwjglCanvas.this.setTitle(title);
            }

            public boolean setWindowedMode(int width, int height, boolean fullscreen) {
                if (!super.setWindowedMode(width, height)) {
                    return false;
                }
                if (!fullscreen) {
                    LwjglCanvas.this.setDisplayMode(width, height);
                }
                return true;
            }

            @Override
            public boolean setFullscreenMode(Graphics.DisplayMode displayMode) {
                if (!super.setFullscreenMode(displayMode)) {
                    return false;
                }
                LwjglCanvas.this.setDisplayMode(displayMode.width, displayMode.height);
                return true;
            }
        };
        this.graphics.setVSync(config.vSyncEnabled);
        if (!LwjglApplicationConfiguration.disableAudio) {
            this.audio = new OpenALAudio();
        }
        this.files = new LwjglFiles();
        this.input = new LwjglInput();
        this.net = new LwjglNet();
        this.listener = listener;
        Gdx.app = this;
        Gdx.graphics = this.graphics;
        Gdx.audio = this.audio;
        Gdx.files = this.files;
        Gdx.input = this.input;
        Gdx.net = this.net;
    }

    protected void setDisplayMode(int width, int height) {
    }

    protected void setTitle(String title) {
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return this.listener;
    }

    public Canvas getCanvas() {
        return this.canvas;
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
    public Graphics getGraphics() {
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

    void create() {
        try {
            this.graphics.setupDisplay();
            this.listener.create();
            this.listener.resize(Math.max(1, this.graphics.getWidth()), Math.max(1, this.graphics.getHeight()));
            this.start();
        }
        catch (Exception ex) {
            this.stopped();
            this.exception(ex);
            return;
        }
        EventQueue.invokeLater(new Runnable(){
            int lastWidth;
            int lastHeight;
            {
                this.lastWidth = Math.max(1, LwjglCanvas.this.graphics.getWidth());
                this.lastHeight = Math.max(1, LwjglCanvas.this.graphics.getHeight());
            }

            @Override
            public void run() {
                if (!LwjglCanvas.this.running || Display.isCloseRequested()) {
                    LwjglCanvas.this.running = false;
                    LwjglCanvas.this.stopped();
                    return;
                }
                try {
                    Display.processMessages();
                    if (LwjglCanvas.this.cursor != null || !isWindows) {
                        LwjglCanvas.this.canvas.setCursor(LwjglCanvas.this.cursor);
                    }
                    boolean shouldRender = false;
                    int width = Math.max(1, LwjglCanvas.this.graphics.getWidth());
                    int height = Math.max(1, LwjglCanvas.this.graphics.getHeight());
                    if (this.lastWidth != width || this.lastHeight != height) {
                        this.lastWidth = width;
                        this.lastHeight = height;
                        Gdx.gl.glViewport(0, 0, this.lastWidth, this.lastHeight);
                        LwjglCanvas.this.resize(width, height);
                        LwjglCanvas.this.listener.resize(width, height);
                        shouldRender = true;
                    }
                    if (LwjglCanvas.this.executeRunnables()) {
                        shouldRender = true;
                    }
                    if (!LwjglCanvas.this.running) {
                        return;
                    }
                    LwjglCanvas.this.input.update();
                    shouldRender |= LwjglCanvas.this.graphics.shouldRender();
                    LwjglCanvas.this.input.processEvents();
                    if (LwjglCanvas.this.audio != null) {
                        LwjglCanvas.this.audio.update();
                    }
                    if (shouldRender) {
                        LwjglCanvas.this.graphics.updateTime();
                        ++LwjglCanvas.this.graphics.frameId;
                        LwjglCanvas.this.listener.render();
                        Display.update(false);
                    }
                    Display.sync(LwjglCanvas.this.getFrameRate());
                }
                catch (Throwable ex) {
                    LwjglCanvas.this.exception(ex);
                }
                EventQueue.invokeLater(this);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean executeRunnables() {
        Array<Runnable> array = this.runnables;
        synchronized (array) {
            for (int i = this.runnables.size - 1; i >= 0; --i) {
                this.executedRunnables.addAll((Runnable[])new Runnable[]{this.runnables.get(i)});
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

    protected int getFrameRate() {
        int frameRate;
        int n = frameRate = Display.isActive() ? this.graphics.config.foregroundFPS : this.graphics.config.backgroundFPS;
        if (frameRate == -1) {
            frameRate = 10;
        }
        if (frameRate == 0) {
            frameRate = this.graphics.config.backgroundFPS;
        }
        if (frameRate == 0) {
            frameRate = 30;
        }
        return frameRate;
    }

    protected void exception(Throwable ex) {
        ex.printStackTrace();
        this.stop();
    }

    protected void start() {
    }

    protected void resize(int width, int height) {
    }

    protected void stopped() {
    }

    public void stop() {
        EventQueue.invokeLater(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Array<LifecycleListener> listeners;
                if (!LwjglCanvas.this.running) {
                    return;
                }
                LwjglCanvas.this.running = false;
                Array<LifecycleListener> array = listeners = LwjglCanvas.this.lifecycleListeners;
                synchronized (array) {
                    for (LifecycleListener listener : listeners) {
                        listener.pause();
                        listener.dispose();
                    }
                }
                LwjglCanvas.this.listener.pause();
                LwjglCanvas.this.listener.dispose();
                try {
                    Display.destroy();
                    if (LwjglCanvas.this.audio != null) {
                        LwjglCanvas.this.audio.dispose();
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        });
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
        LwjglPreferences prefs = new LwjglPreferences(name, ".prefs/");
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
                LwjglCanvas.this.listener.pause();
                LwjglCanvas.this.listener.dispose();
                if (LwjglCanvas.this.audio != null) {
                    LwjglCanvas.this.audio.dispose();
                }
                System.exit(-1);
            }
        });
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
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

