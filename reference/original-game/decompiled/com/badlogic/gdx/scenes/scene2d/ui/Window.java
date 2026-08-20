/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class Window
extends Table {
    private static final Vector2 tmpPosition = new Vector2();
    private static final Vector2 tmpSize = new Vector2();
    private static final int MOVE = 32;
    private WindowStyle style;
    boolean isMovable = true;
    boolean isModal;
    boolean isResizable;
    int resizeBorder = 8;
    boolean keepWithinStage = true;
    Label titleLabel;
    Table titleTable;
    boolean drawTitleTable;
    protected int edge;
    protected boolean dragging;

    public Window(String title, Skin skin) {
        this(title, skin.get(WindowStyle.class));
        this.setSkin(skin);
    }

    public Window(String title, Skin skin, String styleName) {
        this(title, skin.get(styleName, WindowStyle.class));
        this.setSkin(skin);
    }

    public Window(String title, WindowStyle style) {
        if (title == null) {
            throw new IllegalArgumentException("title cannot be null.");
        }
        this.setTouchable(Touchable.enabled);
        this.setClip(true);
        this.titleLabel = new Label((CharSequence)title, new Label.LabelStyle(style.titleFont, style.titleFontColor));
        this.titleLabel.setEllipsis(true);
        this.titleTable = new Table(){

            @Override
            public void draw(Batch batch, float parentAlpha) {
                if (Window.this.drawTitleTable) {
                    super.draw(batch, parentAlpha);
                }
            }
        };
        this.titleTable.add(this.titleLabel).expandX().fillX().minWidth(0.0f);
        this.addActor(this.titleTable);
        this.setStyle(style);
        this.setWidth(150.0f);
        this.setHeight(150.0f);
        this.addCaptureListener(new InputListener(){

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Window.this.toFront();
                return false;
            }
        });
        this.addListener(new InputListener(){
            float startX;
            float startY;
            float lastX;
            float lastY;

            private void updateEdge(float x, float y) {
                float border = (float)Window.this.resizeBorder / 2.0f;
                float width = Window.this.getWidth();
                float height = Window.this.getHeight();
                float padTop = Window.this.getPadTop();
                float padLeft = Window.this.getPadLeft();
                float padBottom = Window.this.getPadBottom();
                float padRight = Window.this.getPadRight();
                float left = padLeft;
                float right = width - padRight;
                float bottom = padBottom;
                Window.this.edge = 0;
                if (Window.this.isResizable && x >= left - border && x <= right + border && y >= bottom - border) {
                    if (x < left + border) {
                        Window.this.edge |= 8;
                    }
                    if (x > right - border) {
                        Window.this.edge |= 0x10;
                    }
                    if (y < bottom + border) {
                        Window.this.edge |= 4;
                    }
                    if (Window.this.edge != 0) {
                        border += 25.0f;
                    }
                    if (x < left + border) {
                        Window.this.edge |= 8;
                    }
                    if (x > right - border) {
                        Window.this.edge |= 0x10;
                    }
                    if (y < bottom + border) {
                        Window.this.edge |= 4;
                    }
                }
                if (Window.this.isMovable && Window.this.edge == 0 && y <= height && y >= height - padTop && x >= left && x <= right) {
                    Window.this.edge = 32;
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == 0) {
                    this.updateEdge(x, y);
                    Window.this.dragging = Window.this.edge != 0;
                    this.startX = x;
                    this.startY = y;
                    this.lastX = x - Window.this.getWidth();
                    this.lastY = y - Window.this.getHeight();
                }
                return Window.this.edge != 0 || Window.this.isModal;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                Window.this.dragging = false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                float amountY;
                float amountX;
                boolean clampPosition;
                if (!Window.this.dragging) {
                    return;
                }
                float width = Window.this.getWidth();
                float height = Window.this.getHeight();
                float windowX = Window.this.getX();
                float windowY = Window.this.getY();
                float minWidth = Window.this.getMinWidth();
                float maxWidth = Window.this.getMaxWidth();
                float minHeight = Window.this.getMinHeight();
                float maxHeight = Window.this.getMaxHeight();
                Stage stage = Window.this.getStage();
                boolean bl = clampPosition = Window.this.keepWithinStage && Window.this.getParent() == stage.getRoot();
                if ((Window.this.edge & 0x20) != 0) {
                    amountX = x - this.startX;
                    float amountY2 = y - this.startY;
                    windowX += amountX;
                    windowY += amountY2;
                }
                if ((Window.this.edge & 8) != 0) {
                    amountX = x - this.startX;
                    if (width - amountX < minWidth) {
                        amountX = -(minWidth - width);
                    }
                    if (clampPosition && windowX + amountX < 0.0f) {
                        amountX = -windowX;
                    }
                    width -= amountX;
                    windowX += amountX;
                }
                if ((Window.this.edge & 4) != 0) {
                    amountY = y - this.startY;
                    if (height - amountY < minHeight) {
                        amountY = -(minHeight - height);
                    }
                    if (clampPosition && windowY + amountY < 0.0f) {
                        amountY = -windowY;
                    }
                    height -= amountY;
                    windowY += amountY;
                }
                if ((Window.this.edge & 0x10) != 0) {
                    amountX = x - this.lastX - width;
                    if (width + amountX < minWidth) {
                        amountX = minWidth - width;
                    }
                    if (clampPosition && windowX + width + amountX > stage.getWidth()) {
                        amountX = stage.getWidth() - windowX - width;
                    }
                    width += amountX;
                }
                if ((Window.this.edge & 2) != 0) {
                    amountY = y - this.lastY - height;
                    if (height + amountY < minHeight) {
                        amountY = minHeight - height;
                    }
                    if (clampPosition && windowY + height + amountY > stage.getHeight()) {
                        amountY = stage.getHeight() - windowY - height;
                    }
                    height += amountY;
                }
                Window.this.setBounds(Math.round(windowX), Math.round(windowY), Math.round(width), Math.round(height));
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                this.updateEdge(x, y);
                return Window.this.isModal;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, int amount) {
                return Window.this.isModal;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return Window.this.isModal;
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                return Window.this.isModal;
            }

            @Override
            public boolean keyTyped(InputEvent event, char character) {
                return Window.this.isModal;
            }
        });
    }

    public void setStyle(WindowStyle style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        this.style = style;
        this.setBackground(style.background);
        this.titleLabel.setStyle(new Label.LabelStyle(style.titleFont, style.titleFontColor));
        this.invalidateHierarchy();
    }

    public WindowStyle getStyle() {
        return this.style;
    }

    void keepWithinStage() {
        if (!this.keepWithinStage) {
            return;
        }
        Stage stage = this.getStage();
        Camera camera = stage.getCamera();
        if (camera instanceof OrthographicCamera) {
            OrthographicCamera orthographicCamera = (OrthographicCamera)camera;
            float parentWidth = stage.getWidth();
            float parentHeight = stage.getHeight();
            if (this.getX(16) - camera.position.x > parentWidth / 2.0f / orthographicCamera.zoom) {
                this.setPosition(camera.position.x + parentWidth / 2.0f / orthographicCamera.zoom, this.getY(16), 16);
            }
            if (this.getX(8) - camera.position.x < -parentWidth / 2.0f / orthographicCamera.zoom) {
                this.setPosition(camera.position.x - parentWidth / 2.0f / orthographicCamera.zoom, this.getY(8), 8);
            }
            if (this.getY(2) - camera.position.y > parentHeight / 2.0f / orthographicCamera.zoom) {
                this.setPosition(this.getX(2), camera.position.y + parentHeight / 2.0f / orthographicCamera.zoom, 2);
            }
            if (this.getY(4) - camera.position.y < -parentHeight / 2.0f / orthographicCamera.zoom) {
                this.setPosition(this.getX(4), camera.position.y - parentHeight / 2.0f / orthographicCamera.zoom, 4);
            }
        } else if (this.getParent() == stage.getRoot()) {
            float parentWidth = stage.getWidth();
            float parentHeight = stage.getHeight();
            if (this.getX() < 0.0f) {
                this.setX(0.0f);
            }
            if (this.getRight() > parentWidth) {
                this.setX(parentWidth - this.getWidth());
            }
            if (this.getY() < 0.0f) {
                this.setY(0.0f);
            }
            if (this.getTop() > parentHeight) {
                this.setY(parentHeight - this.getHeight());
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Stage stage = this.getStage();
        if (stage.getKeyboardFocus() == null) {
            stage.setKeyboardFocus(this);
        }
        this.keepWithinStage();
        if (this.style.stageBackground != null) {
            this.stageToLocalCoordinates(tmpPosition.set(0.0f, 0.0f));
            this.stageToLocalCoordinates(tmpSize.set(stage.getWidth(), stage.getHeight()));
            this.drawStageBackground(batch, parentAlpha, this.getX() + Window.tmpPosition.x, this.getY() + Window.tmpPosition.y, this.getX() + Window.tmpSize.x, this.getY() + Window.tmpSize.y);
        }
        super.draw(batch, parentAlpha);
    }

    protected void drawStageBackground(Batch batch, float parentAlpha, float x, float y, float width, float height) {
        Color color = this.getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        this.style.stageBackground.draw(batch, x, y, width, height);
    }

    @Override
    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        super.drawBackground(batch, parentAlpha, x, y);
        this.titleTable.getColor().a = this.getColor().a;
        float padTop = this.getPadTop();
        float padLeft = this.getPadLeft();
        this.titleTable.setSize(this.getWidth() - padLeft - this.getPadRight(), padTop);
        this.titleTable.setPosition(padLeft, this.getHeight() - padTop);
        this.drawTitleTable = true;
        this.titleTable.draw(batch, parentAlpha);
        this.drawTitleTable = false;
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        Actor hit = super.hit(x, y, touchable);
        if (hit == null && this.isModal && (!touchable || this.getTouchable() == Touchable.enabled)) {
            return this;
        }
        float height = this.getHeight();
        if (hit == null || hit == this) {
            return hit;
        }
        if (y <= height && y >= height - this.getPadTop() && x >= 0.0f && x <= this.getWidth()) {
            Actor current = hit;
            while (current.getParent() != this) {
                current = current.getParent();
            }
            if (this.getCell(current) != null) {
                return this;
            }
        }
        return hit;
    }

    public boolean isMovable() {
        return this.isMovable;
    }

    public void setMovable(boolean isMovable) {
        this.isMovable = isMovable;
    }

    public boolean isModal() {
        return this.isModal;
    }

    public void setModal(boolean isModal) {
        this.isModal = isModal;
    }

    public void setKeepWithinStage(boolean keepWithinStage) {
        this.keepWithinStage = keepWithinStage;
    }

    public boolean isResizable() {
        return this.isResizable;
    }

    public void setResizable(boolean isResizable) {
        this.isResizable = isResizable;
    }

    public void setResizeBorder(int resizeBorder) {
        this.resizeBorder = resizeBorder;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public float getPrefWidth() {
        return Math.max(super.getPrefWidth(), this.titleLabel.getPrefWidth() + this.getPadLeft() + this.getPadRight());
    }

    public Table getTitleTable() {
        return this.titleTable;
    }

    public Label getTitleLabel() {
        return this.titleLabel;
    }

    public static class WindowStyle {
        public Drawable background;
        public BitmapFont titleFont;
        public Color titleFontColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        public Drawable stageBackground;

        public WindowStyle() {
        }

        public WindowStyle(BitmapFont titleFont, Color titleFontColor, Drawable background) {
            this.background = background;
            this.titleFont = titleFont;
            this.titleFontColor.set(titleFontColor);
        }

        public WindowStyle(WindowStyle style) {
            this.background = style.background;
            this.titleFont = style.titleFont;
            this.titleFontColor = new Color(style.titleFontColor);
        }
    }
}

