/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;

public class ButtonGroup<T extends Button> {
    private final Array<T> buttons = new Array();
    private Array<T> checkedButtons = new Array(1);
    private int minCheckCount;
    private int maxCheckCount = 1;
    private boolean uncheckLast = true;
    private T lastChecked;

    public ButtonGroup() {
        this.minCheckCount = 1;
    }

    public ButtonGroup(T ... buttons) {
        this.minCheckCount = 0;
        this.add((T)buttons);
        this.minCheckCount = 1;
    }

    public void add(T button) {
        if (button == null) {
            throw new IllegalArgumentException("button cannot be null.");
        }
        ((Button)button).buttonGroup = null;
        boolean shouldCheck = ((Button)button).isChecked() || this.buttons.size < this.minCheckCount;
        ((Button)button).setChecked(false);
        ((Button)button).buttonGroup = this;
        this.buttons.add(button);
        ((Button)button).setChecked(shouldCheck);
    }

    public void add(T ... buttons) {
        if (buttons == null) {
            throw new IllegalArgumentException("buttons cannot be null.");
        }
        int n = buttons.length;
        for (int i = 0; i < n; ++i) {
            this.add(buttons[i]);
        }
    }

    public void remove(T button) {
        if (button == null) {
            throw new IllegalArgumentException("button cannot be null.");
        }
        ((Button)button).buttonGroup = null;
        this.buttons.removeValue(button, true);
        this.checkedButtons.removeValue(button, true);
    }

    public void remove(T ... buttons) {
        if (buttons == null) {
            throw new IllegalArgumentException("buttons cannot be null.");
        }
        int n = buttons.length;
        for (int i = 0; i < n; ++i) {
            this.remove(buttons[i]);
        }
    }

    public void clear() {
        this.buttons.clear();
        this.checkedButtons.clear();
    }

    public void setChecked(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        int n = this.buttons.size;
        for (int i = 0; i < n; ++i) {
            Button button = (Button)this.buttons.get(i);
            if (!(button instanceof TextButton) || !text.contentEquals(((TextButton)button).getText())) continue;
            button.setChecked(true);
            return;
        }
    }

    protected boolean canCheck(T button, boolean newState) {
        if (((Button)button).isChecked == newState) {
            return false;
        }
        if (!newState) {
            if (this.checkedButtons.size <= this.minCheckCount) {
                return false;
            }
            this.checkedButtons.removeValue(button, true);
        } else {
            if (this.maxCheckCount != -1 && this.checkedButtons.size >= this.maxCheckCount) {
                if (this.uncheckLast) {
                    int old = this.minCheckCount;
                    this.minCheckCount = 0;
                    ((Button)this.lastChecked).setChecked(false);
                    this.minCheckCount = old;
                } else {
                    return false;
                }
            }
            this.checkedButtons.add(button);
            this.lastChecked = button;
        }
        return true;
    }

    public void uncheckAll() {
        int old = this.minCheckCount;
        this.minCheckCount = 0;
        int n = this.buttons.size;
        for (int i = 0; i < n; ++i) {
            Button button = (Button)this.buttons.get(i);
            button.setChecked(false);
        }
        this.minCheckCount = old;
    }

    public T getChecked() {
        if (this.checkedButtons.size > 0) {
            return (T)((Button)this.checkedButtons.get(0));
        }
        return null;
    }

    public int getCheckedIndex() {
        if (this.checkedButtons.size > 0) {
            return this.buttons.indexOf(this.checkedButtons.get(0), true);
        }
        return -1;
    }

    public Array<T> getAllChecked() {
        return this.checkedButtons;
    }

    public Array<T> getButtons() {
        return this.buttons;
    }

    public void setMinCheckCount(int minCheckCount) {
        this.minCheckCount = minCheckCount;
    }

    public void setMaxCheckCount(int maxCheckCount) {
        if (maxCheckCount == 0) {
            maxCheckCount = -1;
        }
        this.maxCheckCount = maxCheckCount;
    }

    public void setUncheckLast(boolean uncheckLast) {
        this.uncheckLast = uncheckLast;
    }
}

