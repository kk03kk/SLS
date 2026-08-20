/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.screens.options.RemapInputElement;

public class RemapInputElementHeader
extends RemapInputElement {
    private String keyboardText;
    private String controllerText;

    public RemapInputElementHeader(String commandText, String keyboardText, String controllerText) {
        super(null, commandText, null);
        this.keyboardText = keyboardText;
        this.controllerText = controllerText;
        this.isHeader = true;
    }

    @Override
    public void update() {
    }

    @Override
    protected String getKeyColumnText() {
        return this.keyboardText;
    }

    @Override
    protected String getControllerColumnText() {
        return this.controllerText;
    }

    @Override
    protected Color getTextColor() {
        return Settings.GOLD_COLOR;
    }

    @Override
    public void hoverStarted(Hitbox hitbox) {
    }

    @Override
    public void startClicking(Hitbox hitbox) {
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }
}

