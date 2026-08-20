/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

public interface InputProcessor {
    public boolean keyDown(int var1);

    public boolean keyUp(int var1);

    public boolean keyTyped(char var1);

    public boolean touchDown(int var1, int var2, int var3, int var4);

    public boolean touchUp(int var1, int var2, int var3, int var4);

    public boolean touchDragged(int var1, int var2, int var3);

    public boolean mouseMoved(int var1, int var2);

    public boolean scrolled(int var1);
}

