/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Vector2;

public final class GeometryUtils {
    private static final Vector2 tmp1 = new Vector2();
    private static final Vector2 tmp2 = new Vector2();
    private static final Vector2 tmp3 = new Vector2();

    public static Vector2 toBarycoord(Vector2 p, Vector2 a, Vector2 b, Vector2 c, Vector2 barycentricOut) {
        Vector2 v0 = tmp1.set(b).sub(a);
        Vector2 v1 = tmp2.set(c).sub(a);
        Vector2 v2 = tmp3.set(p).sub(a);
        float d00 = v0.dot(v0);
        float d01 = v0.dot(v1);
        float d11 = v1.dot(v1);
        float d20 = v2.dot(v0);
        float d21 = v2.dot(v1);
        float denom = d00 * d11 - d01 * d01;
        barycentricOut.x = (d11 * d20 - d01 * d21) / denom;
        barycentricOut.y = (d00 * d21 - d01 * d20) / denom;
        return barycentricOut;
    }

    public static boolean barycoordInsideTriangle(Vector2 barycentric) {
        return barycentric.x >= 0.0f && barycentric.y >= 0.0f && barycentric.x + barycentric.y <= 1.0f;
    }

    public static Vector2 fromBarycoord(Vector2 barycentric, Vector2 a, Vector2 b, Vector2 c, Vector2 interpolatedOut) {
        float u = 1.0f - barycentric.x - barycentric.y;
        interpolatedOut.x = u * a.x + barycentric.x * b.x + barycentric.y * c.x;
        interpolatedOut.y = u * a.y + barycentric.x * b.y + barycentric.y * c.y;
        return interpolatedOut;
    }

    public static float fromBarycoord(Vector2 barycentric, float a, float b, float c) {
        float u = 1.0f - barycentric.x - barycentric.y;
        return u * a + barycentric.x * b + barycentric.y * c;
    }

    public static float lowestPositiveRoot(float a, float b, float c) {
        float r2;
        float invA;
        float det = b * b - 4.0f * a * c;
        if (det < 0.0f) {
            return Float.NaN;
        }
        float sqrtD = (float)Math.sqrt(det);
        float r1 = (-b - sqrtD) * (invA = 1.0f / (2.0f * a));
        if (r1 > (r2 = (-b + sqrtD) * invA)) {
            float tmp = r2;
            r2 = r1;
            r1 = tmp;
        }
        if (r1 > 0.0f) {
            return r1;
        }
        if (r2 > 0.0f) {
            return r2;
        }
        return Float.NaN;
    }

    public static boolean colinear(float x1, float y1, float x2, float y2, float x3, float y3) {
        float dx32 = x3 - x2;
        float dy21 = y2 - y1;
        float dx21 = x2 - x1;
        float dy32 = y3 - y2;
        float det = dx32 * dy21 - dx21 * dy32;
        return Math.abs(det) < 1.0E-6f;
    }

    public static Vector2 triangleCentroid(float x1, float y1, float x2, float y2, float x3, float y3, Vector2 centroid) {
        centroid.x = (x1 + x2 + x3) / 3.0f;
        centroid.y = (y1 + y2 + y3) / 3.0f;
        return centroid;
    }

    public static Vector2 triangleCircumcenter(float x1, float y1, float x2, float y2, float x3, float y3, Vector2 circumcenter) {
        float dx21 = x2 - x1;
        float dy21 = y2 - y1;
        float dx32 = x3 - x2;
        float dy32 = y3 - y2;
        float dx13 = x1 - x3;
        float dy13 = y1 - y3;
        float det = dx32 * dy21 - dx21 * dy32;
        if (Math.abs(det) < 1.0E-6f) {
            throw new IllegalArgumentException("Triangle points must not be colinear.");
        }
        float sqr1 = x1 * x1 + y1 * y1;
        float sqr2 = x2 * x2 + y2 * y2;
        float sqr3 = x3 * x3 + y3 * y3;
        circumcenter.set((sqr1 * dy32 + sqr2 * dy13 + sqr3 * dy21) / (det *= 2.0f), -(sqr1 * dx32 + sqr2 * dx13 + sqr3 * dx21) / det);
        return circumcenter;
    }

