/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraInputController
extends GestureDetector {
    public int rotateButton = 0;
    public float rotateAngle = 360.0f;
    public int translateButton = 1;
    public float translateUnits = 10.0f;
    public int forwardButton = 2;
    public int activateKey = 0;
    protected boolean activatePressed;
    public boolean alwaysScroll = true;
    public float scrollFactor = -0.1f;
    public float pinchZoomFactor = 10.0f;
    public boolean autoUpdate = true;
    public Vector3 target = new Vector3();
    public boolean translateTarget = true;
    public boolean forwardTarget = true;
    public boolean scrollTarget = false;
    public int forwardKey = 51;
    protected boolean forwardPressed;
    public int backwardKey = 47;
    protected boolean backwardPressed;
    public int rotateRightKey = 29;
    protected boolean rotateRightPressed;
    public int rotateLeftKey = 32;
    protected boolean rotateLeftPressed;
    public Camera camera;
    protected int button = -1;
    private float startX;
    private float startY;
    private final Vector3 tmpV1 = new Vector3();
    private final Vector3 tmpV2 = new Vector3();
    protected final CameraGestureListener gestureListener;
    private int touched;
    private boolean multiTouch;

    protected CameraInputController(CameraGestureListener gestureListener, Camera camera) {
        super(gestureListener);
        this.gestureListener = gestureListener;
        this.gestureListener.controller = this;
        this.camera = camera;
    }

    public CameraInputController(Camera camera) {
        this(new CameraGestureListener(), camera);
    }

    public void update() {
        if (this.rotateRightPressed || this.rotateLeftPressed || this.forwardPressed || this.backwardPressed) {
            float delta = Gdx.graphics.getDeltaTime();
            if (this.rotateRightPressed) {
                this.camera.rotate(this.camera.up, -delta * this.rotateAngle);
            }
            if (this.rotateLeftPressed) {
                this.camera.rotate(this.camera.up, delta * this.rotateAngle);
            }
            if (this.forwardPressed) {
                this.camera.translate(this.tmpV1.set(this.camera.direction).scl(delta * this.translateUnits));
                if (this.forwardTarget) {
                    this.target.add(this.tmpV1);
                }
            }
            if (this.backwardPressed) {
                this.camera.translate(this.tmpV1.set(this.camera.direction).scl(-delta * this.translateUnits));
                if (this.forwardTarget) {
                    this.target.add(this.tmpV1);
                }
            }
            if (this.autoUpdate) {
                this.camera.update();
            }
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        this.touched |= 1 << pointer;
        boolean bl = this.multiTouch = !MathUtils.isPowerOfTwo(this.touched);
        if (this.multiTouch) {
            this.button = -1;
        } else if (this.button < 0 && (this.activateKey == 0 || this.activatePressed)) {
            this.startX = screenX;
            this.startY = screenY;
            this.button = button;
        }
        return super.touchDown(screenX, screenY, pointer, button) || this.activateKey == 0 || this.activatePressed;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        this.touched &= 0xFFFFFFFF ^ 1 << pointer;
        boolean bl = this.multiTouch = !MathUtils.isPowerOfTwo(this.touched);
        if (button == this.button) {
            this.button = -1;
        }
        return super.touchUp(screenX, screenY, pointer, button) || this.activatePressed;
    }

    protected boolean process(float deltaX, float deltaY, int button) {
        if (button == this.rotateButton) {
            this.tmpV1.set((Vector3)this.camera.direction).crs((Vector3)this.camera.up).y = 0.0f;
            this.camera.rotateAround(this.target, this.tmpV1.nor(), deltaY * this.rotateAngle);
            this.camera.rotateAround(this.target, Vector3.Y, deltaX * -this.rotateAngle);
        } else if (button == this.translateButton) {
            this.camera.translate(this.tmpV1.set(this.camera.direction).crs(this.camera.up).nor().scl(-deltaX * this.translateUnits));
            this.camera.translate(this.tmpV2.set(this.camera.up).scl(-deltaY * this.translateUnits));
            if (this.translateTarget) {
                this.target.add(this.tmpV1).add(this.tmpV2);
            }
        } else if (button == this.forwardButton) {
            this.camera.translate(this.tmpV1.set(this.camera.direction).scl(deltaY * this.translateUnits));
            if (this.forwardTarget) {
                this.target.add(this.tmpV1);
            }
        }
        if (this.autoUpdate) {
            this.camera.update();
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        boolean result = super.touchDragged(screenX, screenY, pointer);
        if (result || this.button < 0) {
            return result;
        }
        float deltaX = ((float)screenX - this.startX) / (float)Gdx.graphics.getWidth();
        float deltaY = (this.startY - (float)screenY) / (float)Gdx.graphics.getHeight();
        this.startX = screenX;
        this.startY = screenY;
        return this.process(deltaX, deltaY, this.button);
    }

    @Override
    public boolean scrolled(int amount) {
        return this.zoom((float)amount * this.scrollFactor * this.translateUnits);
    }

    public boolean zoom(float amount) {
        if (!this.alwaysScroll && this.activateKey != 0 && !this.activatePressed) {
            return false;
        }
        this.camera.translate(this.tmpV1.set(this.camera.direction).scl(amount));
        if (this.scrollTarget) {
            this.target.add(this.tmpV1);
        }
        if (this.autoUpdate) {
            this.camera.update();
        }
        return true;
    }

    protected boolean pinchZoom(float amount) {
        return this.zoom(this.pinchZoomFactor * amount);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == this.activateKey) {
            this.activatePressed = true;
        }
        if (keycode == this.forwardKey) {
            this.forwardPressed = true;
        } else if (keycode == this.backwardKey) {
            this.backwardPressed = true;
        } else if (keycode == this.rotateRightKey) {
            this.rotateRightPressed = true;
        } else if (keycode == this.rotateLeftKey) {
            this.rotateLeftPressed = true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == this.activateKey) {
            this.activatePressed = false;
            this.button = -1;
        }
        if (keycode == this.forwardKey) {
            this.forwardPressed = false;
        } else if (keycode == this.backwardKey) {
            this.backwardPressed = false;
        } else if (keycode == this.rotateRightKey) {
            this.rotateRightPressed = false;
        } else if (keycode == this.rotateLeftKey) {
            this.rotateLeftPressed = false;
        }
        return false;
    }

    protected static class CameraGestureListener
    extends GestureDetector.GestureAdapter {
        public CameraInputController controller;
        private float previousZoom;

        protected CameraGestureListener() {
        }

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            this.previousZoom = 0.0f;
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            float h;
            float newZoom = distance - initialDistance;
            float amount = newZoom - this.previousZoom;
            this.previousZoom = newZoom;
            float w = Gdx.graphics.getWidth();
            return this.controller.pinchZoom(amount / (w > (h = (float)Gdx.graphics.getHeight()) ? h : w));
        }

        @Override
        public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }
    }
}

