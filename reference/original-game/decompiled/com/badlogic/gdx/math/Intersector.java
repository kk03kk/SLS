/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import java.util.Arrays;
import java.util.List;

public final class Intersector {
    private static final Vector3 v0 = new Vector3();
    private static final Vector3 v1 = new Vector3();
    private static final Vector3 v2 = new Vector3();
    private static final FloatArray floatArray = new FloatArray();
    private static final FloatArray floatArray2 = new FloatArray();
    private static final Vector2 ip = new Vector2();
    private static final Vector2 ep1 = new Vector2();
    private static final Vector2 ep2 = new Vector2();
    private static final Vector2 s = new Vector2();
    private static final Vector2 e = new Vector2();
    private static final Plane p = new Plane(new Vector3(), 0.0f);
    private static final Vector3 i = new Vector3();
    private static final Vector3 dir = new Vector3();
    private static final Vector3 start = new Vector3();
    static Vector3 best = new Vector3();
    static Vector3 tmp = new Vector3();
    static Vector3 tmp1 = new Vector3();
    static Vector3 tmp2 = new Vector3();
    static Vector3 tmp3 = new Vector3();
    static Vector2 v2tmp = new Vector2();
    static Vector3 intersection = new Vector3();

    public static boolean isPointInTriangle(Vector3 point, Vector3 t1, Vector3 t2, Vector3 t3) {
        v0.set(t1).sub(point);
        v1.set(t2).sub(point);
        v2.set(t3).sub(point);
        float ab = v0.dot(v1);
        float ac = v0.dot(v2);
        float bc = v1.dot(v2);
        float cc = v2.dot(v2);
        if (bc * ac - cc * ab < 0.0f) {
            return false;
        }
        float bb = v1.dot(v1);
        return !(ab * bc - ac * bb < 0.0f);
    }

    public static boolean isPointInTriangle(Vector2 p, Vector2 a, Vector2 b, Vector2 c) {
        boolean side12;
        float px1;
        float py1;
        if ((c.x - a.x) * py1 - (c.y - a.y) * px1 > 0.0f == (side12 = (b.x - a.x) * (py1 = p.y - a.y) - (b.y - a.y) * (px1 = p.x - a.x) > 0.0f)) {
            return false;
        }
        return (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x) > 0.0f == side12;
    }

    public static boolean isPointInTriangle(float px, float py, float ax, float ay, float bx, float by, float cx, float cy) {
        boolean side12;
        float px1;
        float py1;
        if ((cx - ax) * py1 - (cy - ay) * px1 > 0.0f == (side12 = (bx - ax) * (py1 = py - ay) - (by - ay) * (px1 = px - ax) > 0.0f)) {
            return false;
        }
        return (cx - bx) * (py - by) - (cy - by) * (px - bx) > 0.0f == side12;
    }

    public static boolean intersectSegmentPlane(Vector3 start, Vector3 end, Plane plane, Vector3 intersection) {
        Vector3 dir = v0.set(end).sub(start);
        float denom = dir.dot(plane.getNormal());
        float t = -(start.dot(plane.getNormal()) + plane.getD()) / denom;
        if (t < 0.0f || t > 1.0f) {
            return false;
        }
        intersection.set(start).add(dir.scl(t));
        return true;
    }

    public static int pointLineSide(Vector2 linePoint1, Vector2 linePoint2, Vector2 point) {
        return (int)Math.signum((linePoint2.x - linePoint1.x) * (point.y - linePoint1.y) - (linePoint2.y - linePoint1.y) * (point.x - linePoint1.x));
    }

    public static int pointLineSide(float linePoint1X, float linePoint1Y, float linePoint2X, float linePoint2Y, float pointX, float pointY) {
        return (int)Math.signum((linePoint2X - linePoint1X) * (pointY - linePoint1Y) - (linePoint2Y - linePoint1Y) * (pointX - linePoint1X));
    }

    public static boolean isPointInPolygon(Array<Vector2> polygon, Vector2 point) {
        Vector2 lastVertice = polygon.peek();
        boolean oddNodes = false;
        for (int i = 0; i < polygon.size; ++i) {
            Vector2 vertice = polygon.get(i);
            if ((vertice.y < point.y && lastVertice.y >= point.y || lastVertice.y < point.y && vertice.y >= point.y) && vertice.x + (point.y - vertice.y) / (lastVertice.y - vertice.y) * (lastVertice.x - vertice.x) < point.x) {
                oddNodes = !oddNodes;
            }
            lastVertice = vertice;
        }
        return oddNodes;
    }

