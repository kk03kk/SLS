/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.model.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.utils.Array;

public class ModelMaterial {
    public String id;
    public MaterialType type;
    public Color ambient;
    public Color diffuse;
    public Color specular;
    public Color emissive;
    public Color reflection;
    public float shininess;
    public float opacity = 1.0f;
    public Array<ModelTexture> textures;

    public static enum MaterialType {
        Lambert,
        Phong;

    }
}