    public static float triangleCircumradius(float x1, float y1, float x2, float y2, float x3, float y3) {
        float y;
        float x;
        if (Math.abs(y2 - y1) < 1.0E-6f) {
            float m2 = -(x3 - x2) / (y3 - y2);
            float mx2 = (x2 + x3) / 2.0f;
            float my2 = (y2 + y3) / 2.0f;
            x = (x2 + x1) / 2.0f;
            y = m2 * (x - mx2) + my2;
        } else if (Math.abs(y3 - y2) < 1.0E-6f) {
            float m1 = -(x2 - x1) / (y2 - y1);
            float mx1 = (x1 + x2) / 2.0f;
            float my1 = (y1 + y2) / 2.0f;
            x = (x3 + x2) / 2.0f;
            y = m1 * (x - mx1) + my1;
        } else {
            float m1 = -(x2 - x1) / (y2 - y1);
            float m2 = -(x3 - x2) / (y3 - y2);
            float mx1 = (x1 + x2) / 2.0f;
            float mx2 = (x2 + x3) / 2.0f;
            float my1 = (y1 + y2) / 2.0f;
            float my2 = (y2 + y3) / 2.0f;
            x = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            y = m1 * (x - mx1) + my1;
        }
        float dx = x1 - x;
        float dy = y1 - y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    public static float triangleQuality(float x1, float y1, float x2, float y2, float x3, float y3) {
        float length1 = (float)Math.sqrt(x1 * x1 + y1 * y1);
        float length2 = (float)Math.sqrt(x2 * x2 + y2 * y2);
        float length3 = (float)Math.sqrt(x3 * x3 + y3 * y3);
        return Math.min(length1, Math.min(length2, length3)) / GeometryUtils.triangleCircumradius(x1, y1, x2, y2, x3, y3);
    }

    public static float triangleArea(float x1, float y1, float x2, float y2, float x3, float y3) {
        return Math.abs((x1 - x3) * (y2 - y1) - (x1 - x2) * (y3 - y1)) * 0.5f;
    }

    public static Vector2 quadrilateralCentroid(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2 centroid) {
        float avgX1 = (x1 + x2 + x3) / 3.0f;
        float avgY1 = (y1 + y2 + y3) / 3.0f;
        float avgX2 = (x1 + x4 + x3) / 3.0f;
        float avgY2 = (y1 + y4 + y3) / 3.0f;
        centroid.x = avgX1 - (avgX1 - avgX2) / 2.0f;
        centroid.y = avgY1 - (avgY1 - avgY2) / 2.0f;
        return centroid;
    }

    public static Vector2 polygonCentroid(float[] polygon, int offset, int count, Vector2 centroid) {
        int i;
        if (count < 6) {
            throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.");
        }
        float x = 0.0f;
        float y = 0.0f;
        float signedArea = 0.0f;
        int n = offset + count - 2;
        for (i = offset; i < n; i += 2) {
            float x0 = polygon[i];
            float y0 = polygon[i + 1];
            float x1 = polygon[i + 2];
            float y1 = polygon[i + 3];
            float a = x0 * y1 - x1 * y0;
            signedArea += a;
            x += (x0 + x1) * a;
            y += (y0 + y1) * a;
        }
        float x0 = polygon[i];
        float y0 = polygon[i + 1];
        float x1 = polygon[offset];
        float y1 = polygon[offset + 1];
        float a = x0 * y1 - x1 * y0;
        x += (x0 + x1) * a;
        y += (y0 + y1) * a;
        if ((signedArea += a) == 0.0f) {
            centroid.x = 0.0f;
            centroid.y = 0.0f;
        } else {
            centroid.x = x / (6.0f * (signedArea *= 0.5f));
            centroid.y = y / (6.0f * signedArea);
        }
        return centroid;
    }

    public static float polygonArea(float[] polygon, int offset, int count) {
        float area = 0.0f;
        int n = offset + count;
        for (int i = offset; i < n; i += 2) {
            int y2;
            int x1 = i;
            int y1 = i + 1;
            int x2 = (i + 2) % n;
            if (x2 < offset) {
                x2 += offset;
            }
            if ((y2 = (i + 3) % n) < offset) {
                y2 += offset;
            }
            area += polygon[x1] * polygon[y2];
            area -= polygon[x2] * polygon[y1];
        }
        return area *= 0.5f;
    }

    public static void ensureCCW(float[] polygon) {
        if (!GeometryUtils.areVerticesClockwise(polygon, 0, polygon.length)) {
            return;
        }
        int lastX = polygon.length - 2;
        int n = polygon.length / 2;
        for (int i = 0; i < n; i += 2) {
            int other = lastX - i;
            float x = polygon[i];
            float y = polygon[i + 1];
            polygon[i] = polygon[other];
            polygon[i + 1] = polygon[other + 1];
            polygon[other] = x;
            polygon[other + 1] = y;
        }
    }

    private static boolean areVerticesClockwise(float[] polygon, int offset, int count) {
        float p2y;
        float p2x;
        float p1y;
        float p1x;
        if (count <= 2) {
            return false;
        }
        float area = 0.0f;
        int n = offset + count - 3;
        for (int i = offset; i < n; i += 2) {
            p1x = polygon[i];
            p1y = polygon[i + 1];
            p2x = polygon[i + 2];
            p2y = polygon[i + 3];
            area += p1x * p2y - p2x * p1y;
        }
        p1x = polygon[count - 2];
        p2y = polygon[1];
        p2x = polygon[0];
        p1y = polygon[count - 1];
        return area + p1x * p2y - p2x * p1y < 0.0f;
    }
}