    public static boolean isPointInPolygon(float[] polygon, int offset, int count, float x, float y) {
        boolean oddNodes = false;
        int j = offset + count - 2;
        int n = j;
        for (int i = offset; i <= n; i += 2) {
            float xi;
            float yi = polygon[i + 1];
            float yj = polygon[j + 1];
            if ((yi < y && yj >= y || yj < y && yi >= y) && (xi = polygon[i]) + (y - yi) / (yj - yi) * (polygon[j] - xi) < x) {
                oddNodes = !oddNodes;
            }
            j = i;
        }
        return oddNodes;
    }

    public static boolean intersectPolygons(Polygon p1, Polygon p2, Polygon overlap) {
        floatArray2.clear();
        floatArray.clear();
        floatArray2.addAll(p1.getTransformedVertices());
        if (p1.getVertices().length == 0 || p2.getVertices().length == 0) {
            return false;
        }
        for (int i = 0; i < p2.getTransformedVertices().length; i += 2) {
            ep1.set(p2.getTransformedVertices()[i], p2.getTransformedVertices()[i + 1]);
            if (i < p2.getTransformedVertices().length - 2) {
                ep2.set(p2.getTransformedVertices()[i + 2], p2.getTransformedVertices()[i + 3]);
            } else {
                ep2.set(p2.getTransformedVertices()[0], p2.getTransformedVertices()[1]);
            }
            if (Intersector.floatArray2.size == 0) {
                return false;
            }
            s.set(floatArray2.get(Intersector.floatArray2.size - 2), floatArray2.get(Intersector.floatArray2.size - 1));
            for (int j = 0; j < Intersector.floatArray2.size; j += 2) {
                e.set(floatArray2.get(j), floatArray2.get(j + 1));
                if (Intersector.pointLineSide(ep2, ep1, e) > 0) {
                    if (Intersector.pointLineSide(ep2, ep1, s) <= 0) {
                        Intersector.intersectLines(s, e, ep1, ep2, ip);
                        if (Intersector.floatArray.size < 2 || floatArray.get(Intersector.floatArray.size - 2) != Intersector.ip.x || floatArray.get(Intersector.floatArray.size - 1) != Intersector.ip.y) {
                            floatArray.add(Intersector.ip.x);
                            floatArray.add(Intersector.ip.y);
                        }
                    }
                    floatArray.add(Intersector.e.x);
                    floatArray.add(Intersector.e.y);
                } else if (Intersector.pointLineSide(ep2, ep1, s) > 0) {
                    Intersector.intersectLines(s, e, ep1, ep2, ip);
                    floatArray.add(Intersector.ip.x);
                    floatArray.add(Intersector.ip.y);
                }
                s.set(Intersector.e.x, Intersector.e.y);
            }
            floatArray2.clear();
            floatArray2.addAll(floatArray);
            floatArray.clear();
        }
        if (Intersector.floatArray2.size != 0) {
            overlap.setVertices(floatArray2.toArray());
            return true;
        }
        return false;
    }

    public static float distanceLinePoint(float startX, float startY, float endX, float endY, float pointX, float pointY) {
        float normalLength = (float)Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
        return Math.abs((pointX - startX) * (endY - startY) - (pointY - startY) * (endX - startX)) / normalLength;
    }

    public static float distanceSegmentPoint(float startX, float startY, float endX, float endY, float pointX, float pointY) {
        return Intersector.nearestSegmentPoint(startX, startY, endX, endY, pointX, pointY, v2tmp).dst(pointX, pointY);
    }

    public static float distanceSegmentPoint(Vector2 start, Vector2 end, Vector2 point) {
        return Intersector.nearestSegmentPoint(start, end, point, v2tmp).dst(point);
    }

