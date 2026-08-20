/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.utils.Disposable;

public interface Shader
extends Disposable {
    public void init();

    public int compareTo(Shader var1);

    public boolean canRender(Renderable var1);

    public void begin(Camera var1, RenderContext var2);

    public void render(Renderable var1);

    public void end();
}

