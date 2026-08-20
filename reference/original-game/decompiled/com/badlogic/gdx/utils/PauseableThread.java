/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

public class PauseableThread
extends Thread {
    final Runnable runnable;
    boolean paused = false;
    boolean exit = false;

    public PauseableThread(Runnable runnable) {
        this.runnable = runnable;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        while (true) {
            PauseableThread pauseableThread = this;
            synchronized (pauseableThread) {
                try {
                    while (this.paused) {
                        this.wait();
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (this.exit) {
                return;
            }
            this.runnable.run();
        }
    }

    public void onPause() {
        this.paused = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onResume() {
        PauseableThread pauseableThread = this;
        synchronized (pauseableThread) {
            this.paused = false;
            this.notifyAll();
        }
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void stopThread() {
        this.exit = true;
        if (this.paused) {
            this.onResume();
        }
    }
}

