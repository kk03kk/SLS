/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.TransformConstraintData;
import com.esotericsoftware.spine.Updatable;

public class TransformConstraint
implements Updatable {
    final TransformConstraintData data;
    final Array<Bone> bones;
    Bone target;
    float rotateMix;
    float translateMix;
    float scaleMix;
    float shearMix;
    final Vector2 temp = new Vector2();

    public TransformConstraint(TransformConstraintData data, Skeleton skeleton) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = data;
        this.rotateMix = data.rotateMix;
        this.translateMix = data.translateMix;
        this.scaleMix = data.scaleMix;
        this.shearMix = data.shearMix;
        this.bones = new Array(data.bones.size);
        for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.findBone(boneData.name));
        }
        this.target = skeleton.findBone(data.target.name);
    }

    public TransformConstraint(TransformConstraint constraint, Skeleton skeleton) {
        if (constraint == null) {
            throw new IllegalArgumentException("constraint cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = constraint.data;
        this.bones = new Array(constraint.bones.size);
        for (Bone bone : constraint.bones) {
            this.bones.add(skeleton.bones.get(bone.data.index));
        }
        this.target = skeleton.bones.get(constraint.target.data.index);
        this.rotateMix = constraint.rotateMix;
        this.translateMix = constraint.translateMix;
        this.scaleMix = constraint.scaleMix;
        this.shearMix = constraint.shearMix;
    }

    public void apply() {
        this.update();
    }

    @Override
    public void update() {
        float rotateMix = this.rotateMix;
        float translateMix = this.translateMix;
        float scaleMix = this.scaleMix;
        float shearMix = this.shearMix;
        Bone target = this.target;
        float ta = target.a;
        float tb = target.b;
        float tc = target.c;
        float td = target.d;
        Array<Bone> bones = this.bones;
        int n = bones.size;
        for (int i = 0; i < n; ++i) {
            Bone bone = bones.get(i);
            if (rotateMix > 0.0f) {
                float a = bone.a;
                float b = bone.b;
                float c = bone.c;
                float d = bone.d;
                float r = MathUtils.atan2(tc, ta) - MathUtils.atan2(c, a) + this.data.offsetRotation * ((float)Math.PI / 180);
                if (r > (float)Math.PI) {
                    r -= (float)Math.PI * 2;
                } else if (r < (float)(-Math.PI)) {
                    r += (float)Math.PI * 2;
                }
                float cos = MathUtils.cos(r *= rotateMix);
                float sin = MathUtils.sin(r);
                bone.a = cos * a - sin * c;
                bone.b = cos * b - sin * d;
                bone.c = sin * a + cos * c;
                bone.d = sin * b + cos * d;
            }
            if (translateMix > 0.0f) {
                Vector2 temp = this.temp;
                target.localToWorld(temp.set(this.data.offsetX, this.data.offsetY));
                bone.worldX += (temp.x - bone.worldX) * translateMix;
                bone.worldY += (temp.y - bone.worldY) * translateMix;
            }
            if (scaleMix > 0.0f) {
                float bs = (float)Math.sqrt(bone.a * bone.a + bone.c * bone.c);
                float ts = (float)Math.sqrt(ta * ta + tc * tc);
                float s = bs > 1.0E-5f ? (bs + (ts - bs + this.data.offsetScaleX) * scaleMix) / bs : 0.0f;
                bone.a *= s;
                bone.c *= s;
                bs = (float)Math.sqrt(bone.b * bone.b + bone.d * bone.d);
                ts = (float)Math.sqrt(tb * tb + td * td);
                s = bs > 1.0E-5f ? (bs + (ts - bs + this.data.offsetScaleY) * scaleMix) / bs : 0.0f;
                bone.b *= s;
                bone.d *= s;
            }
            if (!(shearMix > 0.0f)) continue;
            float b = bone.b;
            float d = bone.d;
            float by = MathUtils.atan2(d, b);
            float r = MathUtils.atan2(td, tb) - MathUtils.atan2(tc, ta) - (by - MathUtils.atan2(bone.c, bone.a));
            if (r > (float)Math.PI) {
                r -= (float)Math.PI * 2;
            } else if (r < (float)(-Math.PI)) {
                r += (float)Math.PI * 2;
            }
            r = by + (r + this.data.offsetShearY * ((float)Math.PI / 180)) * shearMix;
            float s = (float)Math.sqrt(b * b + d * d);
            bone.b = MathUtils.cos(r) * s;
            bone.d = MathUtils.sin(r) * s;
        }
    }

    public Array<Bone> getBones() {
        return this.bones;
    }

    public Bone getTarget() {
        return this.target;
    }

    public void setTarget(Bone target) {
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

    public TransformConstraintData getData() {
        return this.data;
    }

    public String toString() {
        return this.data.name;
    }
}

