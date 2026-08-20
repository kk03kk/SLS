/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import java.awt.Dimension;
import javax.swing.JFrame;

public class LwjglAWTFrame
extends JFrame {
    final LwjglAWTCanvas lwjglAWTCanvas;
    private Thread shutdownHook;

    public LwjglAWTFrame(ApplicationListener listener, String title, int width, int height) {
        super(title);
        this.lwjglAWTCanvas = new LwjglAWTCanvas(listener){

            @Override
            protected void stopped() {
                LwjglAWTFrame.this.dispose();
            }

            @Override
            protected void setTitle(String title) {
                LwjglAWTFrame.this.setTitle(title);
            }

            @Override
            protected void setDisplayMode(int width, int height) {
                LwjglAWTFrame.this.getContentPane().setPreferredSize(new Dimension(width, height));
                LwjglAWTFrame.this.getContentPane().invalidate();
                LwjglAWTFrame.this.pack();
                LwjglAWTFrame.this.setLocationRelativeTo(null);
                LwjglAWTFrame.this.updateSize(width, height);
            }

            @Override
            protected void resize(int width, int height) {
                LwjglAWTFrame.this.updateSize(width, height);
            }

            @Override
            protected void start() {
                LwjglAWTFrame.this.start();
            }
        };
        this.getContentPane().add(this.lwjglAWTCanvas.getCanvas());
        this.setHaltOnShutdown(true);
        this.setDefaultCloseOperation(3);
        this.getContentPane().setPreferredSize(new Dimension(width, height));
        this.initialize();
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.lwjglAWTCanvas.getCanvas().requestFocus();
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

    protected void initialize() {
    }

    protected void start() {
    }

    public void updateSize(int width, int height) {
    }

    public LwjglAWTCanvas getLwjglAWTCanvas() {
        return this.lwjglAWTCanvas;
    }
}

