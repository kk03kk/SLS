/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;

public class LwjglApplet
extends Applet {
    final Canvas canvas;
    LwjglApplication app;

    public LwjglApplet(final ApplicationListener listener, final LwjglApplicationConfiguration config) {
        LwjglNativesLoader.load = false;
        this.canvas = new Canvas(){

            @Override
            public final void addNotify() {
                super.addNotify();
                LwjglApplet.this.app = new LwjglAppletApplication(listener, LwjglApplet.this.canvas, config);
            }

            @Override
            public final void removeNotify() {
                LwjglApplet.this.app.stop();
                super.removeNotify();
            }
        };
        this.setLayout(new BorderLayout());
        this.canvas.setIgnoreRepaint(true);
        this.add(this.canvas);
        this.canvas.setFocusable(true);
        this.canvas.requestFocus();
    }

    public LwjglApplet(final ApplicationListener listener) {
        LwjglNativesLoader.load = false;
        this.canvas = new Canvas(){

            @Override
            public final void addNotify() {
                super.addNotify();
                LwjglApplet.this.app = new LwjglAppletApplication(listener, LwjglApplet.this.canvas);
            }

            @Override
            public final void removeNotify() {
                LwjglApplet.this.app.stop();
                super.removeNotify();
            }
        };
        this.setLayout(new BorderLayout());
        this.canvas.setIgnoreRepaint(true);
        this.add(this.canvas);
        this.canvas.setFocusable(true);
        this.canvas.requestFocus();
    }

    @Override
    public void destroy() {
        this.remove(this.canvas);
        super.destroy();
    }

    class LwjglAppletApplication
    extends LwjglApplication {
        public LwjglAppletApplication(ApplicationListener listener, Canvas canvas) {
            super(listener, canvas);
        }

        public LwjglAppletApplication(ApplicationListener listener, Canvas canvas, LwjglApplicationConfiguration config) {
            super(listener, config, canvas);
        }

        @Override
        public Application.ApplicationType getType() {
            return Application.ApplicationType.Applet;
        }
    }
}

