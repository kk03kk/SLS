/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Frustum {
    protected static final Vector3[] clipSpacePlanePoints = new Vector3[]{new Vector3(-1.0f, -1.0f, -1.0f), new Vector3(1.0f, -1.0f, -1.0f), new Vector3(1.0f, 1.0f, -1.0f), new Vector3(-1.0f, 1.0f, -1.0f), new Vector3(-1.0f, -1.0f, 1.0f), new Vector3(1.0f, -1.0f, 1.0f), new Vector3(1.0f, 1.0f, 1.0f), new Vector3(-1.0f, 1.0f, 1.0f)};
    protected static final float[] clipSpacePlanePointsArray = new float[24];
    private static final Vector3 tmpV;
    public final Plane[] planes = new Plane[6];
    public final Vector3[] planePoints = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3()};
    protected final float[] planePointsArray = new float[24];

    public Frustum() {
        for (int i = 0; i < 6; ++i) {
            this.planes[i] = new Plane(new Vector3(), 0.0f);
        }
    }

    public void update(Matrix4 inverseProjectionView) {
        System.arraycopy(clipSpacePlanePointsArray, 0, this.planePointsArray, 0, clipSpacePlanePointsArray.length);
        Matrix4.prj(inverseProjectionView.val, this.planePointsArray, 0, 8, 3);
        int j = 0;
        for (int i = 0; i < 8; ++i) {
            Vector3 v = this.planePoints[i];
            v.x = this.planePointsArray[j++];
            v.y = this.planePointsArray[j++];
            v.z = this.planePointsArray[j++];
        }
        this.planes[0].set(this.planePoints[1], this.planePoints[0], this.planePoints[2]);
        this.planes[1].set(this.planePoints[4], this.planePoints[5], this.planePoints[7]);
        this.planes[2].set(this.planePoints[0], this.planePoints[4], this.planePoints[3]);
        this.planes[3].set(this.planePoints[5], this.planePoints[1], this.planePoints[6]);
        this.planes[4].set(this.planePoints[2], this.planePoints[3], this.planePoints[6]);
        this.planes[5].set(this.planePoints[4], this.planePoints[0], this.planePoints[1]);
    }

    public boolean pointInFrustum(Vector3 point) {
        for (int i = 0; i < this.planes.length; ++i) {
            Plane.PlaneSide result = this.planes[i].testPoint(point);
            if (result != Plane.PlaneSide.Back) continue;
            return false;
        }
        return true;
    }

    public boolean pointInFrustum(float x, float y, float z) {
        for (int i = 0; i < this.planes.length; ++i) {
            Plane.PlaneSide result = this.planes[i].testPoint(x, y, z);
            if (result != Plane.PlaneSide.Back) continue;
            return false;
        }
        return true;
    }

    public boolean sphereInFrustum(Vector3 center, float radius) {
        for (int i = 0; i < 6; ++i) {
            if (!(this.planes[i].normal.x * center.x + this.planes[i].normal.y * center.y + this.planes[i].normal.z * center.z < -radius - this.planes[i].d)) continue;
            return false;
        }
        return true;
    }

    public boolean sphereInFrustum(float x, float y, float z, float radius) {
        for (int i = 0; i < 6; ++i) {
            if (!(this.planes[i].normal.x * x + this.planes[i].normal.y * y + this.planes[i].normal.z * z < -radius - this.planes[i].d)) continue;
            return false;
        }
        return true;
    }

    public boolean sphereInFrustumWithoutNearFar(Vector3 center, float radius) {
        for (int i = 2; i < 6; ++i) {
            if (!(this.planes[i].normal.x * center.x + this.planes[i].normal.y * center.y + this.planes[i].normal.z * center.z < -radius - this.planes[i].d)) continue;
            return false;
        }
        return true;
    }

    public boolean sphereInFrustumWithoutNearFar(float x, float y, float z, float radius) {
        for (int i = 2; i < 6; ++i) {
            if (!(this.planes[i].normal.x * x + this.planes[i].normal.y * y + this.planes[i].normal.z * z < -radius - this.planes[i].d)) continue;
            return false;
        }
        return true;
    }

    public boolean boundsInFrustum(BoundingBox bounds) {
        int len2 = this.planes.length;
        for (int i = 0; i < len2; ++i) {
            if (this.planes[i].testPoint(bounds.getCorner000(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner001(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner010(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner011(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner100(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner101(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner110(tmpV)) != Plane.PlaneSide.Back || this.planes[i].testPoint(bounds.getCorner111(tmpV)) != Plane.PlaneSide.Back) continue;
            return false;
        }
        return true;
    }

    public boolean boundsInFrustum(Vector3 center, Vector3 dimensions) {
        return this.boundsInFrustum(center.x, center.y, center.z, dimensions.x / 2.0f, dimensions.y / 2.0f, dimensions.z / 2.0f);
    }

    public boolean boundsInFrustum(float x, float y, float z, float halfWidth, float halfHeight, float halfDepth) {
        int len2 = this.planes.length;
        for (int i = 0; i < len2; ++i) {
            if (this.planes[i].testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != Plane.PlaneSide.Back || this.planes[i].testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != Plane.PlaneSide.Back) continue;
            return false;
        }
        return true;
    }

    static {
        int j = 0;
        for (Vector3 v : clipSpacePlanePoints) {
            Frustum.clipSpacePlanePointsArray[j++] = v.x;
            Frustum.clipSpacePlanePointsArray[j++] = v.y;
            Frustum.clipSpacePlanePointsArray[j++] = v.z;
        }
        tmpV = new Vector3();
    }
}

