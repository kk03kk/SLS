/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;

public class BoneData {
    final int index;
    final String name;
    final BoneData parent;
    float length;
    float x;
    float y;
    float rotation;
    float scaleX = 1.0f;
    float scaleY = 1.0f;
    float shearX;
    float shearY;
    boolean inheritRotation = true;
    boolean inheritScale = true;
    final Color color = new Color(0.61f, 0.61f, 0.61f, 1.0f);

    public BoneData(int index, String name, BoneData parent) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0.");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.index = index;
        this.name = name;
        this.parent = parent;
    }

    public BoneData(BoneData bone, BoneData parent) {
        if (bone == null) {
            throw new IllegalArgumentException("bone cannot be null.");
        }
        this.index = bone.index;
        this.name = bone.name;
        this.parent = parent;
        this.length = bone.length;
        this.x = bone.x;
        this.y = bone.y;
        this.rotation = bone.rotation;
        this.scaleX = bone.scaleX;
        this.scaleY = bone.scaleY;
        this.shearX = bone.shearX;
        this.shearY = bone.shearY;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public BoneData getParent() {
        return this.parent;
    }

    public float getLength() {
        return this.length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
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

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public float getShearX() {
        return this.shearX;
    }

    public void setShearX(float shearX) {
        this.shearX = shearX;
    }

    public float getShearY() {
        return this.shearY;
    }

    public void setShearY(float shearY) {
        this.shearY = shearY;
    }

    public boolean getInheritRotation() {
        return this.inheritRotation;
    }

    public void setInheritRotation(boolean inheritRotation) {
        this.inheritRotation = inheritRotation;
    }

    public boolean getInheritScale() {
        return this.inheritScale;
    }

    public void setInheritScale(boolean inheritScale) {
        this.inheritScale = inheritScale;
    }

    public Color getColor() {
        return this.color;
    }

    public String toString() {
        return this.name;
    }
}

