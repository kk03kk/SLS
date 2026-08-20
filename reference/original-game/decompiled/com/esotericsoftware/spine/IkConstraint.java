/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.IkConstraintData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Updatable;

public class IkConstraint
implements Updatable {
    final IkConstraintData data;
    final Array<Bone> bones;
    Bone target;
    float mix = 1.0f;
    int bendDirection;
    int level;

    public IkConstraint(IkConstraintData data, Skeleton skeleton) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = data;
        this.mix = data.mix;
        this.bendDirection = data.bendDirection;
        this.bones = new Array(data.bones.size);
        for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.findBone(boneData.name));
        }
        this.target = skeleton.findBone(data.target.name);
    }

    public IkConstraint(IkConstraint constraint, Skeleton skeleton) {
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
        this.mix = constraint.mix;
        this.bendDirection = constraint.bendDirection;
    }

    public void apply() {
        this.update();
    }

    @Override
    public void update() {
        Bone target = this.target;
        Array<Bone> bones = this.bones;
        switch (bones.size) {
            case 1: {
                IkConstraint.apply(bones.first(), target.worldX, target.worldY, this.mix);
                break;
            }
            case 2: {
                IkConstraint.apply(bones.first(), bones.get(1), target.worldX, target.worldY, this.bendDirection, this.mix);
            }
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

    public float getMix() {
        return this.mix;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public int getBendDirection() {
        return this.bendDirection;
    }

    public void setBendDirection(int bendDirection) {
        this.bendDirection = bendDirection;
    }

    public IkConstraintData getData() {
        return this.data;
    }

    public String toString() {
        return this.data.name;
    }

    public static void apply(Bone bone, float targetX, float targetY, float alpha) {
        Bone pp = bone.parent;
        float id = 1.0f / (pp.a * pp.d - pp.b * pp.c);
        float x = targetX - pp.worldX;
        float y = targetY - pp.worldY;
        float tx = (x * pp.d - y * pp.b) * id - bone.x;
        float ty = (y * pp.a - x * pp.c) * id - bone.y;
        float rotationIK = MathUtils.atan2(ty, tx) * 57.295776f - bone.shearX - bone.rotation;
        if (bone.scaleX < 0.0f) {
            rotationIK += 180.0f;
        }
        if (rotationIK > 180.0f) {
            rotationIK -= 360.0f;
        } else if (rotationIK < -180.0f) {
            rotationIK += 360.0f;
        }
        bone.updateWorldTransform(bone.x, bone.y, bone.rotation + rotationIK * alpha, bone.scaleX, bone.scaleY, bone.shearX, bone.shearY);
    }

    /*
     * Unable to fully structure code
     */
    public static void apply(Bone parent, Bone child, float targetX, float targetY, int bendDir, float alpha) {
        block27: {
            block26: {
                if (alpha == 0.0f) {
                    child.updateWorldTransform();
                    return;
                }
                px = parent.x;
                py = parent.y;
                psx = parent.scaleX;
                psy = parent.scaleY;
                csx = child.scaleX;
                if (psx < 0.0f) {
                    psx = -psx;
                    os1 = 180;
                    s2 = -1;
                } else {
                    os1 = 0;
                    s2 = 1;
                }
                if (psy < 0.0f) {
                    psy = -psy;
                    s2 = -s2;
                }
                if (csx < 0.0f) {
                    csx = -csx;
                    os2 = 180;
                } else {
                    os2 = 0;
                }
                cx = child.x;
                a = parent.a;
                b = parent.b;
                c = parent.c;
                d = parent.d;
                v0 = u = Math.abs(psx - psy) <= 1.0E-4f;
                if (!u) {
                    cy = 0.0f;
                    cwx = a * cx + parent.worldX;
                    cwy = c * cx + parent.worldY;
                } else {
                    cy = child.y;
                    cwx = a * cx + b * cy + parent.worldX;
                    cwy = c * cx + d * cy + parent.worldY;
                }
                pp = parent.parent;
                a = pp.a;
                b = pp.b;
                c = pp.c;
                d = pp.d;
                id = 1.0f / (a * d - b * c);
                x = targetX - pp.worldX;
                y = targetY - pp.worldY;
                tx = (x * d - y * b) * id - px;
                ty = (y * a - x * c) * id - py;
                x = cwx - pp.worldX;
                y = cwy - pp.worldY;
                dx = (x * d - y * b) * id - px;
                dy = (y * a - x * c) * id - py;
                l1 = (float)Math.sqrt(dx * dx + dy * dy);
                l2 = child.data.length * csx;
                if (!u) break block26;
                if ((cos = (tx * tx + ty * ty - l1 * l1 - (l2 *= psx) * l2) / (2.0f * l1 * l2)) < -1.0f) {
                    cos = -1.0f;
                } else if (cos > 1.0f) {
                    cos = 1.0f;
                }
                a2 = (float)Math.acos(cos) * (float)bendDir;
                a = l1 + l2 * cos;
                b = l2 * MathUtils.sin(a2);
                a1 = MathUtils.atan2(ty * a - tx * b, tx * a + ty * b);
                break block27;
            }
            a = psx * l2;
            b = psy * l2;
            aa = a * a;
            bb = b * b;
            dd = tx * tx + ty * ty;
            ta = MathUtils.atan2(ty, tx);
            c1 = -2.0f * bb * l1;
            c2 = bb - aa;
            c = bb * l1 * l1 + aa * dd - aa * bb;
            d = c1 * c1 - 4.0f * c2 * c;
            if (!(d >= 0.0f)) ** GOTO lbl-1000
            q = (float)Math.sqrt(d);
            if (c1 < 0.0f) {
                q = -q;
            }
            q = -(c1 + q) / 2.0f;
            r0 = q / c2;
            r1 = c / q;
            v1 = r = Math.abs(r0) < Math.abs(r1) ? r0 : r1;
            if (r * r <= dd) {
                y = (float)Math.sqrt(dd - r * r) * (float)bendDir;
                a1 = ta - MathUtils.atan2(y, r);
                a2 = MathUtils.atan2(y / psy, (r - l1) / psx);
            } else lbl-1000:
            // 2 sources

            {
                minAngle = 0.0f;
                minDist = 3.4028235E38f;
                minX = 0.0f;
                minY = 0.0f;
                maxAngle = 0.0f;
                maxDist = 0.0f;
                maxX = 0.0f;
                maxY = 0.0f;
                x = l1 + a;
                d = x * x;
                if (d > maxDist) {
                    maxAngle = 0.0f;
                    maxDist = d;
                    maxX = x;
                }
                if ((d = (x = l1 - a) * x) < minDist) {
                    minAngle = 3.1415927f;
                    minDist = d;
                    minX = x;
                }
                if ((d = (x = a * MathUtils.cos(angle = (float)Math.acos(-a * l1 / (aa - bb))) + l1) * x + (y = b * MathUtils.sin(angle)) * y) < minDist) {
                    minAngle = angle;
                    minDist = d;
                    minX = x;
                    minY = y;
                }
                if (d > maxDist) {
                    maxAngle = angle;
                    maxDist = d;
                    maxX = x;
                    maxY = y;
                }
                if (dd <= (minDist + maxDist) / 2.0f) {
                    a1 = ta - MathUtils.atan2(minY * (float)bendDir, minX);
                    a2 = minAngle * (float)bendDir;
                } else {
                    a1 = ta - MathUtils.atan2(maxY * (float)bendDir, maxX);
                    a2 = maxAngle * (float)bendDir;
                }
            }
        }
        os = MathUtils.atan2(cy, cx) * (float)s2;
        rotation = parent.rotation;
        a1 = (a1 - os) * 57.295776f + (float)os1 - rotation;
        if (a1 > 180.0f) {
            a1 -= 360.0f;
        } else if (a1 < -180.0f) {
            a1 += 360.0f;
        }
        parent.updateWorldTransform(px, py, rotation + a1 * alpha, parent.scaleX, parent.scaleY, 0.0f, 0.0f);
        rotation = child.rotation;
        a2 = ((a2 + os) * 57.295776f - child.shearX) * (float)s2 + (float)os2 - rotation;
        if (a2 > 180.0f) {
            a2 -= 360.0f;
        } else if (a2 < -180.0f) {
            a2 += 360.0f;
        }
        child.updateWorldTransform(cx, cy, rotation + a2 * alpha, child.scaleX, child.scaleY, child.shearX, child.shearY);
    }
}

