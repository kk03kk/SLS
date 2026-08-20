/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Stage
extends InputAdapter
implements Disposable {
    static boolean debug;
    private Viewport viewport;
    private final Batch batch;
    private boolean ownsBatch;
    private Group root;
    private final Vector2 tempCoords = new Vector2();
    private final Actor[] pointerOverActors = new Actor[20];
    private final boolean[] pointerTouched = new boolean[20];
    private final int[] pointerScreenX = new int[20];
    private final int[] pointerScreenY = new int[20];
    private int mouseScreenX;
    private int mouseScreenY;
    private Actor mouseOverActor;
    private Actor keyboardFocus;
    private Actor scrollFocus;
    private final SnapshotArray<TouchFocus> touchFocuses = new SnapshotArray(true, 4, TouchFocus.class);
    private boolean actionsRequestRendering = true;
    private ShapeRenderer debugShapes;
    private boolean debugInvisible;
    private boolean debugAll;
    private boolean debugUnderMouse;
    private boolean debugParentUnderMouse;
    private Table.Debug debugTableUnderMouse = Table.Debug.none;
    private final Color debugColor = new Color(0.0f, 1.0f, 0.0f, 0.85f);

    public Stage() {
        this(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), new SpriteBatch());
        this.ownsBatch = true;
    }

    public Stage(Viewport viewport) {
        this(viewport, new SpriteBatch());
        this.ownsBatch = true;
    }

    public Stage(Viewport viewport, Batch batch) {
        if (viewport == null) {
            throw new IllegalArgumentException("viewport cannot be null.");
        }
        if (batch == null) {
            throw new IllegalArgumentException("batch cannot be null.");
        }
        this.viewport = viewport;
        this.batch = batch;
        this.root = new Group();
        this.root.setStage(this);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void draw() {
        Camera camera = this.viewport.getCamera();
        camera.update();
        if (!this.root.isVisible()) {
            return;
        }
        Batch batch = this.batch;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        this.root.draw(batch, 1.0f);
        batch.end();
        if (debug) {
            this.drawDebug();
        }
    }

    private void drawDebug() {
        if (this.debugShapes == null) {
            this.debugShapes = new ShapeRenderer();
            this.debugShapes.setAutoShapeType(true);
        }
        if (this.debugUnderMouse || this.debugParentUnderMouse || this.debugTableUnderMouse != Table.Debug.none) {
            this.screenToStageCoordinates(this.tempCoords.set(Gdx.input.getX(), Gdx.input.getY()));
            Actor actor = this.hit(this.tempCoords.x, this.tempCoords.y, true);
            if (actor == null) {
                return;
            }
            if (this.debugParentUnderMouse && actor.parent != null) {
                actor = actor.parent;
            }
            if (this.debugTableUnderMouse == Table.Debug.none) {
                actor.setDebug(true);
            } else {
                while (actor != null && !(actor instanceof Table)) {
                    actor = actor.parent;
                }
                if (actor == null) {
                    return;
                }
                ((Table)actor).debug(this.debugTableUnderMouse);
            }
            if (this.debugAll && actor instanceof Group) {
                ((Group)actor).debugAll();
            }
            this.disableDebug(this.root, actor);
        } else if (this.debugAll) {
            this.root.debugAll();
        }
        Gdx.gl.glEnable(3042);
        this.debugShapes.setProjectionMatrix(this.viewport.getCamera().combined);
        this.debugShapes.begin();
        this.root.drawDebug(this.debugShapes);
        this.debugShapes.end();
    }

    private void disableDebug(Actor actor, Actor except) {
        if (actor == except) {
            return;
        }
        actor.setDebug(false);
        if (actor instanceof Group) {
            SnapshotArray<Actor> children = ((Group)actor).children;
            int n = children.size;
            for (int i = 0; i < n; ++i) {
                this.disableDebug((Actor)children.get(i), except);
            }
        }
    }

    public void act() {
        this.act(Math.min(Gdx.graphics.getDeltaTime(), 0.033333335f));
    }

    public void act(float delta) {
        int n = this.pointerOverActors.length;
        for (int pointer = 0; pointer < n; ++pointer) {
            Actor overLast = this.pointerOverActors[pointer];
            if (!this.pointerTouched[pointer]) {
                if (overLast == null) continue;
                this.pointerOverActors[pointer] = null;
                this.screenToStageCoordinates(this.tempCoords.set(this.pointerScreenX[pointer], this.pointerScreenY[pointer]));
                InputEvent event = Pools.obtain(InputEvent.class);
                event.setType(InputEvent.Type.exit);
                event.setStage(this);
                event.setStageX(this.tempCoords.x);
                event.setStageY(this.tempCoords.y);
                event.setRelatedActor(overLast);
                event.setPointer(pointer);
                overLast.fire(event);
                Pools.free(event);
                continue;
            }
            this.pointerOverActors[pointer] = this.fireEnterAndExit(overLast, this.pointerScreenX[pointer], this.pointerScreenY[pointer], pointer);
        }
        Application.ApplicationType type = Gdx.app.getType();
        if (type == Application.ApplicationType.Desktop || type == Application.ApplicationType.Applet || type == Application.ApplicationType.WebGL) {
            this.mouseOverActor = this.fireEnterAndExit(this.mouseOverActor, this.mouseScreenX, this.mouseScreenY, -1);
        }
        this.root.act(delta);
    }

    private Actor fireEnterAndExit(Actor overLast, int screenX, int screenY, int pointer) {
        InputEvent event;
        this.screenToStageCoordinates(this.tempCoords.set(screenX, screenY));
        Actor over = this.hit(this.tempCoords.x, this.tempCoords.y, true);
        if (over == overLast) {
            return overLast;
        }
        if (overLast != null) {
            event = Pools.obtain(InputEvent.class);
            event.setStage(this);
            event.setStageX(this.tempCoords.x);
            event.setStageY(this.tempCoords.y);
            event.setPointer(pointer);
            event.setType(InputEvent.Type.exit);
            event.setRelatedActor(over);
            overLast.fire(event);
            Pools.free(event);
        }
        if (over != null) {
            event = Pools.obtain(InputEvent.class);
            event.setStage(this);
            event.setStageX(this.tempCoords.x);
            event.setStageY(this.tempCoords.y);
            event.setPointer(pointer);
            event.setType(InputEvent.Type.enter);
            event.setRelatedActor(overLast);
            over.fire(event);
            Pools.free(event);
        }
        return over;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (screenX < this.viewport.getScreenX() || screenX >= this.viewport.getScreenX() + this.viewport.getScreenWidth()) {
            return false;
        }
        if (Gdx.graphics.getHeight() - screenY < this.viewport.getScreenY() || Gdx.graphics.getHeight() - screenY >= this.viewport.getScreenY() + this.viewport.getScreenHeight()) {
            return false;
        }
        this.pointerTouched[pointer] = true;
        this.pointerScreenX[pointer] = screenX;
        this.pointerScreenY[pointer] = screenY;
        this.screenToStageCoordinates(this.tempCoords.set(screenX, screenY));
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(InputEvent.Type.touchDown);
        event.setStage(this);
        event.setStageX(this.tempCoords.x);
        event.setStageY(this.tempCoords.y);
        event.setPointer(pointer);
        event.setButton(button);
        Actor target = this.hit(this.tempCoords.x, this.tempCoords.y, true);
        if (target == null) {
            if (this.root.getTouchable() == Touchable.enabled) {
                this.root.fire(event);
            }
        } else {
            target.fire(event);
        }
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        this.pointerScreenX[pointer] = screenX;
        this.pointerScreenY[pointer] = screenY;
        this.mouseScreenX = screenX;
        this.mouseScreenY = screenY;
        if (this.touchFocuses.size == 0) {
            return false;
        }
        this.screenToStageCoordinates(this.tempCoords.set(screenX, screenY));
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(InputEvent.Type.touchDragged);
        event.setStage(this);
        event.setStageX(this.tempCoords.x);
        event.setStageY(this.tempCoords.y);
        event.setPointer(pointer);
        SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] focuses = touchFocuses.begin();
        int n = touchFocuses.size;
        for (int i = 0; i < n; ++i) {
            TouchFocus focus = focuses[i];
            if (focus.pointer != pointer || !touchFocuses.contains(focus, true)) continue;
            event.setTarget(focus.target);
            event.setListenerActor(focus.listenerActor);
            if (!focus.listener.handle(event)) continue;
            event.handle();
        }
        touchFocuses.end();
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        this.pointerTouched[pointer] = false;
        this.pointerScreenX[pointer] = screenX;
        this.pointerScreenY[pointer] = screenY;
        if (this.touchFocuses.size == 0) {
            return false;
        }
        this.screenToStageCoordinates(this.tempCoords.set(screenX, screenY));
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(InputEvent.Type.touchUp);
        event.setStage(this);
        event.setStageX(this.tempCoords.x);
        event.setStageY(this.tempCoords.y);
        event.setPointer(pointer);
        event.setButton(button);
        SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] focuses = touchFocuses.begin();
        int n = touchFocuses.size;
        for (int i = 0; i < n; ++i) {
            TouchFocus focus = focuses[i];
            if (focus.pointer != pointer || focus.button != button || !touchFocuses.removeValue(focus, true)) continue;
            event.setTarget(focus.target);
            event.setListenerActor(focus.listenerActor);
            if (focus.listener.handle(event)) {
                event.handle();
            }
            Pools.free(focus);
        }
        touchFocuses.end();
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (screenX < this.viewport.getScreenX() || screenX >= this.viewport.getScreenX() + this.viewport.getScreenWidth()) {
            return false;
        }
        if (Gdx.graphics.getHeight() - screenY < this.viewport.getScreenY() || Gdx.graphics.getHeight() - screenY >= this.viewport.getScreenY() + this.viewport.getScreenHeight()) {
            return false;
        }
        this.mouseScreenX = screenX;
        this.mouseScreenY = screenY;
        this.screenToStageCoordinates(this.tempCoords.set(screenX, screenY));
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.mouseMoved);
        event.setStageX(this.tempCoords.x);
        event.setStageY(this.tempCoords.y);
        Actor target = this.hit(this.tempCoords.x, this.tempCoords.y, true);
        if (target == null) {
            target = this.root;
        }
        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean scrolled(int amount) {
        Actor target = this.scrollFocus == null ? this.root : this.scrollFocus;
        this.screenToStageCoordinates(this.tempCoords.set(this.mouseScreenX, this.mouseScreenY));
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.scrolled);
        event.setScrollAmount(amount);
        event.setStageX(this.tempCoords.x);
        event.setStageY(this.tempCoords.y);
        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean keyDown(int keyCode) {
        Actor target = this.keyboardFocus == null ? this.root : this.keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.keyDown);
        event.setKeyCode(keyCode);
        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean keyUp(int keyCode) {
        Actor target = this.keyboardFocus == null ? this.root : this.keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.keyUp);
        event.setKeyCode(keyCode);
        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    @Override
    public boolean keyTyped(char character) {
        Actor target = this.keyboardFocus == null ? this.root : this.keyboardFocus;
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.keyTyped);
        event.setCharacter(character);
        target.fire(event);
        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    public void addTouchFocus(EventListener listener, Actor listenerActor, Actor target, int pointer, int button) {
        TouchFocus focus = Pools.obtain(TouchFocus.class);
        focus.listenerActor = listenerActor;
        focus.target = target;
        focus.listener = listener;
        focus.pointer = pointer;
        focus.button = button;
        this.touchFocuses.add(focus);
    }

    public void removeTouchFocus(EventListener listener, Actor listenerActor, Actor target, int pointer, int button) {
        SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
        for (int i = touchFocuses.size - 1; i >= 0; --i) {
            TouchFocus focus = (TouchFocus)touchFocuses.get(i);
            if (focus.listener != listener || focus.listenerActor != listenerActor || focus.target != target || focus.pointer != pointer || focus.button != button) continue;
            touchFocuses.removeIndex(i);
            Pools.free(focus);
        }
    }

    public void cancelTouchFocus(Actor actor) {
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.touchUp);
        event.setStageX(-2.1474836E9f);
        event.setStageY(-2.1474836E9f);
        SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] items = touchFocuses.begin();
        int n = touchFocuses.size;
        for (int i = 0; i < n; ++i) {
            TouchFocus focus = items[i];
            if (focus.listenerActor != actor || !touchFocuses.removeValue(focus, true)) continue;
            event.setTarget(focus.target);
            event.setListenerActor(focus.listenerActor);
            event.setPointer(focus.pointer);
            event.setButton(focus.button);
            focus.listener.handle(event);
        }
        touchFocuses.end();
        Pools.free(event);
    }

    public void cancelTouchFocus() {
        this.cancelTouchFocusExcept(null, null);
    }

    public void cancelTouchFocusExcept(EventListener exceptListener, Actor exceptActor) {
        InputEvent event = Pools.obtain(InputEvent.class);
        event.setStage(this);
        event.setType(InputEvent.Type.touchUp);
        event.setStageX(-2.1474836E9f);
        event.setStageY(-2.1474836E9f);
        SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
        TouchFocus[] items = touchFocuses.begin();
        int n = touchFocuses.size;
        for (int i = 0; i < n; ++i) {
            TouchFocus focus = items[i];
            if (focus.listener == exceptListener && focus.listenerActor == exceptActor || !touchFocuses.removeValue(focus, true)) continue;
            event.setTarget(focus.target);
            event.setListenerActor(focus.listenerActor);
            event.setPointer(focus.pointer);
            event.setButton(focus.button);
            focus.listener.handle(event);
        }
        touchFocuses.end();
        Pools.free(event);
    }

    public void addActor(Actor actor) {
        this.root.addActor(actor);
    }

    public void addAction(Action action) {
        this.root.addAction(action);
    }

    public Array<Actor> getActors() {
        return this.root.children;
    }

    public boolean addListener(EventListener listener) {
        return this.root.addListener(listener);
    }

    public boolean removeListener(EventListener listener) {
        return this.root.removeListener(listener);
    }

    public boolean addCaptureListener(EventListener listener) {
        return this.root.addCaptureListener(listener);
    }

    public boolean removeCaptureListener(EventListener listener) {
        return this.root.removeCaptureListener(listener);
    }

    public void clear() {
        this.unfocusAll();
        this.root.clear();
    }

    public void unfocusAll() {
        this.setScrollFocus(null);
        this.setKeyboardFocus(null);
        this.cancelTouchFocus();
    }

    public void unfocus(Actor actor) {
        this.cancelTouchFocus(actor);
        if (this.scrollFocus != null && this.scrollFocus.isDescendantOf(actor)) {
            this.setScrollFocus(null);
        }
        if (this.keyboardFocus != null && this.keyboardFocus.isDescendantOf(actor)) {
            this.setKeyboardFocus(null);
        }
    }

    public boolean setKeyboardFocus(Actor actor) {
        boolean success;
        if (this.keyboardFocus == actor) {
            return true;
        }
        FocusListener.FocusEvent event = Pools.obtain(FocusListener.FocusEvent.class);
        event.setStage(this);
        event.setType(FocusListener.FocusEvent.Type.keyboard);
        Actor oldKeyboardFocus = this.keyboardFocus;
        if (oldKeyboardFocus != null) {
            event.setFocused(false);
            event.setRelatedActor(actor);
            oldKeyboardFocus.fire(event);
        }
        boolean bl = success = !event.isCancelled();
        if (success) {
            this.keyboardFocus = actor;
            if (actor != null) {
                event.setFocused(true);
                event.setRelatedActor(oldKeyboardFocus);
                actor.fire(event);
                boolean bl2 = success = !event.isCancelled();
                if (!success) {
                    this.setKeyboardFocus(oldKeyboardFocus);
                }
            }
        }
        Pools.free(event);
        return success;
    }

    public Actor getKeyboardFocus() {
        return this.keyboardFocus;
    }

    public boolean setScrollFocus(Actor actor) {
        boolean success;
        if (this.scrollFocus == actor) {
            return true;
        }
        FocusListener.FocusEvent event = Pools.obtain(FocusListener.FocusEvent.class);
        event.setStage(this);
        event.setType(FocusListener.FocusEvent.Type.scroll);
        Actor oldScrollFocus = this.scrollFocus;
        if (oldScrollFocus != null) {
            event.setFocused(false);
            event.setRelatedActor(actor);
            oldScrollFocus.fire(event);
        }
        boolean bl = success = !event.isCancelled();
        if (success) {
            this.scrollFocus = actor;
            if (actor != null) {
                event.setFocused(true);
                event.setRelatedActor(oldScrollFocus);
                actor.fire(event);
                boolean bl2 = success = !event.isCancelled();
                if (!success) {
                    this.setScrollFocus(oldScrollFocus);
                }
            }
        }
        Pools.free(event);
        return success;
    }

    public Actor getScrollFocus() {
        return this.scrollFocus;
    }

    public Batch getBatch() {
        return this.batch;
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    public float getWidth() {
        return this.viewport.getWorldWidth();
    }

    public float getHeight() {
        return this.viewport.getWorldHeight();
    }

    public Camera getCamera() {
        return this.viewport.getCamera();
    }

    public Group getRoot() {
        return this.root;
    }

    public void setRoot(Group root) {
        this.root = root;
    }

    public Actor hit(float stageX, float stageY, boolean touchable) {
        this.root.parentToLocalCoordinates(this.tempCoords.set(stageX, stageY));
        return this.root.hit(this.tempCoords.x, this.tempCoords.y, touchable);
    }

    public Vector2 screenToStageCoordinates(Vector2 screenCoords) {
        this.viewport.unproject(screenCoords);
        return screenCoords;
    }

    public Vector2 stageToScreenCoordinates(Vector2 stageCoords) {
        this.viewport.project(stageCoords);
        stageCoords.y = (float)this.viewport.getScreenHeight() - stageCoords.y;
        return stageCoords;
    }

    public Vector2 toScreenCoordinates(Vector2 coords, Matrix4 transformMatrix) {
        return this.viewport.toScreenCoordinates(coords, transformMatrix);
    }

    public void calculateScissors(Rectangle localRect, Rectangle scissorRect) {
        this.viewport.calculateScissors(this.batch.getTransformMatrix(), localRect, scissorRect);
        Matrix4 transformMatrix = this.debugShapes != null && this.debugShapes.isDrawing() ? this.debugShapes.getTransformMatrix() : this.batch.getTransformMatrix();
        this.viewport.calculateScissors(transformMatrix, localRect, scissorRect);
    }

    public void setActionsRequestRendering(boolean actionsRequestRendering) {
        this.actionsRequestRendering = actionsRequestRendering;
    }

    public boolean getActionsRequestRendering() {
        return this.actionsRequestRendering;
    }

    public Color getDebugColor() {
        return this.debugColor;
    }

    public void setDebugInvisible(boolean debugInvisible) {
        this.debugInvisible = debugInvisible;
    }

    public void setDebugAll(boolean debugAll) {
        if (this.debugAll == debugAll) {
            return;
        }
        this.debugAll = debugAll;
        if (debugAll) {
            debug = true;
        } else {
            this.root.setDebug(false, true);
        }
    }

    public void setDebugUnderMouse(boolean debugUnderMouse) {
        if (this.debugUnderMouse == debugUnderMouse) {
            return;
        }
        this.debugUnderMouse = debugUnderMouse;
        if (debugUnderMouse) {
            debug = true;
        } else {
            this.root.setDebug(false, true);
        }
    }

    public void setDebugParentUnderMouse(boolean debugParentUnderMouse) {
        if (this.debugParentUnderMouse == debugParentUnderMouse) {
            return;
        }
        this.debugParentUnderMouse = debugParentUnderMouse;
        if (debugParentUnderMouse) {
            debug = true;
        } else {
            this.root.setDebug(false, true);
        }
    }

    public void setDebugTableUnderMouse(Table.Debug debugTableUnderMouse) {
        if (debugTableUnderMouse == null) {
            debugTableUnderMouse = Table.Debug.none;
        }
        if (this.debugTableUnderMouse == debugTableUnderMouse) {
            return;
        }
        this.debugTableUnderMouse = debugTableUnderMouse;
        if (debugTableUnderMouse != Table.Debug.none) {
            debug = true;
        } else {
            this.root.setDebug(false, true);
        }
    }

    public void setDebugTableUnderMouse(boolean debugTableUnderMouse) {
        this.setDebugTableUnderMouse(debugTableUnderMouse ? Table.Debug.all : Table.Debug.none);
    }

    @Override
    public void dispose() {
        this.clear();
        if (this.ownsBatch) {
            this.batch.dispose();
        }
    }

    public static final class TouchFocus
    implements Pool.Poolable {
        EventListener listener;
        Actor listenerActor;
        Actor target;
        int pointer;
        int button;

        @Override
        public void reset() {
            this.listenerActor = null;
            this.listener = null;
            this.target = null;
        }
    }
}

