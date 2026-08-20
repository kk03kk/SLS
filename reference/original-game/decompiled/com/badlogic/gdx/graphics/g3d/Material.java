/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.utils.Array;

public class Material
extends Attributes {
    private static int counter = 0;
    public String id;

    public Material() {
        this("mtl" + ++counter);
    }

    public Material(String id) {
        this.id = id;
    }

    public Material(Attribute ... attributes) {
        this();
        this.set(attributes);
    }

    public Material(String id, Attribute ... attributes) {
        this(id);
        this.set(attributes);
    }

    public Material(Array<Attribute> attributes) {
        this();
        this.set(attributes);
    }

    public Material(String id, Array<Attribute> attributes) {
        this(id);
        this.set(attributes);
    }

    public Material(Material copyFrom) {
        this(copyFrom.id, copyFrom);
    }

    public Material(String id, Material copyFrom) {
        this(id);
        for (Attribute attr : copyFrom) {
            this.set(attr.copy());
        }
    }

    public final Material copy() {
        return new Material(this);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 3 * this.id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Material && (other == this || ((Material)other).id.equals(this.id) && super.equals(other));
    }
}

