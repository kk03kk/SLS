/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.TimeUtils;

public class InputEventQueue
implements InputProcessor {
    private static final int KEY_DOWN = 0;
    private static final int KEY_UP = 1;
    private static final int KEY_TYPED = 2;
    private static final int TOUCH_DOWN = 3;
    private static final int TOUCH_UP = 4;
    private static final int TOUCH_DRAGGED = 5;
    private static final int MOUSE_MOVED = 6;
    private static final int SCROLLED = 7;
    private InputProcessor processor;
    private final IntArray queue = new IntArray();
    private final IntArray processingQueue = new IntArray();
    private long currentEventTime;

    public InputEventQueue() {
    }

    public InputEventQueue(InputProcessor processor) {
        this.processor = processor;
    }

    public void setProcessor(InputProcessor processor) {
        this.processor = processor;
    }

    public InputProcessor getProcessor() {
        return this.processor;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void drain() {
        IntArray q = this.processingQueue;
        InputEventQueue inputEventQueue = this;
        synchronized (inputEventQueue) {
            if (this.processor == null) {
                this.queue.clear();
                return;
            }
            q.addAll(this.queue);
            this.queue.clear();
        }
        InputProcessor localProcessor = this.processor;
        int i = 0;
        int n = q.size;
        while (i < n) {
            this.currentEventTime = (long)q.get(i++) << 32 | (long)q.get(i++) & 0xFFFFFFFFL;
            switch (q.get(i++)) {
                case 0: {
                    localProcessor.keyDown(q.get(i++));
                    break;
                }
                case 1: {
                    localProcessor.keyUp(q.get(i++));
                    break;
                }
                case 2: {
                    localProcessor.keyTyped((char)q.get(i++));
                    break;
                }
                case 3: {
                    localProcessor.touchDown(q.get(i++), q.get(i++), q.get(i++), q.get(i++));
                    break;
                }
                case 4: {
                    localProcessor.touchUp(q.get(i++), q.get(i++), q.get(i++), q.get(i++));
                    break;
                }
                case 5: {
                    localProcessor.touchDragged(q.get(i++), q.get(i++), q.get(i++));
                    break;
                }
                case 6: {
                    localProcessor.mouseMoved(q.get(i++), q.get(i++));
                    break;
                }
                case 7: {
                    localProcessor.scrolled(q.get(i++));
                }
            }
        }
        q.clear();
    }

    private void queueTime() {
        long time = TimeUtils.nanoTime();
        this.queue.add((int)(time >> 32));
        this.queue.add((int)time);
    }

    @Override
    public synchronized boolean keyDown(int keycode) {
        this.queueTime();
        this.queue.add(0);
        this.queue.add(keycode);
        return false;
    }

    @Override
    public synchronized boolean keyUp(int keycode) {
        this.queueTime();
        this.queue.add(1);
        this.queue.add(keycode);
        return false;
    }

    @Override
    public synchronized boolean keyTyped(char character) {
        this.queueTime();
        this.queue.add(2);
        this.queue.add(character);
        return false;
    }

    @Override
    public synchronized boolean touchDown(int screenX, int screenY, int pointer, int button) {
        this.queueTime();
        this.queue.add(3);
        this.queue.add(screenX);
        this.queue.add(screenY);
        this.queue.add(pointer);
        this.queue.add(button);
        return false;
    }

    @Override
    public synchronized boolean touchUp(int screenX, int screenY, int pointer, int button) {
        this.queueTime();
        this.queue.add(4);
        this.queue.add(screenX);
        this.queue.add(screenY);
        this.queue.add(pointer);
        this.queue.add(button);
        return false;
    }

    @Override
    public synchronized boolean touchDragged(int screenX, int screenY, int pointer) {
        this.queueTime();
        this.queue.add(5);
        this.queue.add(screenX);
        this.queue.add(screenY);
        this.queue.add(pointer);
        return false;
    }

    @Override
    public synchronized boolean mouseMoved(int screenX, int screenY) {
        this.queueTime();
        this.queue.add(6);
        this.queue.add(screenX);
        this.queue.add(screenY);
        return false;
    }

    @Override
    public synchronized boolean scrolled(int amount) {
        this.queueTime();
        this.queue.add(7);
        this.queue.add(amount);
        return false;
    }

    public long getCurrentEventTime() {
        return this.currentEventTime;
    }
}

