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
import com.badlogic.gdx.backends.lwjgl.LwjglAWTInput;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationLogger;
import com.badlogic.gdx.backends.lwjgl.LwjglClipboard;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.backends.lwjgl.LwjglNet;
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.PaintEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

public class LwjglAWTCanvas
implements Application {
    static int instanceCount;
    LwjglGraphics graphics;
    OpenALAudio audio;
    LwjglFiles files;
    LwjglAWTInput input;
    LwjglNet net;
    final ApplicationListener listener;
    AWTGLCanvas canvas;
    final Array<Runnable> runnables = new Array();
    final Array<Runnable> executedRunnables = new Array();
    final Array<LifecycleListener> lifecycleListeners = new Array();
    boolean running = true;
    int lastWidth;
    int lastHeight;
    int logLevel = 2;
    ApplicationLogger applicationLogger;
    final String logTag = "LwjglAWTCanvas";
    Cursor cursor;
    Map<String, Preferences> preferences = new HashMap<String, Preferences>();

    public LwjglAWTCanvas(ApplicationListener listener) {
        this(listener, null, null);
    }

    public LwjglAWTCanvas(ApplicationListener listener, LwjglAWTCanvas sharedContextCanvas) {
        this(listener, null, sharedContextCanvas);
    }

    public LwjglAWTCanvas(ApplicationListener listener, LwjglApplicationConfiguration config) {
        this(listener, config, null);
    }

    public LwjglAWTCanvas(ApplicationListener listener, LwjglApplicationConfiguration config, LwjglAWTCanvas sharedContextCanvas) {
        this.listener = listener;
        if (config == null) {
            config = new LwjglApplicationConfiguration();
        }
        LwjglNativesLoader.load();
        this.setApplicationLogger(new LwjglApplicationLogger());
        ++instanceCount;
        AWTGLCanvas sharedDrawable = sharedContextCanvas != null ? sharedContextCanvas.canvas : null;
        try {
            this.canvas = new AWTGLCanvas(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice(), new PixelFormat(), sharedDrawable){
                private final Dimension minSize;
                private final NonSystemPaint nonSystemPaint;
                {
                    this.minSize = new Dimension(0, 0);
                    this.nonSystemPaint = new NonSystemPaint(this);
                }

                @Override
                public Dimension getMinimumSize() {
                    return this.minSize;
                }

                @Override
                public void initGL() {
                    LwjglAWTCanvas.this.create();
                }

                @Override
                public void paintGL() {
                    try {
                        boolean systemPaint = !(EventQueue.getCurrentEvent() instanceof NonSystemPaint);
                        LwjglAWTCanvas.this.render(systemPaint);
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(this.nonSystemPaint);
                    }
                    catch (Throwable ex) {
                        LwjglAWTCanvas.this.exception(ex);
                    }
                }
            };
        }
        catch (Throwable ex) {
            this.exception(ex);
            return;
        }
        this.canvas.setBackground(new Color(config.initialBackgroundColor.r, config.initialBackgroundColor.g, config.initialBackgroundColor.b, config.initialBackgroundColor.a));
        this.graphics = new LwjglGraphics(this.canvas, config){

            @Override
            public void setTitle(String title) {
                super.setTitle(title);
                LwjglAWTCanvas.this.setTitle(title);
            }

            @Override
            public boolean setWindowedMode(int width, int height) {
                if (!super.setWindowedMode(width, height)) {
                    return false;
                }
                LwjglAWTCanvas.this.setDisplayMode(width, height);
                return true;
            }

            @Override
            public boolean setFullscreenMode(Graphics.DisplayMode displayMode) {
                if (!super.setFullscreenMode(displayMode)) {
                    return false;
                }
                LwjglAWTCanvas.this.setDisplayMode(displayMode.width, displayMode.height);
                return true;
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public boolean shouldRender() {
                2 var1_1 = this;
                synchronized (var1_1) {
                    boolean rq = this.requestRendering;
                    this.requestRendering = false;
                    return rq || this.isContinuous;
                }
            }
        };
        if (!LwjglApplicationConfiguration.disableAudio && Gdx.audio == null) {
            this.audio = new OpenALAudio();
        }
        if (Gdx.files == null) {
            this.files = new LwjglFiles();
        }
        if (Gdx.net == null) {
            this.net = new LwjglNet();
        }
        this.input = new LwjglAWTInput(this);
        this.setGlobals();
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
        return Gdx.audio;
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

    void setGlobals() {
        Gdx.app = this;
        if (this.audio != null) {
            Gdx.audio = this.audio;
        }
        if (this.files != null) {
            Gdx.files = this.files;
        }
        if (this.net != null) {
            Gdx.net = this.net;
        }
        Gdx.graphics = this.graphics;
        Gdx.input = this.input;
    }

    void create() {
        try {
            this.setGlobals();
            this.graphics.initiateGL();
            this.canvas.setVSyncEnabled(this.graphics.config.vSyncEnabled);
            this.listener.create();
            this.lastWidth = Math.max(1, this.graphics.getWidth());
            this.lastHeight = Math.max(1, this.graphics.getHeight());
            this.listener.resize(this.lastWidth, this.lastHeight);
            this.start();
        }
        catch (Throwable ex) {
            this.stopped();
            this.exception(ex);
        }
    }

    void render(boolean shouldRender) throws LWJGLException {
        if (!this.running) {
            return;
        }
        this.setGlobals();
        this.canvas.setCursor(this.cursor);
        int width = Math.max(1, this.graphics.getWidth());
        int height = Math.max(1, this.graphics.getHeight());
        if (this.lastWidth != width || this.lastHeight != height) {
            this.lastWidth = width;
            this.lastHeight = height;
            Gdx.gl.glViewport(0, 0, this.lastWidth, this.lastHeight);
            this.resize(width, height);
            this.listener.resize(width, height);
            shouldRender = true;
        }
        if (this.executeRunnables()) {
            shouldRender = true;
        }
        if (!this.running) {
            return;
        }
        shouldRender |= this.graphics.shouldRender();
        this.input.processEvents();
        if (this.audio != null) {
            this.audio.update();
        }
        if (shouldRender) {
            this.graphics.updateTime();
            ++this.graphics.frameId;
            this.listener.render();
            this.canvas.swapBuffers();
        }
        Display.sync(this.getFrameRate() * instanceCount);
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
        int n = frameRate = this.isActive() ? this.graphics.config.foregroundFPS : this.graphics.config.backgroundFPS;
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

    public boolean isActive() {
        Component root = SwingUtilities.getRoot(this.canvas);
        return root instanceof Frame ? ((Frame)root).isActive() : true;
    }

    protected void start() {
    }

    protected void resize(int width, int height) {
    }

    protected void stopped() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;
        this.setGlobals();
        Array<LifecycleListener> listeners = this.lifecycleListeners;
        if (this.canvas.isDisplayable()) {
            this.makeCurrent();
        } else {
            this.error("LwjglAWTCanvas", "OpenGL context destroyed before application listener has had a chance to dispose of textures.");
        }
        Array<LifecycleListener> array = listeners;
        synchronized (array) {
            for (LifecycleListener listener : listeners) {
                listener.pause();
                listener.dispose();
            }
        }
        this.listener.pause();
        this.listener.dispose();
        Gdx.app = null;
        Gdx.graphics = null;
        if (this.audio != null) {
            this.audio.dispose();
            Gdx.audio = null;
        }
        if (this.files != null) {
            Gdx.files = null;
        }
        if (this.net != null) {
            Gdx.net = null;
        }
        --instanceCount;
        this.stopped();
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
                LwjglAWTCanvas.this.stop();
                System.exit(-1);
            }
        });
    }

    public void makeCurrent() {
        try {
            this.canvas.makeCurrent();
            this.setGlobals();
        }
        catch (Throwable ex) {
            this.exception(ex);
        }
    }

    public boolean isCurrent() {
        try {
            return this.canvas.isCurrent();
        }
        catch (Throwable ex) {
            this.exception(ex);
            return false;
        }
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

    protected void exception(Throwable ex) {
        ex.printStackTrace();
        this.stop();
    }

    public static class NonSystemPaint
    extends PaintEvent {
        public NonSystemPaint(AWTGLCanvas canvas) {
            super(canvas, 801, new Rectangle(0, 0, 99999, 99999));
        }
    }
}

