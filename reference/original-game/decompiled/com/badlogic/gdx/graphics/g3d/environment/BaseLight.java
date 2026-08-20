/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.environment;

import com.badlogic.gdx.graphics.Color;

public abstract class BaseLight<T extends BaseLight<T>> {
    public final Color color = new Color(0.0f, 0.0f, 0.0f, 1.0f);

    public T setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        return (T)this;
    }

    public T setColor(Color color) {
        this.color.set(color);
        return (T)this;
    }
}

