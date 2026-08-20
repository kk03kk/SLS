/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public class SkeletonRendererDebug {
    private static final Color boneLineColor = Color.RED;
    private static final Color boneOriginColor = Color.GREEN;
    private static final Color attachmentLineColor = new Color(0.0f, 0.0f, 1.0f, 0.5f);
    private static final Color triangleLineColor = new Color(1.0f, 0.64f, 0.0f, 0.5f);
    private static final Color aabbColor = new Color(0.0f, 1.0f, 0.0f, 0.5f);
    private final ShapeRenderer shapes;
    private boolean drawBones = true;
    private boolean drawRegionAttachments = true;
    private boolean drawBoundingBoxes = true;
    private boolean drawMeshHull = true;
    private boolean drawMeshTriangles = true;
    private boolean drawPaths = true;
    private final SkeletonBounds bounds = new SkeletonBounds();
    private final FloatArray temp = new FloatArray();
    private float scale = 1.0f;
    private float boneWidth = 2.0f;
    private boolean premultipliedAlpha;

    public SkeletonRendererDebug() {
        this.shapes = new ShapeRenderer();
    }

    public SkeletonRendererDebug(ShapeRenderer shapes) {
        this.shapes = shapes;
    }

    public void draw(Skeleton skeleton) {
        float[] vertices;
        int i;
        int i2;
        float skeletonX = skeleton.getX();
        float skeletonY = skeleton.getY();
        Gdx.gl.glEnable(3042);
        int srcFunc = this.premultipliedAlpha ? 1 : 770;
        Gdx.gl.glBlendFunc(srcFunc, 771);
        ShapeRenderer shapes = this.shapes;
        Array<Bone> bones = skeleton.getBones();
        if (this.drawBones) {
            shapes.setColor(boneLineColor);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            int n = bones.size;
            for (i2 = 0; i2 < n; ++i2) {
                Bone bone = bones.get(i2);
                if (bone.parent == null) continue;
                float x = skeletonX + bone.data.length * bone.a + bone.worldX;
                float y = skeletonY + bone.data.length * bone.c + bone.worldY;
                shapes.rectLine(skeletonX + bone.worldX, skeletonY + bone.worldY, x, y, this.boneWidth * this.scale);
            }
            shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.x(skeletonX, skeletonY, 4.0f * this.scale);
        } else {
            shapes.begin(ShapeRenderer.ShapeType.Line);
        }
        if (this.drawRegionAttachments) {
            shapes.setColor(attachmentLineColor);
            Array<Slot> slots = skeleton.getSlots();
            int n = slots.size;
            for (i = 0; i < n; ++i) {
                Slot slot = slots.get(i);
                Attachment attachment = slot.attachment;
                if (!(attachment instanceof RegionAttachment)) continue;
                RegionAttachment regionAttachment = (RegionAttachment)attachment;
                vertices = regionAttachment.updateWorldVertices(slot, false);
                shapes.line(vertices[0], vertices[1], vertices[5], vertices[6]);
                shapes.line(vertices[5], vertices[6], vertices[10], vertices[11]);
                shapes.line(vertices[10], vertices[11], vertices[15], vertices[16]);
                shapes.line(vertices[15], vertices[16], vertices[0], vertices[1]);
            }
        }
        if (this.drawMeshHull || this.drawMeshTriangles) {
            Array<Slot> slots = skeleton.getSlots();
            int n = slots.size;
            for (i = 0; i < n; ++i) {
                Slot slot = slots.get(i);
                Attachment attachment = slot.attachment;
                if (!(attachment instanceof MeshAttachment)) continue;
                MeshAttachment mesh = (MeshAttachment)attachment;
                mesh.updateWorldVertices(slot, false);
                vertices = mesh.getWorldVertices();
                short[] triangles = mesh.getTriangles();
                int hullLength = mesh.getHullLength();
                if (this.drawMeshTriangles) {
                    shapes.setColor(triangleLineColor);
                    int nn = triangles.length;
                    for (int ii = 0; ii < nn; ii += 3) {
                        int v1 = triangles[ii] * 5;
                        int v2 = triangles[ii + 1] * 5;
                        int v3 = triangles[ii + 2] * 5;
                        shapes.triangle(vertices[v1], vertices[v1 + 1], vertices[v2], vertices[v2 + 1], vertices[v3], vertices[v3 + 1]);
                    }
                }
                if (!this.drawMeshHull || hullLength <= 0) continue;
                shapes.setColor(attachmentLineColor);
                hullLength = (hullLength >> 1) * 5;
                float lastX = vertices[hullLength - 5];
                float lastY = vertices[hullLength - 4];
                int nn = hullLength;
                for (int ii = 0; ii < nn; ii += 5) {
                    float x = vertices[ii];
                    float y = vertices[ii + 1];
                    shapes.line(x, y, lastX, lastY);
                    lastX = x;
                    lastY = y;
                }
            }
        }
        if (this.drawBoundingBoxes) {
            SkeletonBounds bounds = this.bounds;
            bounds.update(skeleton, true);
            shapes.setColor(aabbColor);
            shapes.rect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            Array<FloatArray> polygons = bounds.getPolygons();
            Array<BoundingBoxAttachment> boxes = bounds.getBoundingBoxes();
            int n = polygons.size;
            for (int i3 = 0; i3 < n; ++i3) {
                FloatArray polygon = polygons.get(i3);
                shapes.setColor(boxes.get(i3).getColor());
                shapes.polygon(polygon.items, 0, polygon.size);
            }
        }
        if (this.drawPaths) {
            Array<Slot> slots = skeleton.getSlots();
            int n = slots.size;
            for (int i4 = 0; i4 < n; ++i4) {
                Slot slot = slots.get(i4);
                Attachment attachment = slot.attachment;
                if (!(attachment instanceof PathAttachment)) continue;
                PathAttachment path = (PathAttachment)attachment;
                int nn = path.getWorldVerticesLength();
                float[] world = this.temp.setSize(nn);
                path.computeWorldVertices(slot, world);
                Color color = path.getColor();
                float x1 = world[2];
                float y1 = world[3];
                float x2 = 0.0f;
                float y2 = 0.0f;
                if (path.getClosed()) {
                    shapes.setColor(color);
                    float cx1 = world[0];
                    float cy1 = world[1];
                    float cx2 = world[nn - 2];
                    float cy2 = world[nn - 1];
                    x2 = world[nn - 4];
                    y2 = world[nn - 3];
                    shapes.curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, 32);
                    shapes.setColor(Color.LIGHT_GRAY);
                    shapes.line(x1, y1, cx1, cy1);
                    shapes.line(x2, y2, cx2, cy2);
                }
                nn -= 4;
                for (int ii = 4; ii < nn; ii += 6) {
                    float cx1 = world[ii];
                    float cy1 = world[ii + 1];
                    float cx2 = world[ii + 2];
                    float cy2 = world[ii + 3];
                    x2 = world[ii + 4];
                    y2 = world[ii + 5];
                    shapes.setColor(color);
                    shapes.curve(x1, y1, cx1, cy1, cx2, cy2, x2, y2, 32);
                    shapes.setColor(Color.LIGHT_GRAY);
                    shapes.line(x1, y1, cx1, cy1);
                    shapes.line(x2, y2, cx2, cy2);
                    x1 = x2;
                    y1 = y2;
                }
            }
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (this.drawBones) {
            shapes.setColor(boneOriginColor);
            int n = bones.size;
            for (i2 = 0; i2 < n; ++i2) {
                Bone bone = bones.get(i2);
                shapes.setColor(Color.GREEN);
                shapes.circle(skeletonX + bone.worldX, skeletonY + bone.worldY, 3.0f * this.scale, 8);
            }
        }
        shapes.end();
    }

    public ShapeRenderer getShapeRenderer() {
        return this.shapes;
    }

    public void setBones(boolean bones) {
        this.drawBones = bones;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRegionAttachments(boolean regionAttachments) {
        this.drawRegionAttachments = regionAttachments;
    }

    public void setBoundingBoxes(boolean boundingBoxes) {
        this.drawBoundingBoxes = boundingBoxes;
    }

    public void setMeshHull(boolean meshHull) {
        this.drawMeshHull = meshHull;
    }

    public void setMeshTriangles(boolean meshTriangles) {
        this.drawMeshTriangles = meshTriangles;
    }

    public void setPaths(boolean paths) {
        this.drawPaths = paths;
    }

    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        this.premultipliedAlpha = premultipliedAlpha;
    }
}

