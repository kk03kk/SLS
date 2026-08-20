/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;

public class ConvexHull {
    private final IntArray quicksortStack = new IntArray();
    private float[] sortedPoints;
    private final FloatArray hull = new FloatArray();
    private final IntArray indices = new IntArray();
    private final ShortArray originalIndices = new ShortArray(false, 0);

    public FloatArray computePolygon(FloatArray points, boolean sorted) {
        return this.computePolygon(points.items, 0, points.size, sorted);
    }

    public FloatArray computePolygon(float[] polygon, boolean sorted) {
        return this.computePolygon(polygon, 0, polygon.length, sorted);
    }

    public FloatArray computePolygon(float[] points, int offset, int count, boolean sorted) {
        int i;
        int end = offset + count;
        if (!sorted) {
            if (this.sortedPoints == null || this.sortedPoints.length < count) {
                this.sortedPoints = new float[count];
            }
            System.arraycopy(points, offset, this.sortedPoints, 0, count);
            points = this.sortedPoints;
            offset = 0;
            this.sort(points, count);
        }
        FloatArray hull = this.hull;
        hull.clear();
        for (i = offset; i < end; i += 2) {
            float x = points[i];
            float y = points[i + 1];
            while (hull.size >= 4 && this.ccw(x, y) <= 0.0f) {
                hull.size -= 2;
            }
            hull.add(x);
            hull.add(y);
        }
        int t = hull.size + 2;
        for (i = end - 4; i >= offset; i -= 2) {
            float x = points[i];
            float y = points[i + 1];
            while (hull.size >= t && this.ccw(x, y) <= 0.0f) {
                hull.size -= 2;
            }
            hull.add(x);
            hull.add(y);
        }
        return hull;
    }

    public IntArray computeIndices(FloatArray points, boolean sorted, boolean yDown) {
        return this.computeIndices(points.items, 0, points.size, sorted, yDown);
    }

    public IntArray computeIndices(float[] polygon, boolean sorted, boolean yDown) {
        return this.computeIndices(polygon, 0, polygon.length, sorted, yDown);
    }

    public IntArray computeIndices(float[] points, int offset, int count, boolean sorted, boolean yDown) {
        int end = offset + count;
        if (!sorted) {
            if (this.sortedPoints == null || this.sortedPoints.length < count) {
                this.sortedPoints = new float[count];
            }
            System.arraycopy(points, offset, this.sortedPoints, 0, count);
            points = this.sortedPoints;
            offset = 0;
            this.sortWithIndices(points, count, yDown);
        }
        IntArray indices = this.indices;
        indices.clear();
        FloatArray hull = this.hull;
        hull.clear();
        int i = offset;
        int index = i / 2;
        while (i < end) {
            float x = points[i];
            float y = points[i + 1];
            while (hull.size >= 4 && this.ccw(x, y) <= 0.0f) {
                hull.size -= 2;
                --indices.size;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
            i += 2;
            ++index;
        }
        i = end - 4;
        index = i / 2;
        int t = hull.size + 2;
        while (i >= offset) {
            float x = points[i];
            float y = points[i + 1];
            while (hull.size >= t && this.ccw(x, y) <= 0.0f) {
                hull.size -= 2;
                --indices.size;
            }
            hull.add(x);
            hull.add(y);
            indices.add(index);
            i -= 2;
            --index;
        }
        if (!sorted) {
            short[] originalIndicesArray = this.originalIndices.items;
            int[] indicesArray = indices.items;
            int n = indices.size;
            for (int i2 = 0; i2 < n; ++i2) {
                indicesArray[i2] = originalIndicesArray[indicesArray[i2]];
            }
        }
        return indices;
    }

    private float ccw(float p3x, float p3y) {
        FloatArray hull = this.hull;
        int size = hull.size;
        float p1x = hull.get(size - 4);
        float p1y = hull.get(size - 3);
        float p2x = hull.get(size - 2);
        float p2y = hull.peek();
        return (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x);
    }

    private void sort(float[] values, int count) {
        int lower = 0;
        int upper = count - 1;
        IntArray stack = this.quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while (stack.size > 0) {
            upper = stack.pop();
            if (upper <= (lower = stack.pop())) continue;
            int i = this.quicksortPartition(values, lower, upper);
            if (i - lower > upper - i) {
                stack.add(lower);
                stack.add(i - 2);
            }
            stack.add(i + 2);
            stack.add(upper);
            if (upper - i < i - lower) continue;
            stack.add(lower);
            stack.add(i - 2);
        }
    }

    private int quicksortPartition(float[] values, int lower, int upper) {
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        while (down < up) {
            while (down < up && values[down] <= x) {
                down += 2;
            }
            while (values[up] > x || values[up] == x && values[up + 1] < y) {
                up -= 2;
            }
            if (down >= up) continue;
            float temp = values[down];
            values[down] = values[up];
            values[up] = temp;
            temp = values[down + 1];
            values[down + 1] = values[up + 1];
            values[up + 1] = temp;
        }
        values[lower] = values[up];
        values[up] = x;
        values[lower + 1] = values[up + 1];
        values[up + 1] = y;
        return up;
    }

    private void sortWithIndices(float[] values, int count, boolean yDown) {
        int pointCount = count / 2;
        this.originalIndices.clear();
        this.originalIndices.ensureCapacity(pointCount);
        short[] originalIndicesArray = this.originalIndices.items;
        for (int i = 0; i < pointCount; i = (int)((short)(i + 1))) {
            originalIndicesArray[i] = i;
        }
        int lower = 0;
        int upper = count - 1;
        IntArray stack = this.quicksortStack;
        stack.add(lower);
        stack.add(upper - 1);
        while (stack.size > 0) {
            upper = stack.pop();
            if (upper <= (lower = stack.pop())) continue;
            int i = this.quicksortPartitionWithIndices(values, lower, upper, yDown, originalIndicesArray);
            if (i - lower > upper - i) {
                stack.add(lower);
                stack.add(i - 2);
            }
            stack.add(i + 2);
            stack.add(upper);
            if (upper - i < i - lower) continue;
            stack.add(lower);
            stack.add(i - 2);
        }
    }

    private int quicksortPartitionWithIndices(float[] values, int lower, int upper, boolean yDown, short[] originalIndices) {
        short tempIndex;
        float x = values[lower];
        float y = values[lower + 1];
        int up = upper;
        int down = lower;
        while (down < up) {
            while (down < up && values[down] <= x) {
                down += 2;
            }
            if (yDown) {
                while (values[up] > x || values[up] == x && values[up + 1] < y) {
                    up -= 2;
                }
            } else {
                while (values[up] > x || values[up] == x && values[up + 1] > y) {
                    up -= 2;
                }
            }
            if (down >= up) continue;
            float temp = values[down];
            values[down] = values[up];
            values[up] = temp;
            temp = values[down + 1];
            values[down + 1] = values[up + 1];
            values[up + 1] = temp;
            tempIndex = originalIndices[down / 2];
            originalIndices[down / 2] = originalIndices[up / 2];
            originalIndices[up / 2] = tempIndex;
        }
        values[lower] = values[up];
        values[up] = x;
        values[lower + 1] = values[up + 1];
        values[up + 1] = y;
        tempIndex = originalIndices[lower / 2];
        originalIndices[lower / 2] = originalIndices[up / 2];
        originalIndices[up / 2] = tempIndex;
        return up;
    }
}