    public static Vector2 nearestSegmentPoint(Vector2 start, Vector2 end, Vector2 point, Vector2 nearest) {
        float length2 = start.dst2(end);
        if (length2 == 0.0f) {
            return nearest.set(start);
        }
        float t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / length2;
        if (t < 0.0f) {
            return nearest.set(start);
        }
        if (t > 1.0f) {
            return nearest.set(end);
        }
        return nearest.set(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y));
    }

    public static Vector2 nearestSegmentPoint(float startX, float startY, float endX, float endY, float pointX, float pointY, Vector2 nearest) {
        float xDiff = endX - startX;
        float yDiff = endY - startY;
        float length2 = xDiff * xDiff + yDiff * yDiff;
        if (length2 == 0.0f) {
            return nearest.set(startX, startY);
        }
        float t = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY)) / length2;
        if (t < 0.0f) {
            return nearest.set(startX, startY);
        }
        if (t > 1.0f) {
            return nearest.set(endX, endY);
        }
        return nearest.set(startX + t * (endX - startX), startY + t * (endY - startY));
    }

    public static boolean intersectSegmentCircle(Vector2 start, Vector2 end, Vector2 center, float squareRadius) {
        tmp.set(end.x - start.x, end.y - start.y, 0.0f);
        tmp1.set(center.x - start.x, center.y - start.y, 0.0f);
        float l = tmp.len();
        float u = tmp1.dot(tmp.nor());
        if (u <= 0.0f) {
            tmp2.set(start.x, start.y, 0.0f);
        } else if (u >= l) {
            tmp2.set(end.x, end.y, 0.0f);
        } else {
            tmp3.set(tmp.scl(u));
            tmp2.set(Intersector.tmp3.x + start.x, Intersector.tmp3.y + start.y, 0.0f);
        }
        float x = center.x - Intersector.tmp2.x;
        float y = center.y - Intersector.tmp2.y;
        return x * x + y * y <= squareRadius;
    }

    public static float intersectSegmentCircleDisplace(Vector2 start, Vector2 end, Vector2 point, float radius, Vector2 displacement) {
        float u = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y);
        float d = start.dst(end);
        if ((u /= d * d) < 0.0f || u > 1.0f) {
            return Float.POSITIVE_INFINITY;
        }
        tmp.set(end.x, end.y, 0.0f).sub(start.x, start.y, 0.0f);
        tmp2.set(start.x, start.y, 0.0f).add(tmp.scl(u));
        d = tmp2.dst(point.x, point.y, 0.0f);
        if (d < radius) {
            displacement.set(point).sub(Intersector.tmp2.x, Intersector.tmp2.y).nor();
            return d;
        }
        return Float.POSITIVE_INFINITY;
    }

    public static float intersectRayRay(Vector2 start1, Vector2 direction1, Vector2 start2, Vector2 direction2) {
        float difx = start2.x - start1.x;
        float dify = start2.y - start1.y;
        float d1xd2 = direction1.x * direction2.y - direction1.y * direction2.x;
        if (d1xd2 == 0.0f) {
            return Float.POSITIVE_INFINITY;
        }
        float d2sx = direction2.x / d1xd2;
        float d2sy = direction2.y / d1xd2;
        return difx * d2sy - dify * d2sx;
    }

    public static boolean intersectRayPlane(Ray ray, Plane plane, Vector3 intersection) {
        float denom = ray.direction.dot(plane.getNormal());
        if (denom != 0.0f) {
            float t = -(ray.origin.dot(plane.getNormal()) + plane.getD()) / denom;
            if (t < 0.0f) {
                return false;
            }
            if (intersection != null) {
                intersection.set(ray.origin).add(v0.set(ray.direction).scl(t));
            }
            return true;
        }
        if (plane.testPoint(ray.origin) == Plane.PlaneSide.OnPlane) {
            if (intersection != null) {
                intersection.set(ray.origin);
            }
            return true;
        }
        return false;
    }

    public static float intersectLinePlane(float x, float y, float z, float x2, float y2, float z2, Plane plane, Vector3 intersection) {
        Vector3 direction = tmp.set(x2, y2, z2).sub(x, y, z);
        Vector3 origin = tmp2.set(x, y, z);
        float denom = direction.dot(plane.getNormal());
        if (denom != 0.0f) {
            float t = -(origin.dot(plane.getNormal()) + plane.getD()) / denom;
            if (intersection != null) {
                intersection.set(origin).add(direction.scl(t));
            }
            return t;
        }
        if (plane.testPoint(origin) == Plane.PlaneSide.OnPlane) {
            if (intersection != null) {
                intersection.set(origin);
            }
            return 0.0f;
        }
        return -1.0f;
    }

    public static boolean intersectRayTriangle(Ray ray, Vector3 t1, Vector3 t2, Vector3 t3, Vector3 intersection) {
        Vector3 edge1 = v0.set(t2).sub(t1);
        Vector3 edge2 = v1.set(t3).sub(t1);
        Vector3 pvec = v2.set(ray.direction).crs(edge2);
        float det = edge1.dot(pvec);
        if (MathUtils.isZero(det)) {
            p.set(t1, t2, t3);
            if (p.testPoint(ray.origin) == Plane.PlaneSide.OnPlane && Intersector.isPointInTriangle(ray.origin, t1, t2, t3)) {
                if (intersection != null) {
                    intersection.set(ray.origin);
                }
                return true;
            }
            return false;
        }
        det = 1.0f / det;
        Vector3 tvec = i.set(ray.origin).sub(t1);
        float u = tvec.dot(pvec) * det;
        if (u < 0.0f || u > 1.0f) {
            return false;
        }
        Vector3 qvec = tvec.crs(edge1);
        float v = ray.direction.dot(qvec) * det;
        if (v < 0.0f || u + v > 1.0f) {
            return false;
        }
        float t = edge2.dot(qvec) * det;
        if (t < 0.0f) {
            return false;
        }
        if (intersection != null) {
            if (t <= 1.0E-6f) {
                intersection.set(ray.origin);
            } else {
                ray.getEndPoint(intersection, t);
            }
        }
        return true;
    }

    public static boolean intersectRaySphere(Ray ray, Vector3 center, float radius, Vector3 intersection) {
        float r2;
        float len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z);
        if (len < 0.0f) {
            return false;
        }
        float dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len, ray.origin.z + ray.direction.z * len);
        if (dst2 > (r2 = radius * radius)) {
            return false;
        }
        if (intersection != null) {
            intersection.set(ray.direction).scl(len - (float)Math.sqrt(r2 - dst2)).add(ray.origin);
        }
        return true;
    }

    public static boolean intersectRayBounds(Ray ray, BoundingBox box, Vector3 intersection) {
        float t;
        if (box.contains(ray.origin)) {
            if (intersection != null) {
                intersection.set(ray.origin);
            }
            return true;
        }
        float lowest = 0.0f;
        boolean hit = false;
        if (ray.origin.x <= box.min.x && ray.direction.x > 0.0f && (t = (box.min.x - ray.origin.x) / ray.direction.x) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.y >= box.min.y && Intersector.v2.y <= box.max.y && Intersector.v2.z >= box.min.z && Intersector.v2.z <= box.max.z && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (ray.origin.x >= box.max.x && ray.direction.x < 0.0f && (t = (box.max.x - ray.origin.x) / ray.direction.x) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.y >= box.min.y && Intersector.v2.y <= box.max.y && Intersector.v2.z >= box.min.z && Intersector.v2.z <= box.max.z && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (ray.origin.y <= box.min.y && ray.direction.y > 0.0f && (t = (box.min.y - ray.origin.y) / ray.direction.y) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.x >= box.min.x && Intersector.v2.x <= box.max.x && Intersector.v2.z >= box.min.z && Intersector.v2.z <= box.max.z && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (ray.origin.y >= box.max.y && ray.direction.y < 0.0f && (t = (box.max.y - ray.origin.y) / ray.direction.y) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.x >= box.min.x && Intersector.v2.x <= box.max.x && Intersector.v2.z >= box.min.z && Intersector.v2.z <= box.max.z && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (ray.origin.z <= box.min.z && ray.direction.z > 0.0f && (t = (box.min.z - ray.origin.z) / ray.direction.z) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.x >= box.min.x && Intersector.v2.x <= box.max.x && Intersector.v2.y >= box.min.y && Intersector.v2.y <= box.max.y && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (ray.origin.z >= box.max.z && ray.direction.z < 0.0f && (t = (box.max.z - ray.origin.z) / ray.direction.z) >= 0.0f) {
            v2.set(ray.direction).scl(t).add(ray.origin);
            if (Intersector.v2.x >= box.min.x && Intersector.v2.x <= box.max.x && Intersector.v2.y >= box.min.y && Intersector.v2.y <= box.max.y && (!hit || t < lowest)) {
                hit = true;
                lowest = t;
            }
        }
        if (hit && intersection != null) {
            intersection.set(ray.direction).scl(lowest).add(ray.origin);
            if (intersection.x < box.min.x) {
                intersection.x = box.min.x;
            } else if (intersection.x > box.max.x) {
                intersection.x = box.max.x;
            }
            if (intersection.y < box.min.y) {
                intersection.y = box.min.y;
            } else if (intersection.y > box.max.y) {
                intersection.y = box.max.y;
            }
            if (intersection.z < box.min.z) {
                intersection.z = box.min.z;
            } else if (intersection.z > box.max.z) {
                intersection.z = box.max.z;
            }
        }
        return hit;
    }

    public static boolean intersectRayBoundsFast(Ray ray, BoundingBox box) {
        return Intersector.intersectRayBoundsFast(ray, box.getCenter(tmp1), box.getDimensions(tmp2));
    }

    public static boolean intersectRayBoundsFast(Ray ray, Vector3 center, Vector3 dimensions) {
        float maxz;
        float minz;
        float maxy;
        float miny;
        float divX = 1.0f / ray.direction.x;
        float divY = 1.0f / ray.direction.y;
        float divZ = 1.0f / ray.direction.z;
        float minx = (center.x - dimensions.x * 0.5f - ray.origin.x) * divX;
        float maxx = (center.x + dimensions.x * 0.5f - ray.origin.x) * divX;
        if (minx > maxx) {
            float t = minx;
            minx = maxx;
            maxx = t;
        }
        if ((miny = (center.y - dimensions.y * 0.5f - ray.origin.y) * divY) > (maxy = (center.y + dimensions.y * 0.5f - ray.origin.y) * divY)) {
            float t = miny;
            miny = maxy;
            maxy = t;
        }
        if ((minz = (center.z - dimensions.z * 0.5f - ray.origin.z) * divZ) > (maxz = (center.z + dimensions.z * 0.5f - ray.origin.z) * divZ)) {
            float t = minz;
            minz = maxz;
            maxz = t;
        }
        float min = Math.max(Math.max(minx, miny), minz);
        float max = Math.min(Math.min(maxx, maxy), maxz);
        return max >= 0.0f && max >= min;
    }

    public static boolean intersectRayTriangles(Ray ray, float[] triangles, Vector3 intersection) {
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;
        if (triangles.length / 3 % 3 != 0) {
            throw new RuntimeException("triangle list size is not a multiple of 3");
        }
        for (int i = 0; i < triangles.length - 6; i += 9) {
            float dist;
            boolean result = Intersector.intersectRayTriangle(ray, tmp1.set(triangles[i], triangles[i + 1], triangles[i + 2]), tmp2.set(triangles[i + 3], triangles[i + 4], triangles[i + 5]), tmp3.set(triangles[i + 6], triangles[i + 7], triangles[i + 8]), tmp);
            if (!result || !((dist = ray.origin.dst2(tmp)) < min_dist)) continue;
            min_dist = dist;
            best.set(tmp);
            hit = true;
        }
        if (!hit) {
            return false;
        }
        if (intersection != null) {
            intersection.set(best);
        }
        return true;
    }

    public static boolean intersectRayTriangles(Ray ray, float[] vertices, short[] indices, int vertexSize, Vector3 intersection) {
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;
        if (indices.length % 3 != 0) {
            throw new RuntimeException("triangle list size is not a multiple of 3");
        }
        for (int i = 0; i < indices.length; i += 3) {
            float dist;
            int i1 = indices[i] * vertexSize;
            int i2 = indices[i + 1] * vertexSize;
            int i3 = indices[i + 2] * vertexSize;
            boolean result = Intersector.intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]), tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]), tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp);
            if (!result || !((dist = ray.origin.dst2(tmp)) < min_dist)) continue;
            min_dist = dist;
            best.set(tmp);
            hit = true;
        }
        if (!hit) {
            return false;
        }
        if (intersection != null) {
            intersection.set(best);
        }
        return true;
    }

    public static boolean intersectRayTriangles(Ray ray, List<Vector3> triangles, Vector3 intersection) {
        float min_dist = Float.MAX_VALUE;
        boolean hit = false;
        if (triangles.size() % 3 != 0) {
            throw new RuntimeException("triangle list size is not a multiple of 3");
        }
        for (int i = 0; i < triangles.size() - 2; i += 3) {
            float dist;
            boolean result = Intersector.intersectRayTriangle(ray, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2), tmp);
            if (!result || !((dist = ray.origin.dst2(tmp)) < min_dist)) continue;
            min_dist = dist;
            best.set(tmp);
            hit = true;
        }
        if (!hit) {
            return false;
        }
        if (intersection != null) {
            intersection.set(best);
        }
        return true;
    }

    public static boolean intersectLines(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4, Vector2 intersection) {
        float y4 = p4.y;
        float y3 = p3.y;
        float x2 = p2.x;
        float x1 = p1.x;
        float x4 = p4.x;
        float x3 = p3.x;
        float y2 = p2.y;
        float y1 = p1.y;
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0f) {
            return false;
        }
        if (intersection != null) {
            float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d;
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    public static boolean intersectLines(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2 intersection) {
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0f) {
            return false;
        }
        if (intersection != null) {
            float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / d;
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    public static boolean intersectLinePolygon(Vector2 p1, Vector2 p2, Polygon polygon) {
        float[] vertices = polygon.getTransformedVertices();
        float x1 = p1.x;
        float y1 = p1.y;
        float x2 = p2.x;
        float y2 = p2.y;
        int n = vertices.length;
        float x3 = vertices[n - 2];
        float y3 = vertices[n - 1];
        for (int i = 0; i < n; i += 2) {
            float xd;
            float yd;
            float ua;
            float y4 = vertices[i + 1];
            float x4 = vertices[i];
            float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if (d != 0.0f && (ua = ((x4 - x3) * (yd = y1 - y3) - (y4 - y3) * (xd = x1 - x3)) / d) >= 0.0f && ua <= 1.0f) {
                return true;
            }
            x3 = x4;
            y3 = y4;
        }
        return false;
    }

    public static boolean intersectRectangles(Rectangle rectangle1, Rectangle rectangle2, Rectangle intersection) {
        if (rectangle1.overlaps(rectangle2)) {
            intersection.x = Math.max(rectangle1.x, rectangle2.x);
            intersection.width = Math.min(rectangle1.x + rectangle1.width, rectangle2.x + rectangle2.width) - intersection.x;
            intersection.y = Math.max(rectangle1.y, rectangle2.y);
            intersection.height = Math.min(rectangle1.y + rectangle1.height, rectangle2.y + rectangle2.height) - intersection.y;
            return true;
        }
        return false;
    }

    public static boolean intersectSegmentPolygon(Vector2 p1, Vector2 p2, Polygon polygon) {
        float[] vertices = polygon.getTransformedVertices();
        float x1 = p1.x;
        float y1 = p1.y;
        float x2 = p2.x;
        float y2 = p2.y;
        int n = vertices.length;
        float x3 = vertices[n - 2];
        float y3 = vertices[n - 1];
        for (int i = 0; i < n; i += 2) {
            float ub;
            float xd;
            float yd;
            float ua;
            float y4 = vertices[i + 1];
            float x4 = vertices[i];
            float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if (d != 0.0f && (ua = ((x4 - x3) * (yd = y1 - y3) - (y4 - y3) * (xd = x1 - x3)) / d) >= 0.0f && ua <= 1.0f && (ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d) >= 0.0f && ub <= 1.0f) {
                return true;
            }
            x3 = x4;
            y3 = y4;
        }
        return false;
    }

    public static boolean intersectSegments(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4, Vector2 intersection) {
        float y4 = p4.y;
        float y3 = p3.y;
        float x2 = p2.x;
        float x1 = p1.x;
        float x4 = p4.x;
        float x3 = p3.x;
        float y2 = p2.y;
        float y1 = p1.y;
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0f) {
            return false;
        }
        float yd = y1 - y3;
        float xd = x1 - x3;
        float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
        if (ua < 0.0f || ua > 1.0f) {
            return false;
        }
        float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
        if (ub < 0.0f || ub > 1.0f) {
            return false;
        }
        if (intersection != null) {
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    public static boolean intersectSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2 intersection) {
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0f) {
            return false;
        }
        float yd = y1 - y3;
        float xd = x1 - x3;
        float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
        if (ua < 0.0f || ua > 1.0f) {
            return false;
        }
        float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
        if (ub < 0.0f || ub > 1.0f) {
            return false;
        }
        if (intersection != null) {
            intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
        }
        return true;
    }

    static float det(float a, float b, float c, float d) {
        return a * d - b * c;
    }

    static double detd(double a, double b, double c, double d) {
        return a * d - b * c;
    }

    public static boolean overlaps(Circle c1, Circle c2) {
        return c1.overlaps(c2);
    }

    public static boolean overlaps(Rectangle r1, Rectangle r2) {
        return r1.overlaps(r2);
    }

    public static boolean overlaps(Circle c, Rectangle r) {
        float closestX = c.x;
        float closestY = c.y;
        if (c.x < r.x) {
            closestX = r.x;
        } else if (c.x > r.x + r.width) {
            closestX = r.x + r.width;
        }
        if (c.y < r.y) {
            closestY = r.y;
        } else if (c.y > r.y + r.height) {
            closestY = r.y + r.height;
        }
        closestX -= c.x;
        closestX *= closestX;
        closestY -= c.y;
        closestY *= closestY;
        return closestX + closestY < c.radius * c.radius;
    }

    public static boolean overlapConvexPolygons(Polygon p1, Polygon p2) {
        return Intersector.overlapConvexPolygons(p1, p2, null);
    }

    public static boolean overlapConvexPolygons(Polygon p1, Polygon p2, MinimumTranslationVector mtv) {
        return Intersector.overlapConvexPolygons(p1.getTransformedVertices(), p2.getTransformedVertices(), mtv);
    }

    public static boolean overlapConvexPolygons(float[] verts1, float[] verts2, MinimumTranslationVector mtv) {
        return Intersector.overlapConvexPolygons(verts1, 0, verts1.length, verts2, 0, verts2.length, mtv);
    }

    public static boolean overlapConvexPolygons(float[] verts1, int offset1, int count1, float[] verts2, int offset2, int count2, MinimumTranslationVector mtv) {
        float maxs;
        float mins;
        float o;
        float p;
        float max2;
        float min2;
        int numInNormalDir;
        float p2;
        float max1;
        float min1;
        float length;
        float axisY;
        float axisX;
        float y2;
        float x2;
        float y1;
        float x1;
        int i;
        float overlap = Float.MAX_VALUE;
        float smallestAxisX = 0.0f;
        float smallestAxisY = 0.0f;
        int end1 = offset1 + count1;
        int end2 = offset2 + count2;
        for (i = offset1; i < end1; i += 2) {
            x1 = verts1[i];
            y1 = verts1[i + 1];
            x2 = verts1[(i + 2) % count1];
            y2 = verts1[(i + 3) % count1];
            axisX = y1 - y2;
            axisY = -(x1 - x2);
            length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
            max1 = min1 = (axisX /= length) * verts1[0] + (axisY /= length) * verts1[1];
            for (int j = offset1; j < end1; j += 2) {
                p2 = axisX * verts1[j] + axisY * verts1[j + 1];
                if (p2 < min1) {
                    min1 = p2;
                    continue;
                }
                if (!(p2 > max1)) continue;
                max1 = p2;
            }
            numInNormalDir = 0;
            max2 = min2 = axisX * verts2[0] + axisY * verts2[1];
            for (int j = offset2; j < end2; j += 2) {
                numInNormalDir -= Intersector.pointLineSide(x1, y1, x2, y2, verts2[j], verts2[j + 1]);
                p = axisX * verts2[j] + axisY * verts2[j + 1];
                if (p < min2) {
                    min2 = p;
                    continue;
                }
                if (!(p > max2)) continue;
                max2 = p;
            }
            if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
                return false;
            }
            o = Math.min(max1, max2) - Math.max(min1, min2);
            if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
                mins = Math.abs(min1 - min2);
                o = mins < (maxs = Math.abs(max1 - max2)) ? (o += mins) : (o += maxs);
            }
            if (!(o < overlap)) continue;
            overlap = o;
            smallestAxisX = numInNormalDir >= 0 ? axisX : -axisX;
            smallestAxisY = numInNormalDir >= 0 ? axisY : -axisY;
        }
        for (i = offset2; i < end2; i += 2) {
            x1 = verts2[i];
            y1 = verts2[i + 1];
            x2 = verts2[(i + 2) % count2];
            y2 = verts2[(i + 3) % count2];
            axisX = y1 - y2;
            axisY = -(x1 - x2);
            length = (float)Math.sqrt(axisX * axisX + axisY * axisY);
            numInNormalDir = 0;
            max1 = min1 = (axisX /= length) * verts1[0] + (axisY /= length) * verts1[1];
            for (int j = offset1; j < end1; j += 2) {
                p2 = axisX * verts1[j] + axisY * verts1[j + 1];
                numInNormalDir -= Intersector.pointLineSide(x1, y1, x2, y2, verts1[j], verts1[j + 1]);
                if (p2 < min1) {
                    min1 = p2;
                    continue;
                }
                if (!(p2 > max1)) continue;
                max1 = p2;
            }
            max2 = min2 = axisX * verts2[0] + axisY * verts2[1];
            for (int j = offset2; j < end2; j += 2) {
                p = axisX * verts2[j] + axisY * verts2[j + 1];
                if (p < min2) {
                    min2 = p;
                    continue;
                }
                if (!(p > max2)) continue;
                max2 = p;
            }
            if (!(min1 <= min2 && max1 >= min2 || min2 <= min1 && max2 >= min1)) {
                return false;
            }
            o = Math.min(max1, max2) - Math.max(min1, min2);
            if (min1 < min2 && max1 > max2 || min2 < min1 && max2 > max1) {
                mins = Math.abs(min1 - min2);
                o = mins < (maxs = Math.abs(max1 - max2)) ? (o += mins) : (o += maxs);
            }
            if (!(o < overlap)) continue;
            overlap = o;
            smallestAxisX = numInNormalDir < 0 ? axisX : -axisX;
            smallestAxisY = numInNormalDir < 0 ? axisY : -axisY;
        }
        if (mtv != null) {
            mtv.normal.set(smallestAxisX, smallestAxisY);
            mtv.depth = overlap;
        }
        return true;
    }

    public static void splitTriangle(float[] triangle, Plane plane, SplitTriangle split) {
        int stride = triangle.length / 3;
        boolean r1 = plane.testPoint(triangle[0], triangle[1], triangle[2]) == Plane.PlaneSide.Back;
        boolean r2 = plane.testPoint(triangle[0 + stride], triangle[1 + stride], triangle[2 + stride]) == Plane.PlaneSide.Back;
        boolean r3 = plane.testPoint(triangle[0 + stride * 2], triangle[1 + stride * 2], triangle[2 + stride * 2]) == Plane.PlaneSide.Back;
        split.reset();
        if (r1 == r2 && r2 == r3) {
            split.total = 1;
            if (r1) {
                split.numBack = 1;
                System.arraycopy(triangle, 0, split.back, 0, triangle.length);
            } else {
                split.numFront = 1;
                System.arraycopy(triangle, 0, split.front, 0, triangle.length);
            }
            return;
        }
        split.total = 3;
        split.numFront = (r1 ? 0 : 1) + (r2 ? 0 : 1) + (r3 ? 0 : 1);
        split.numBack = split.total - split.numFront;
        split.setSide(!r1);
        int first = 0;
        int second = stride;
        if (r1 != r2) {
            Intersector.splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        } else {
            split.add(triangle, first, stride);
        }
        first = stride;
        second = stride + stride;
        if (r2 != r3) {
            Intersector.splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        } else {
            split.add(triangle, first, stride);
        }
        first = stride + stride;
        second = 0;
        if (r3 != r1) {
            Intersector.splitEdge(triangle, first, second, stride, plane, split.edgeSplit, 0);
            split.add(triangle, first, stride);
            split.add(split.edgeSplit, 0, stride);
            split.setSide(!split.getSide());
            split.add(split.edgeSplit, 0, stride);
        } else {
            split.add(triangle, first, stride);
        }
        if (split.numFront == 2) {
            System.arraycopy(split.front, stride * 2, split.front, stride * 3, stride * 2);
            System.arraycopy(split.front, 0, split.front, stride * 5, stride);
        } else {
            System.arraycopy(split.back, stride * 2, split.back, stride * 3, stride * 2);
            System.arraycopy(split.back, 0, split.back, stride * 5, stride);
        }
    }

    private static void splitEdge(float[] vertices, int s, int e, int stride, Plane plane, float[] split, int offset) {
        float t = Intersector.intersectLinePlane(vertices[s], vertices[s + 1], vertices[s + 2], vertices[e], vertices[e + 1], vertices[e + 2], plane, intersection);
        split[offset + 0] = Intersector.intersection.x;
        split[offset + 1] = Intersector.intersection.y;
        split[offset + 2] = Intersector.intersection.z;
        for (int i = 3; i < stride; ++i) {
            float a = vertices[s + i];
            float b = vertices[e + i];
            split[offset + i] = a + t * (b - a);
        }
    }

    public static class MinimumTranslationVector {
        public Vector2 normal = new Vector2();
        public float depth = 0.0f;
    }

    public static class SplitTriangle {
        public float[] front;
        public float[] back;
        float[] edgeSplit;
        public int numFront;
        public int numBack;
        public int total;
        boolean frontCurrent = false;
        int frontOffset = 0;
        int backOffset = 0;

        public SplitTriangle(int numAttributes) {
            this.front = new float[numAttributes * 3 * 2];
            this.back = new float[numAttributes * 3 * 2];
            this.edgeSplit = new float[numAttributes];
        }

        public String toString() {
            return "SplitTriangle [front=" + Arrays.toString(this.front) + ", back=" + Arrays.toString(this.back) + ", numFront=" + this.numFront + ", numBack=" + this.numBack + ", total=" + this.total + "]";
        }

        void setSide(boolean front) {
            this.frontCurrent = front;
        }

        boolean getSide() {
            return this.frontCurrent;
        }

        void add(float[] vertex, int offset, int stride) {
            if (this.frontCurrent) {
                System.arraycopy(vertex, offset, this.front, this.frontOffset, stride);
                this.frontOffset += stride;
            } else {
                System.arraycopy(vertex, offset, this.back, this.backOffset, stride);
                this.backOffset += stride;
            }
        }

        void reset() {
            this.frontCurrent = false;
            this.frontOffset = 0;
            this.backOffset = 0;
            this.numFront = 0;
            this.numBack = 0;
            this.total = 0;
        }
    }
}

