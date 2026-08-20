/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import javax.swing.JFrame;

public class LwjglFrame
extends JFrame {
    LwjglCanvas lwjglCanvas;
    private Thread shutdownHook;

    public LwjglFrame(ApplicationListener listener, String title, int width, int height) {
        super(title);
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        this.construct(listener, config);
    }

    public LwjglFrame(ApplicationListener listener, LwjglApplicationConfiguration config) {
        super(config.title);
        this.construct(listener, config);
    }

    private void construct(ApplicationListener listener, LwjglApplicationConfiguration config) {
        this.lwjglCanvas = new LwjglCanvas(listener, config){

            @Override
            protected void stopped() {
                LwjglFrame.this.dispose();
            }

            @Override
            protected void setTitle(String title) {
                LwjglFrame.this.setTitle(title);
            }

            @Override
            protected void setDisplayMode(int width, int height) {
                LwjglFrame.this.getContentPane().setPreferredSize(new Dimension(width, height));
                LwjglFrame.this.getContentPane().invalidate();
                LwjglFrame.this.pack();
                LwjglFrame.this.setLocationRelativeTo(null);
                LwjglFrame.this.updateSize(width, height);
            }

            @Override
            protected void resize(int width, int height) {
                LwjglFrame.this.updateSize(width, height);
            }

            @Override
            protected void start() {
                LwjglFrame.this.start();
            }

            @Override
            protected void exception(Throwable t) {
                LwjglFrame.this.exception(t);
            }

            @Override
            protected int getFrameRate() {
                int frameRate = LwjglFrame.this.getFrameRate();
                return frameRate == 0 ? super.getFrameRate() : frameRate;
            }
        };
        this.setHaltOnShutdown(true);
        this.setDefaultCloseOperation(3);
        this.getContentPane().setPreferredSize(new Dimension(config.width, config.height));
        this.initialize();
        this.pack();
        Point location = this.getLocation();
        if (location.x == 0 && location.y == 0) {
            this.setLocationRelativeTo(null);
        }
        this.lwjglCanvas.getCanvas().setSize(this.getSize());
        EventQueue.invokeLater(new Runnable(){

            @Override
            public void run() {
                LwjglFrame.this.addCanvas();
                LwjglFrame.this.setVisible(true);
                LwjglFrame.this.lwjglCanvas.getCanvas().requestFocus();
            }
        });
    }

    public void setHaltOnShutdown(boolean halt) {
        if (halt) {
            if (this.shutdownHook != null) {
                return;
            }
            this.shutdownHook = new Thread(){

                @Override
                public void run() {
                    Runtime.getRuntime().halt(0);
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        } else if (this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
    }

    protected int getFrameRate() {
        return 0;
    }

    protected void exception(Throwable ex) {
        ex.printStackTrace();
        this.lwjglCanvas.stop();
    }

    protected void initialize() {
    }

    protected void addCanvas() {
        this.getContentPane().add(this.lwjglCanvas.getCanvas());
    }

    protected void start() {
    }

    public void updateSize(int width, int height) {
    }

    public LwjglCanvas getLwjglCanvas() {
        return this.lwjglCanvas;
    }
}

