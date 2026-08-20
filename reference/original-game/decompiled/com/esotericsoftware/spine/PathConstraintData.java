/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.SlotData;

public class PathConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    SlotData target;
    PositionMode positionMode;
    SpacingMode spacingMode;
    RotateMode rotateMode;
    float offsetRotation;
    float position;
    float spacing;
    float rotateMix;
    float translateMix;

    public PathConstraintData(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.name = name;
    }

    public Array<BoneData> getBones() {
        return this.bones;
    }

    public SlotData getTarget() {
        return this.target;
    }

    public void setTarget(SlotData target) {
        this.target = target;
    }

    public PositionMode getPositionMode() {
        return this.positionMode;
    }

    public void setPositionMode(PositionMode positionMode) {
        this.positionMode = positionMode;
    }

    public SpacingMode getSpacingMode() {
        return this.spacingMode;
    }

    public void setSpacingMode(SpacingMode spacingMode) {
        this.spacingMode = spacingMode;
    }

    public RotateMode getRotateMode() {
        return this.rotateMode;
    }

    public void setRotateMode(RotateMode rotateMode) {
        this.rotateMode = rotateMode;
    }

    public float getOffsetRotation() {
        return this.offsetRotation;
    }

    public void setOffsetRotation(float offsetRotation) {
        this.offsetRotation = offsetRotation;
    }

    public float getPosition() {
        return this.position;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    public float getSpacing() {
        return this.spacing;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
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

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    public static enum RotateMode {
        tangent,
        chain,
        chainScale;

        public static final RotateMode[] values;

        static {
            values = RotateMode.values();
        }
    }

    public static enum SpacingMode {
        length,
        fixed,
        percent;

        public static final SpacingMode[] values;

        static {
            values = SpacingMode.values();
        }
    }

    public static enum PositionMode {
        fixed,
        percent;

        public static final PositionMode[] values;

        static {
            values = PositionMode.values();
        }
    }
}

