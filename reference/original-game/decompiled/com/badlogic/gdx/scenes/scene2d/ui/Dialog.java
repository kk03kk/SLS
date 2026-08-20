/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.ObjectMap;

public class Dialog
extends Window {
    Table contentTable;
    Table buttonTable;
    private Skin skin;
    ObjectMap<Actor, Object> values = new ObjectMap();
    boolean cancelHide;
    Actor previousKeyboardFocus;
    Actor previousScrollFocus;
    FocusListener focusListener;
    protected InputListener ignoreTouchDown = new InputListener(){

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.cancel();
            return false;
        }
    };

    public Dialog(String title, Skin skin) {
        super(title, skin.get(Window.WindowStyle.class));
        this.setSkin(skin);
        this.skin = skin;
        this.initialize();
    }

    public Dialog(String title, Skin skin, String windowStyleName) {
        super(title, skin.get(windowStyleName, Window.WindowStyle.class));
        this.setSkin(skin);
        this.skin = skin;
        this.initialize();
    }

    public Dialog(String title, Window.WindowStyle windowStyle) {
        super(title, windowStyle);
        this.initialize();
    }

    private void initialize() {
        this.setModal(true);
        this.defaults().space(6.0f);
        this.contentTable = new Table(this.skin);
        this.add(this.contentTable).expand().fill();
        this.row();
        this.buttonTable = new Table(this.skin);
        this.add(this.buttonTable).fillX();
        this.contentTable.defaults().space(6.0f);
        this.buttonTable.defaults().space(6.0f);
        this.buttonTable.addListener(new ChangeListener(){

            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                if (!Dialog.this.values.containsKey(actor)) {
                    return;
                }
                while (actor.getParent() != Dialog.this.buttonTable) {
                    actor = actor.getParent();
                }
                Dialog.this.result(Dialog.this.values.get(actor));
                if (!Dialog.this.cancelHide) {
                    Dialog.this.hide();
                }
                Dialog.this.cancelHide = false;
            }
        });
        this.focusListener = new FocusListener(){

            @Override
            public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    this.focusChanged(event);
                }
            }

            @Override
            public void scrollFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    this.focusChanged(event);
                }
            }

            private void focusChanged(FocusListener.FocusEvent event) {
                Actor newFocusedActor;
                Stage stage = Dialog.this.getStage();
                if (Dialog.this.isModal && stage != null && stage.getRoot().getChildren().size > 0 && stage.getRoot().getChildren().peek() == Dialog.this && (newFocusedActor = event.getRelatedActor()) != null && !newFocusedActor.isDescendantOf(Dialog.this) && !newFocusedActor.equals(Dialog.this.previousKeyboardFocus) && !newFocusedActor.equals(Dialog.this.previousScrollFocus)) {
                    event.cancel();
                }
            }
        };
    }

    @Override
    protected void setStage(Stage stage) {
        if (stage == null) {
            this.addListener(this.focusListener);
        } else {
            this.removeListener(this.focusListener);
        }
        super.setStage(stage);
    }

    public Table getContentTable() {
        return this.contentTable;
    }

    public Table getButtonTable() {
        return this.buttonTable;
    }

    public Dialog text(String text) {
        if (this.skin == null) {
            throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin.");
        }
        return this.text(text, this.skin.get(Label.LabelStyle.class));
    }

    public Dialog text(String text, Label.LabelStyle labelStyle) {
        return this.text(new Label((CharSequence)text, labelStyle));
    }

    public Dialog text(Label label) {
        this.contentTable.add(label);
        return this;
    }

    public Dialog button(String text) {
        return this.button(text, null);
    }

    public Dialog button(String text, Object object) {
        if (this.skin == null) {
            throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin.");
        }
        return this.button(text, object, this.skin.get(TextButton.TextButtonStyle.class));
    }

    public Dialog button(String text, Object object, TextButton.TextButtonStyle buttonStyle) {
        return this.button(new TextButton(text, buttonStyle), object);
    }

    public Dialog button(Button button) {
        return this.button(button, null);
    }

    public Dialog button(Button button, Object object) {
        this.buttonTable.add(button);
        this.setObject(button, object);
        return this;
    }

    public Dialog show(Stage stage, Action action) {
        this.clearActions();
        this.removeCaptureListener(this.ignoreTouchDown);
        this.previousKeyboardFocus = null;
        Actor actor = stage.getKeyboardFocus();
        if (actor != null && !actor.isDescendantOf(this)) {
            this.previousKeyboardFocus = actor;
        }
        this.previousScrollFocus = null;
        actor = stage.getScrollFocus();
        if (actor != null && !actor.isDescendantOf(this)) {
            this.previousScrollFocus = actor;
        }
        this.pack();
        stage.addActor(this);
        stage.setKeyboardFocus(this);
        stage.setScrollFocus(this);
        if (action != null) {
            this.addAction(action);
        }
        return this;
    }

    public Dialog show(Stage stage) {
        this.show(stage, Actions.sequence((Action)Actions.alpha(0.0f), (Action)Actions.fadeIn(0.4f, Interpolation.fade)));
        this.setPosition(Math.round((stage.getWidth() - this.getWidth()) / 2.0f), Math.round((stage.getHeight() - this.getHeight()) / 2.0f));
        return this;
    }

    public void hide(Action action) {
        Stage stage = this.getStage();
        if (stage != null) {
            Actor actor;
            this.removeListener(this.focusListener);
            if (this.previousKeyboardFocus != null && this.previousKeyboardFocus.getStage() == null) {
                this.previousKeyboardFocus = null;
            }
            if ((actor = stage.getKeyboardFocus()) == null || actor.isDescendantOf(this)) {
                stage.setKeyboardFocus(this.previousKeyboardFocus);
            }
            if (this.previousScrollFocus != null && this.previousScrollFocus.getStage() == null) {
                this.previousScrollFocus = null;
            }
            if ((actor = stage.getScrollFocus()) == null || actor.isDescendantOf(this)) {
                stage.setScrollFocus(this.previousScrollFocus);
            }
        }
        if (action != null) {
            this.addCaptureListener(this.ignoreTouchDown);
            this.addAction(Actions.sequence(action, (Action)Actions.removeListener(this.ignoreTouchDown, true), (Action)Actions.removeActor()));
        } else {
            this.remove();
        }
    }

    public void hide() {
        this.hide(Actions.sequence((Action)Actions.fadeOut(0.4f, Interpolation.fade), (Action)Actions.removeListener(this.ignoreTouchDown, true), (Action)Actions.removeActor()));
    }

    public void setObject(Actor actor, Object object) {
        this.values.put(actor, object);
    }

    public Dialog key(final int keycode, final Object object) {
        this.addListener(new InputListener(){

            @Override
            public boolean keyDown(InputEvent event, int keycode2) {
                if (keycode == keycode2) {
                    Dialog.this.result(object);
                    if (!Dialog.this.cancelHide) {
                        Dialog.this.hide();
                    }
                    Dialog.this.cancelHide = false;
                }
                return false;
            }
        });
        return this;
    }

    protected void result(Object object) {
    }

    public void cancel() {
        this.cancelHide = true;
    }
}

