/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public abstract class ParticleSorter {
    static final Vector3 TMP_V1 = new Vector3();
    protected Camera camera;

    public abstract <T extends ParticleControllerRenderData> int[] sort(Array<T> var1);

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void ensureCapacity(int capacity) {
    }

    public static class Distance
    extends ParticleSorter {
        private float[] distances;
        private int[] particleIndices;
        private int[] particleOffsets;
        private int currentSize = 0;

        @Override
        public void ensureCapacity(int capacity) {
            if (this.currentSize < capacity) {
                this.distances = new float[capacity];
                this.particleIndices = new int[capacity];
                this.particleOffsets = new int[capacity];
                this.currentSize = capacity;
            }
        }

        @Override
        public <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData) {
            float[] val = this.camera.view.val;
            float cx = val[2];
            float cy = val[6];
            float cz = val[10];
            int count = 0;
            int i = 0;
            for (ParticleControllerRenderData data : renderData) {
                int k = 0;
                int c = i + data.controller.particles.size;
                while (i < c) {
                    this.distances[i] = cx * data.positionChannel.data[k + 0] + cy * data.positionChannel.data[k + 1] + cz * data.positionChannel.data[k + 2];
                    this.particleIndices[i] = i;
                    ++i;
                    k += data.positionChannel.strideSize;
                }
                count += data.controller.particles.size;
            }
            this.qsort(0, count - 1);
            for (i = 0; i < count; ++i) {
                this.particleOffsets[this.particleIndices[i]] = i;
            }
            return this.particleOffsets;
        }

        public void qsort(int si, int ei) {
            if (si < ei) {
                if (ei - si <= 8) {
                    for (int i = si; i <= ei; ++i) {
                        for (int j = i; j > si && this.distances[j - 1] > this.distances[j]; --j) {
                            float tmp = this.distances[j];
                            this.distances[j] = this.distances[j - 1];
                            this.distances[j - 1] = tmp;
                            int tmpIndex = this.particleIndices[j];
                            this.particleIndices[j] = this.particleIndices[j - 1];
                            this.particleIndices[j - 1] = tmpIndex;
                        }
                    }
                    return;
                }
                float pivot = this.distances[si];
                int i = si + 1;
                int particlesPivotIndex = this.particleIndices[si];
                for (int j = si + 1; j <= ei; ++j) {
                    if (!(pivot > this.distances[j])) continue;
                    if (j > i) {
                        float tmp = this.distances[j];
                        this.distances[j] = this.distances[i];
                        this.distances[i] = tmp;
                        int tmpIndex = this.particleIndices[j];
                        this.particleIndices[j] = this.particleIndices[i];
                        this.particleIndices[i] = tmpIndex;
                    }
                    ++i;
                }
                this.distances[si] = this.distances[i - 1];
                this.distances[i - 1] = pivot;
                this.particleIndices[si] = this.particleIndices[i - 1];
                this.particleIndices[i - 1] = particlesPivotIndex;
                this.qsort(si, i - 2);
                this.qsort(i, ei);
            }
        }
    }

    public static class None
    extends ParticleSorter {
        int currentCapacity = 0;
        int[] indices;

        @Override
        public void ensureCapacity(int capacity) {
            if (this.currentCapacity < capacity) {
                this.indices = new int[capacity];
                for (int i = 0; i < capacity; ++i) {
                    this.indices[i] = i;
                }
                this.currentCapacity = capacity;
            }
        }

        @Override
        public <T extends ParticleControllerRenderData> int[] sort(Array<T> renderData) {
            return this.indices;
        }
    }
}

