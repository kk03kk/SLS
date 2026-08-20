/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class Actor {
    private Stage stage;
    Group parent;
    private final DelayedRemovalArray<EventListener> listeners = new DelayedRemovalArray(0);
    private final DelayedRemovalArray<EventListener> captureListeners = new DelayedRemovalArray(0);
    private final Array<Action> actions = new Array(0);
    private String name;
    private Touchable touchable = Touchable.enabled;
    private boolean visible = true;
    private boolean debug;
    float x;
    float y;
    float width;
    float height;
    float originX;
    float originY;
    float scaleX = 1.0f;
    float scaleY = 1.0f;
    float rotation;
    final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private Object userObject;

    public void draw(Batch batch, float parentAlpha) {
    }

    public void act(float delta) {
        Array<Action> actions = this.actions;
        if (actions.size > 0) {
            if (this.stage != null && this.stage.getActionsRequestRendering()) {
                Gdx.graphics.requestRendering();
            }
            for (int i = 0; i < actions.size; ++i) {
                int actionIndex;
                Action action = actions.get(i);
                if (!action.act(delta) || i >= actions.size) continue;
                Action current = actions.get(i);
                int n = actionIndex = current == action ? i : actions.indexOf(action, true);
                if (actionIndex == -1) continue;
                actions.removeIndex(actionIndex);
                action.setActor(null);
                --i;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean fire(Event event) {
        if (event.getStage() == null) {
            event.setStage(this.getStage());
        }
        event.setTarget(this);
        Array ancestors = Pools.obtain(Array.class);
        Group parent = this.parent;
        while (parent != null) {
            ancestors.add(parent);
            parent = parent.parent;
        }
        try {
            int i;
            T[] ancestorsArray = ancestors.items;
            for (i = ancestors.size - 1; i >= 0; --i) {
                Group currentTarget = (Group)ancestorsArray[i];
                currentTarget.notify(event, true);
                if (!event.isStopped()) continue;
                boolean bl = event.isCancelled();
                return bl;
            }
            this.notify(event, true);
            if (event.isStopped()) {
                i = event.isCancelled() ? 1 : 0;
                return i != 0;
            }
            this.notify(event, false);
            if (!event.getBubbles()) {
                i = event.isCancelled();
                return i != 0;
            }
            if (event.isStopped()) {
                i = event.isCancelled();
                return i != 0;
            }
            int n = ancestors.size;
            for (i = 0; i < n; ++i) {
                ((Group)ancestorsArray[i]).notify(event, false);
                if (!event.isStopped()) continue;
                boolean bl = event.isCancelled();
                return bl;
            }
            boolean bl = event.isCancelled();
            return bl;
        }
        finally {
            ancestors.clear();
            Pools.free(ancestors);
        }
    }

    public boolean notify(Event event, boolean capture) {
        DelayedRemovalArray<EventListener> listeners;
        if (event.getTarget() == null) {
            throw new IllegalArgumentException("The event target cannot be null.");
        }
        DelayedRemovalArray<EventListener> delayedRemovalArray = listeners = capture ? this.captureListeners : this.listeners;
        if (listeners.size == 0) {
            return event.isCancelled();
        }
        event.setListenerActor(this);
        event.setCapture(capture);
        if (event.getStage() == null) {
            event.setStage(this.stage);
        }
        listeners.begin();
        int n = listeners.size;
        for (int i = 0; i < n; ++i) {
            InputEvent inputEvent;
            EventListener listener = (EventListener)listeners.get(i);
            if (!listener.handle(event)) continue;
            event.handle();
            if (!(event instanceof InputEvent) || (inputEvent = (InputEvent)event).getType() != InputEvent.Type.touchDown) continue;
            event.getStage().addTouchFocus(listener, this, inputEvent.getTarget(), inputEvent.getPointer(), inputEvent.getButton());
        }
        listeners.end();
        return event.isCancelled();
    }

    public Actor hit(float x, float y, boolean touchable) {
        if (touchable && this.touchable != Touchable.enabled) {
            return null;
        }
        return x >= 0.0f && x < this.width && y >= 0.0f && y < this.height ? this : null;
    }

    public boolean remove() {
        if (this.parent != null) {
            return this.parent.removeActor(this, true);
        }
        return false;
    }

    public boolean addListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        }
        if (!this.listeners.contains(listener, true)) {
            this.listeners.add(listener);
            return true;
        }
        return false;
    }

    public boolean removeListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        }
        return this.listeners.removeValue(listener, true);
    }

    public Array<EventListener> getListeners() {
        return this.listeners;
    }

    public boolean addCaptureListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        }
        if (!this.captureListeners.contains(listener, true)) {
            this.captureListeners.add(listener);
        }
        return true;
    }

    public boolean removeCaptureListener(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        }
        return this.captureListeners.removeValue(listener, true);
    }

    public Array<EventListener> getCaptureListeners() {
        return this.captureListeners;
    }

    public void addAction(Action action) {
        action.setActor(this);
        this.actions.add(action);
        if (this.stage != null && this.stage.getActionsRequestRendering()) {
            Gdx.graphics.requestRendering();
        }
    }

    public void removeAction(Action action) {
        if (this.actions.removeValue(action, true)) {
            action.setActor(null);
        }
    }

    public Array<Action> getActions() {
        return this.actions;
    }

    public boolean hasActions() {
        return this.actions.size > 0;
    }

    public void clearActions() {
        for (int i = this.actions.size - 1; i >= 0; --i) {
            this.actions.get(i).setActor(null);
        }
        this.actions.clear();
    }

    public void clearListeners() {
        this.listeners.clear();
        this.captureListeners.clear();
    }

    public void clear() {
        this.clearActions();
        this.clearListeners();
    }

    public Stage getStage() {
        return this.stage;
    }

    protected void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isDescendantOf(Actor actor) {
        if (actor == null) {
            throw new IllegalArgumentException("actor cannot be null.");
        }
        Actor parent = this;
        while (parent != null) {
            if (parent == actor) {
                return true;
            }
            parent = parent.parent;
        }
        return false;
    }

    public boolean isAscendantOf(Actor actor) {
        if (actor == null) {
            throw new IllegalArgumentException("actor cannot be null.");
        }
        while (actor != null) {
            if (actor == this) {
                return true;
            }
            actor = actor.parent;
        }
        return false;
    }

    public <T extends Actor> T firstAscendant(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("actor cannot be null.");
        }
        Actor actor = this;
        do {
            if (!ClassReflection.isInstance(type, actor)) continue;
            return (T)actor;
        } while ((actor = actor.getParent()) != null);
        return null;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    public Group getParent() {
        return this.parent;
    }

    protected void setParent(Group parent) {
        this.parent = parent;
    }

    public boolean isTouchable() {
        return this.touchable == Touchable.enabled;
    }

    public Touchable getTouchable() {
        return this.touchable;
    }

    public void setTouchable(Touchable touchable) {
        this.touchable = touchable;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Object getUserObject() {
        return this.userObject;
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public float getX() {
        return this.x;
    }

    public float getX(int alignment) {
        float x = this.x;
        if ((alignment & 0x10) != 0) {
            x += this.width;
        } else if ((alignment & 8) == 0) {
            x += this.width / 2.0f;
        }
        return x;
    }

    public void setX(float x) {
        if (this.x != x) {
            this.x = x;
            this.positionChanged();
        }
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        if (this.y != y) {
            this.y = y;
            this.positionChanged();
        }
    }

    public float getY(int alignment) {
        float y = this.y;
        if ((alignment & 2) != 0) {
            y += this.height;
        } else if ((alignment & 4) == 0) {
            y += this.height / 2.0f;
        }
        return y;
    }

    public void setPosition(float x, float y) {
        if (this.x != x || this.y != y) {
            this.x = x;
            this.y = y;
            this.positionChanged();
        }
    }

    public void setPosition(float x, float y, int alignment) {
        if ((alignment & 0x10) != 0) {
            x -= this.width;
        } else if ((alignment & 8) == 0) {
            x -= this.width / 2.0f;
        }
        if ((alignment & 2) != 0) {
            y -= this.height;
        } else if ((alignment & 4) == 0) {
            y -= this.height / 2.0f;
        }
        if (this.x != x || this.y != y) {
            this.x = x;
            this.y = y;
            this.positionChanged();
        }
    }

    public void moveBy(float x, float y) {
        if (x != 0.0f || y != 0.0f) {
            this.x += x;
            this.y += y;
            this.positionChanged();
        }
    }

    public float getWidth() {
        return this.width;
    }

    public void setWidth(float width) {
        if (this.width != width) {
            this.width = width;
            this.sizeChanged();
        }
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        if (this.height != height) {
            this.height = height;
            this.sizeChanged();
        }
    }

    public float getTop() {
        return this.y + this.height;
    }

    public float getRight() {
        return this.x + this.width;
    }

    protected void positionChanged() {
    }

    protected void sizeChanged() {
    }

    protected void rotationChanged() {
    }

    public void setSize(float width, float height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            this.sizeChanged();
        }
    }

    public void sizeBy(float size) {
        if (size != 0.0f) {
            this.width += size;
            this.height += size;
            this.sizeChanged();
        }
    }

    public void sizeBy(float width, float height) {
        if (width != 0.0f || height != 0.0f) {
            this.width += width;
            this.height += height;
            this.sizeChanged();
        }
    }

    public void setBounds(float x, float y, float width, float height) {
        if (this.x != x || this.y != y) {
            this.x = x;
            this.y = y;
            this.positionChanged();
        }
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            this.sizeChanged();
        }
    }

    public float getOriginX() {
        return this.originX;
    }

    public void setOriginX(float originX) {
        this.originX = originX;
    }

    public float getOriginY() {
        return this.originY;
    }

    public void setOriginY(float originY) {
        this.originY = originY;
    }

    public void setOrigin(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
    }

    public void setOrigin(int alignment) {
        this.originX = (alignment & 8) != 0 ? 0.0f : ((alignment & 0x10) != 0 ? this.width : this.width / 2.0f);
        this.originY = (alignment & 4) != 0 ? 0.0f : ((alignment & 2) != 0 ? this.height : this.height / 2.0f);
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public void setScale(float scaleXY) {
        this.scaleX = scaleXY;
        this.scaleY = scaleXY;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void scaleBy(float scale) {
        this.scaleX += scale;
        this.scaleY += scale;
    }

    public void scaleBy(float scaleX, float scaleY) {
        this.scaleX += scaleX;
        this.scaleY += scaleY;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float degrees) {
        if (this.rotation != degrees) {
            this.rotation = degrees;
            this.rotationChanged();
        }
    }

    public void rotateBy(float amountInDegrees) {
        if (amountInDegrees != 0.0f) {
            this.rotation += amountInDegrees;
            this.rotationChanged();
        }
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
    }

    public Color getColor() {
        return this.color;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void toFront() {
        this.setZIndex(Integer.MAX_VALUE);
    }

    public void toBack() {
        this.setZIndex(0);
    }

    public void setZIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("ZIndex cannot be < 0.");
        }
        Group parent = this.parent;
        if (parent == null) {
            return;
        }
        SnapshotArray<Actor> children = parent.children;
        if (children.size == 1) {
            return;
        }
        if (children.get(index = Math.min(index, children.size - 1)) == this) {
            return;
        }
        if (!((Array)children).removeValue(this, true)) {
            return;
        }
        ((Array)children).insert(index, this);
    }

    public int getZIndex() {
        Group parent = this.parent;
        if (parent == null) {
            return -1;
        }
        return parent.children.indexOf(this, true);
    }

    public boolean clipBegin() {
        return this.clipBegin(this.x, this.y, this.width, this.height);
    }

    public boolean clipBegin(float x, float y, float width, float height) {
        if (width <= 0.0f || height <= 0.0f) {
            return false;
        }
        Rectangle tableBounds = Rectangle.tmp;
        tableBounds.x = x;
        tableBounds.y = y;
        tableBounds.width = width;
        tableBounds.height = height;
        Stage stage = this.stage;
        Rectangle scissorBounds = Pools.obtain(Rectangle.class);
        stage.calculateScissors(tableBounds, scissorBounds);
        if (ScissorStack.pushScissors(scissorBounds)) {
            return true;
        }
        Pools.free(scissorBounds);
        return false;
    }

    public void clipEnd() {
        Pools.free(ScissorStack.popScissors());
    }

    public Vector2 screenToLocalCoordinates(Vector2 screenCoords) {
        Stage stage = this.stage;
        if (stage == null) {
            return screenCoords;
        }
        return this.stageToLocalCoordinates(stage.screenToStageCoordinates(screenCoords));
    }

    public Vector2 stageToLocalCoordinates(Vector2 stageCoords) {
        if (this.parent != null) {
            this.parent.stageToLocalCoordinates(stageCoords);
        }
        this.parentToLocalCoordinates(stageCoords);
        return stageCoords;
    }

    public Vector2 localToStageCoordinates(Vector2 localCoords) {
        return this.localToAscendantCoordinates(null, localCoords);
    }

    public Vector2 localToParentCoordinates(Vector2 localCoords) {
        float rotation = -this.rotation;
        float scaleX = this.scaleX;
        float scaleY = this.scaleY;
        float x = this.x;
        float y = this.y;
        if (rotation == 0.0f) {
            if (scaleX == 1.0f && scaleY == 1.0f) {
                localCoords.x += x;
                localCoords.y += y;
            } else {
                float originX = this.originX;
                float originY = this.originY;
                localCoords.x = (localCoords.x - originX) * scaleX + originX + x;
                localCoords.y = (localCoords.y - originY) * scaleY + originY + y;
            }
        } else {
            float cos = (float)Math.cos(rotation * ((float)Math.PI / 180));
            float sin = (float)Math.sin(rotation * ((float)Math.PI / 180));
            float originX = this.originX;
            float originY = this.originY;
            float tox = (localCoords.x - originX) * scaleX;
            float toy = (localCoords.y - originY) * scaleY;
            localCoords.x = tox * cos + toy * sin + originX + x;
            localCoords.y = tox * -sin + toy * cos + originY + y;
        }
        return localCoords;
    }

    public Vector2 localToAscendantCoordinates(Actor ascendant, Vector2 localCoords) {
        Actor actor = this;
        while (actor != null) {
            actor.localToParentCoordinates(localCoords);
            actor = actor.parent;
            if (actor != ascendant) continue;
            break;
        }
        return localCoords;
    }

    public Vector2 parentToLocalCoordinates(Vector2 parentCoords) {
        float rotation = this.rotation;
        float scaleX = this.scaleX;
        float scaleY = this.scaleY;
        float childX = this.x;
        float childY = this.y;
        if (rotation == 0.0f) {
            if (scaleX == 1.0f && scaleY == 1.0f) {
                parentCoords.x -= childX;
                parentCoords.y -= childY;
            } else {
                float originX = this.originX;
                float originY = this.originY;
                parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX;
                parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY;
            }
        } else {
            float cos = (float)Math.cos(rotation * ((float)Math.PI / 180));
            float sin = (float)Math.sin(rotation * ((float)Math.PI / 180));
            float originX = this.originX;
            float originY = this.originY;
            float tox = parentCoords.x - childX - originX;
            float toy = parentCoords.y - childY - originY;
            parentCoords.x = (tox * cos + toy * sin) / scaleX + originX;
            parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY;
        }
        return parentCoords;
    }

    public void drawDebug(ShapeRenderer shapes) {
        this.drawDebugBounds(shapes);
    }

    protected void drawDebugBounds(ShapeRenderer shapes) {
        if (!this.debug) {
            return;
        }
        shapes.set(ShapeRenderer.ShapeType.Line);
        shapes.setColor(this.stage.getDebugColor());
        shapes.rect(this.x, this.y, this.originX, this.originY, this.width, this.height, this.scaleX, this.scaleY, this.rotation);
    }

    public void setDebug(boolean enabled) {
        this.debug = enabled;
        if (enabled) {
            Stage.debug = true;
        }
    }

    public boolean getDebug() {
        return this.debug;
    }

    public Actor debug() {
        this.setDebug(true);
        return this;
    }

    public String toString() {
        int dotIndex;
        String name = this.name;
        if (name == null && (dotIndex = (name = this.getClass().getName()).lastIndexOf(46)) != -1) {
            name = name.substring(dotIndex + 1);
        }
        return name;
    }
}

