/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import java.io.DataOutputStream;
import java.net.Socket;

public class RemoteSender
implements InputProcessor {
    private DataOutputStream out;
    private boolean connected = false;
    public static final int KEY_DOWN = 0;
    public static final int KEY_UP = 1;
    public static final int KEY_TYPED = 2;
    public static final int TOUCH_DOWN = 3;
    public static final int TOUCH_UP = 4;
    public static final int TOUCH_DRAGGED = 5;
    public static final int ACCEL = 6;
    public static final int COMPASS = 7;
    public static final int SIZE = 8;
    public static final int GYRO = 9;

    public RemoteSender(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(3000);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.out.writeBoolean(Gdx.input.isPeripheralAvailable(Input.Peripheral.MultitouchScreen));
            this.connected = true;
            Gdx.input.setInputProcessor(this);
        }
        catch (Exception e) {
            Gdx.app.log("RemoteSender", "couldn't connect to " + ip + ":" + port);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendUpdate() {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return;
            }
        }
        try {
            this.out.writeInt(6);
            this.out.writeFloat(Gdx.input.getAccelerometerX());
            this.out.writeFloat(Gdx.input.getAccelerometerY());
            this.out.writeFloat(Gdx.input.getAccelerometerZ());
            this.out.writeInt(7);
            this.out.writeFloat(Gdx.input.getAzimuth());
            this.out.writeFloat(Gdx.input.getPitch());
            this.out.writeFloat(Gdx.input.getRoll());
            this.out.writeInt(8);
            this.out.writeFloat(Gdx.graphics.getWidth());
            this.out.writeFloat(Gdx.graphics.getHeight());
            this.out.writeInt(9);
            this.out.writeFloat(Gdx.input.getGyroscopeX());
            this.out.writeFloat(Gdx.input.getGyroscopeY());
            this.out.writeFloat(Gdx.input.getGyroscopeZ());
        }
        catch (Throwable t) {
            this.out = null;
            this.connected = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean keyDown(int keycode) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(0);
            this.out.writeInt(keycode);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean keyUp(int keycode) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(1);
            this.out.writeInt(keycode);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean keyTyped(char character) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(2);
            this.out.writeChar(character);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(3);
            this.out.writeInt(x);
            this.out.writeInt(y);
            this.out.writeInt(pointer);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(4);
            this.out.writeInt(x);
            this.out.writeInt(y);
            this.out.writeInt(pointer);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            if (!this.connected) {
                return false;
            }
        }
        try {
            this.out.writeInt(5);
            this.out.writeInt(x);
            this.out.writeInt(y);
            this.out.writeInt(pointer);
        }
        catch (Throwable t) {
            RemoteSender remoteSender2 = this;
            synchronized (remoteSender2) {
                this.connected = false;
            }
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isConnected() {
        RemoteSender remoteSender = this;
        synchronized (remoteSender) {
            return this.connected;
        }
    }
}

