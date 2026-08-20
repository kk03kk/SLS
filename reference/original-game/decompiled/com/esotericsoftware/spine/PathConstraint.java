/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.PathConstraintData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.Updatable;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.PathAttachment;

public class PathConstraint
implements Updatable {
    private static final int NONE = -1;
    private static final int BEFORE = -2;
    private static final int AFTER = -3;
    final PathConstraintData data;
    final Array<Bone> bones;
    Slot target;
    float position;
    float spacing;
    float rotateMix;
    float translateMix;
    private final FloatArray spaces = new FloatArray();
    private final FloatArray positions = new FloatArray();
    private final FloatArray world = new FloatArray();
    private final FloatArray curves = new FloatArray();
    private final FloatArray lengths = new FloatArray();
    private final float[] segments = new float[10];

    public PathConstraint(PathConstraintData data, Skeleton skeleton) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = data;
        this.bones = new Array(data.bones.size);
        for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.findBone(boneData.name));
        }
        this.target = skeleton.findSlot(data.target.name);
        this.position = data.position;
        this.spacing = data.spacing;
        this.rotateMix = data.rotateMix;
        this.translateMix = data.translateMix;
    }

    public PathConstraint(PathConstraint constraint, Skeleton skeleton) {
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
        this.target = skeleton.slots.get(constraint.target.data.index);
        this.position = constraint.position;
        this.spacing = constraint.spacing;
        this.rotateMix = constraint.rotateMix;
        this.translateMix = constraint.translateMix;
    }

    public void apply() {
        this.update();
    }

    @Override
    public void update() {
        int i;
        boolean rotate;
        Attachment attachment = this.target.attachment;
        if (!(attachment instanceof PathAttachment)) {
            return;
        }
        float rotateMix = this.rotateMix;
        float translateMix = this.translateMix;
        boolean translate = translateMix > 0.0f;
        boolean bl = rotate = rotateMix > 0.0f;
        if (!translate && !rotate) {
            return;
        }
        PathConstraintData data = this.data;
        PathConstraintData.SpacingMode spacingMode = data.spacingMode;
        boolean lengthSpacing = spacingMode == PathConstraintData.SpacingMode.length;
        PathConstraintData.RotateMode rotateMode = data.rotateMode;
        boolean tangents = rotateMode == PathConstraintData.RotateMode.tangent;
        boolean scale = rotateMode == PathConstraintData.RotateMode.chainScale;
        int boneCount = this.bones.size;
        int spacesCount = tangents ? boneCount : boneCount + 1;
        T[] bones = this.bones.items;
        float[] spaces = this.spaces.setSize(spacesCount);
        float[] lengths = null;
        float spacing = this.spacing;
        if (scale || lengthSpacing) {
            if (scale) {
                lengths = this.lengths.setSize(boneCount);
            }
            i = 0;
            int n = spacesCount - 1;
            while (i < n) {
                Bone bone = (Bone)bones[i];
                float length = bone.data.length;
                float x = length * bone.a;
                float y = length * bone.c;
                length = (float)Math.sqrt(x * x + y * y);
                if (scale) {
                    lengths[i] = length;
                }
                spaces[++i] = lengthSpacing ? Math.max(0.0f, length + spacing) : spacing;
            }
        } else {
            for (i = 1; i < spacesCount; ++i) {
                spaces[i] = spacing;
            }
        }
        float[] positions = this.computeWorldPositions((PathAttachment)attachment, spacesCount, tangents, data.positionMode == PathConstraintData.PositionMode.percent, spacingMode == PathConstraintData.SpacingMode.percent);
        Skeleton skeleton = this.target.getSkeleton();
        float skeletonX = skeleton.x;
        float skeletonY = skeleton.y;
        float boneX = positions[0];
        float boneY = positions[1];
        float offsetRotation = data.offsetRotation;
        boolean tip = rotateMode == PathConstraintData.RotateMode.chain && offsetRotation == 0.0f;
        int i2 = 0;
        int p = 3;
        while (i2 < boneCount) {
            float length;
            Bone bone = (Bone)bones[i2];
            bone.worldX += (boneX - skeletonX - bone.worldX) * translateMix;
            bone.worldY += (boneY - skeletonY - bone.worldY) * translateMix;
            float x = positions[p];
            float y = positions[p + 1];
            float dx = x - boneX;
            float dy = y - boneY;
            if (scale && (length = lengths[i2]) != 0.0f) {
                float s = ((float)Math.sqrt(dx * dx + dy * dy) / length - 1.0f) * rotateMix + 1.0f;
                bone.a *= s;
                bone.c *= s;
            }
            boneX = x;
            boneY = y;
            if (rotate) {
                float sin;
                float cos;
                float a = bone.a;
                float b = bone.b;
                float c = bone.c;
                float d = bone.d;
                float r = tangents ? positions[p - 1] : (spaces[i2 + 1] == 0.0f ? positions[p + 2] : MathUtils.atan2(dy, dx));
                r -= MathUtils.atan2(c, a) - offsetRotation * ((float)Math.PI / 180);
                if (tip) {
                    cos = MathUtils.cos(r);
                    sin = MathUtils.sin(r);
                    float length2 = bone.data.length;
                    boneX += (length2 * (cos * a - sin * c) - dx) * rotateMix;
                    boneY += (length2 * (sin * a + cos * c) - dy) * rotateMix;
                }
                if (r > (float)Math.PI) {
                    r -= (float)Math.PI * 2;
                } else if (r < (float)(-Math.PI)) {
                    r += (float)Math.PI * 2;
                }
                cos = MathUtils.cos(r *= rotateMix);
                sin = MathUtils.sin(r);
                bone.a = cos * a - sin * c;
                bone.b = cos * b - sin * d;
                bone.c = sin * a + cos * c;
                bone.d = sin * b + cos * d;
            }
            ++i2;
            p += 3;
        }
    }

    /*
     * Unable to fully structure code
     */
    float[] computeWorldPositions(PathAttachment path, int spacesCount, boolean tangents, boolean percentPosition, boolean percentSpacing) {
        block37: {
            target = this.target;
            position = this.position;
            spaces = this.spaces.items;
            out = this.positions.setSize(spacesCount * 3 + 2);
            closed = path.getClosed();
            verticesLength = path.getWorldVerticesLength();
            curveCount = verticesLength / 6;
            prevCurve = -1;
            if (path.getConstantSpeed()) break block37;
            lengths = path.getLengths();
            pathLength = lengths[curveCount -= closed != false ? 1 : 2];
            if (percentPosition) {
                position *= pathLength;
            }
            if (percentSpacing) {
                i = 0;
                while (i < spacesCount) {
                    v0 = i++;
                    spaces[v0] = spaces[v0] * pathLength;
                }
            }
            world = this.world.setSize(8);
            i = 0;
            o = 0;
            curve = 0;
            while (i < spacesCount) {
                block38: {
                    space = spaces[i];
                    p = position += space;
                    if (!closed) break block38;
                    if ((p %= pathLength) < 0.0f) {
                        p += pathLength;
                    }
                    curve = 0;
                    ** GOTO lbl-1000
                }
                if (p < 0.0f) {
                    if (prevCurve != -2) {
                        prevCurve = -2;
                        path.computeWorldVertices(target, 2, 4, world, 0);
                    }
                    this.addBeforePosition(p, world, 0, out, o);
                } else if (p > pathLength) {
                    if (prevCurve != -3) {
                        prevCurve = -3;
                        path.computeWorldVertices(target, verticesLength - 6, 4, world, 0);
                    }
                    this.addAfterPosition(p - pathLength, world, 0, out, o);
                } else lbl-1000:
                // 2 sources

                {
                    while (true) {
                        if (!(p > (length = lengths[curve]))) {
                            if (curve == 0) {
                                p /= length;
                                break;
                            }
                            prev = lengths[curve - 1];
                            p = (p - prev) / (length - prev);
                            break;
                        }
                        ++curve;
                    }
                    if (curve != prevCurve) {
                        prevCurve = curve;
                        if (closed && curve == curveCount) {
                            path.computeWorldVertices(target, verticesLength - 4, 4, world, 0);
                            path.computeWorldVertices(target, 0, 4, world, 4);
                        } else {
                            path.computeWorldVertices(target, curve * 6 + 2, 8, world, 0);
                        }
                    }
                    this.addCurvePosition(p, world[0], world[1], world[2], world[3], world[4], world[5], world[6], world[7], out, o, tangents != false || i > 0 && space == 0.0f);
                }
                ++i;
                o += 3;
            }
            return out;
        }
        if (closed) {
            world = this.world.setSize(verticesLength += 2);
            path.computeWorldVertices(target, 2, verticesLength - 4, world, 0);
            path.computeWorldVertices(target, 0, 2, world, verticesLength - 4);
            world[verticesLength - 2] = world[0];
            world[verticesLength - 1] = world[1];
        } else {
            --curveCount;
            world = this.world.setSize(verticesLength -= 4);
            path.computeWorldVertices(target, 2, verticesLength, world, 0);
        }
        curves = this.curves.setSize(curveCount);
        pathLength = 0.0f;
        x1 = world[0];
        y1 = world[1];
        cx1 = 0.0f;
        cy1 = 0.0f;
        cx2 = 0.0f;
        cy2 = 0.0f;
        x2 = 0.0f;
        y2 = 0.0f;
        i = 0;
        w = 2;
        while (i < curveCount) {
            cx1 = world[w];
            cy1 = world[w + 1];
            cx2 = world[w + 2];
            cy2 = world[w + 3];
            x2 = world[w + 4];
            y2 = world[w + 5];
            tmpx = (x1 - cx1 * 2.0f + cx2) * 0.1875f;
            tmpy = (y1 - cy1 * 2.0f + cy2) * 0.1875f;
            dddfx = ((cx1 - cx2) * 3.0f - x1 + x2) * 0.09375f;
            dddfy = ((cy1 - cy2) * 3.0f - y1 + y2) * 0.09375f;
            ddfx = tmpx * 2.0f + dddfx;
            ddfy = tmpy * 2.0f + dddfy;
            dfx = (cx1 - x1) * 0.75f + tmpx + dddfx * 0.16666667f;
            dfy = (cy1 - y1) * 0.75f + tmpy + dddfy * 0.16666667f;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            dfx += ddfx;
            dfy += ddfy;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            pathLength += (float)Math.sqrt((dfx += (ddfx += dddfx)) * dfx + (dfy += (ddfy += dddfy)) * dfy);
            curves[i] = pathLength += (float)Math.sqrt((dfx += ddfx + dddfx) * dfx + (dfy += ddfy + dddfy) * dfy);
            x1 = x2;
            y1 = y2;
            ++i;
            w += 6;
        }
        if (percentPosition) {
            position *= pathLength;
        }
        if (percentSpacing) {
            i = 0;
            while (i < spacesCount) {
                v1 = i++;
                spaces[v1] = spaces[v1] * pathLength;
            }
        }
        segments = this.segments;
        curveLength = 0.0f;
        i = 0;
        o = 0;
        curve = 0;
        segment = 0;
        while (i < spacesCount) {
            block39: {
                space = spaces[i];
                p = position += space;
                if (!closed) break block39;
                if ((p %= pathLength) < 0.0f) {
                    p += pathLength;
                }
                curve = 0;
                ** GOTO lbl-1000
            }
            if (p < 0.0f) {
                this.addBeforePosition(p, world, 0, out, o);
            } else if (p > pathLength) {
                this.addAfterPosition(p - pathLength, world, verticesLength - 4, out, o);
            } else lbl-1000:
            // 2 sources

            {
                while (true) {
                    if (!(p > (length = curves[curve]))) {
                        if (curve == 0) {
                            p /= length;
                            break;
                        }
                        prev = curves[curve - 1];
                        p = (p - prev) / (length - prev);
                        break;
                    }
                    ++curve;
                }
                if (curve != prevCurve) {
                    prevCurve = curve;
                    ii = curve * 6;
                    x1 = world[ii];
                    y1 = world[ii + 1];
                    cx1 = world[ii + 2];
                    cy1 = world[ii + 3];
                    cx2 = world[ii + 4];
                    cy2 = world[ii + 5];
                    x2 = world[ii + 6];
                    y2 = world[ii + 7];
                    tmpx = (x1 - cx1 * 2.0f + cx2) * 0.03f;
                    tmpy = (y1 - cy1 * 2.0f + cy2) * 0.03f;
                    dddfx = ((cx1 - cx2) * 3.0f - x1 + x2) * 0.006f;
                    dddfy = ((cy1 - cy2) * 3.0f - y1 + y2) * 0.006f;
                    ddfx = tmpx * 2.0f + dddfx;
                    ddfy = tmpy * 2.0f + dddfy;
                    dfx = (cx1 - x1) * 0.3f + tmpx + dddfx * 0.16666667f;
                    dfy = (cy1 - y1) * 0.3f + tmpy + dddfy * 0.16666667f;
                    segments[0] = curveLength = (float)Math.sqrt(dfx * dfx + dfy * dfy);
                    for (ii = 1; ii < 8; ++ii) {
                        segments[ii] = curveLength += (float)Math.sqrt((dfx += (ddfx += dddfx)) * dfx + (dfy += (ddfy += dddfy)) * dfy);
                    }
                    segments[8] = curveLength += (float)Math.sqrt((dfx += ddfx) * dfx + (dfy += ddfy) * dfy);
                    segments[9] = curveLength += (float)Math.sqrt((dfx += ddfx + dddfx) * dfx + (dfy += ddfy + dddfy) * dfy);
                    segment = 0;
                }
                p *= curveLength;
                while (true) {
                    if (!(p > (length = segments[segment]))) {
                        if (segment == 0) {
                            p /= length;
                            break;
                        }
                        prev = segments[segment - 1];
                        p = (float)segment + (p - prev) / (length - prev);
                        break;
                    }
                    ++segment;
                }
                this.addCurvePosition(p * 0.1f, x1, y1, cx1, cy1, cx2, cy2, x2, y2, out, o, tangents != false || i > 0 && space == 0.0f);
            }
            ++i;
            o += 3;
        }
        return out;
    }

    private void addBeforePosition(float p, float[] temp, int i, float[] out, int o) {
        float x1 = temp[i];
        float y1 = temp[i + 1];
        float dx = temp[i + 2] - x1;
        float dy = temp[i + 3] - y1;
        float r = MathUtils.atan2(dy, dx);
        out[o] = x1 + p * MathUtils.cos(r);
        out[o + 1] = y1 + p * MathUtils.sin(r);
        out[o + 2] = r;
    }

    private void addAfterPosition(float p, float[] temp, int i, float[] out, int o) {
        float x1 = temp[i + 2];
        float y1 = temp[i + 3];
        float dx = x1 - temp[i];
        float dy = y1 - temp[i + 1];
        float r = MathUtils.atan2(dy, dx);
        out[o] = x1 + p * MathUtils.cos(r);
        out[o + 1] = y1 + p * MathUtils.sin(r);
        out[o + 2] = r;
    }

    private void addCurvePosition(float p, float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2, float[] out, int o, boolean tangents) {
        if (p == 0.0f) {
            p = 1.0E-4f;
        }
        float tt = p * p;
        float ttt = tt * p;
        float u = 1.0f - p;
        float uu = u * u;
        float uuu = uu * u;
        float ut = u * p;
        float ut3 = ut * 3.0f;
        float uut3 = u * ut3;
        float utt3 = ut3 * p;
        float x = x1 * uuu + cx1 * uut3 + cx2 * utt3 + x2 * ttt;
        float y = y1 * uuu + cy1 * uut3 + cy2 * utt3 + y2 * ttt;
        out[o] = x;
        out[o + 1] = y;
        if (tangents) {
            out[o + 2] = MathUtils.atan2(y - (y1 * uu + cy1 * ut * 2.0f + cy2 * tt), x - (x1 * uu + cx1 * ut * 2.0f + cx2 * tt));
        }
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

    public Array<Bone> getBones() {
        return this.bones;
    }

    public Slot getTarget() {
        return this.target;
    }

    public void setTarget(Slot target) {
        this.target = target;
    }

    public PathConstraintData getData() {
        return this.data;
    }

    public String toString() {
        return this.data.name;
    }
}

