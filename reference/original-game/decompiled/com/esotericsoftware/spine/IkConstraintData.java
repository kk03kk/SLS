/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.BoneData;

public class IkConstraintData {
    final String name;
    final Array<BoneData> bones = new Array();
    BoneData target;
    int bendDirection = 1;
    float mix = 1.0f;

    public IkConstraintData(String name) {
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

    public int getBendDirection() {
        return this.bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    public float getMix() {
        return this.mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public String toString() {
        return this.name;
    }
}

