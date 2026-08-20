/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.BoneData;

public class TransformConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    BoneData target;
    float rotateMix;
    float translateMix;
    float scaleMix;
    float shearMix;
    float offsetRotation;
    float offsetX;
    float offsetY;
    float offsetScaleX;
    float offsetScaleY;
    float offsetShearY;

    public TransformConstraintData(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Array<BoneData> getBones() {
        return this.bones;
    }

    public BoneData getTarget() {
        return this.target;
    }

    public void setTarget(BoneData target) {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null.");
        }
        this.target = target;
    }

    public float getRotateMix() {
        return this.rotateMix;
    }

    public void setRotateMix(float rotateMix) {
        this.rotateMix = rotateMix;
    }

    public float getTranslateMix() {
        return this.translateMix;
    }

    public void setTranslateMix(float translateMix) {
        this.translateMix = translateMix;
    }

    public float getScaleMix() {
        return this.scaleMix;
    }

    public void setScaleMix(float scaleMix) {
        this.scaleMix = scaleMix;
    }

    public float getShearMix() {
        return this.shearMix;
    }

    public void setShearMix(float shearMix) {
        this.shearMix = shearMix;
    }

    public float getOffsetRotation() {
        return this.offsetRotation;
    }

    public void setOffsetRotation(float offsetRotation) {
        this.offsetRotation = offsetRotation;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getOffsetScaleX() {
        return this.offsetScaleX;
    }

    public void setOffsetScaleX(float offsetScaleX) {
        this.offsetScaleX = offsetScaleX;
    }

    public float getOffsetScaleY() {
        return this.offsetScaleY;
    }

    public void setOffsetScaleY(float offsetScaleY) {
        this.offsetScaleY = offsetScaleY;
    }

    public float getOffsetShearY() {
        return this.offsetShearY;
    }

    public void setOffsetShearY(float offsetShearY) {
        this.offsetShearY = offsetShearY;
    }

    public String toString() {
        return this.name;
    }
}

