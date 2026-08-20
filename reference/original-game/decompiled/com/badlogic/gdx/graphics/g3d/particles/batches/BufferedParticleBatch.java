/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.batches;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSorter;
import com.badlogic.gdx.graphics.g3d.particles.batches.ParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderData;
import com.badlogic.gdx.utils.Array;

public abstract class BufferedParticleBatch<T extends ParticleControllerRenderData>
implements ParticleBatch<T> {
    protected Array<T> renderData;
    protected int bufferedParticlesCount;
    protected int currentCapacity = 0;
    protected ParticleSorter sorter = new ParticleSorter.Distance();
    protected Camera camera;

    protected BufferedParticleBatch(Class<T> type) {
        this.renderData = new Array(false, 10, type);
    }

    @Override
    public void begin() {
        this.renderData.clear();
        this.bufferedParticlesCount = 0;
    }

    @Override
    public void draw(T data) {
        if (((ParticleControllerRenderData)data).controller.particles.size > 0) {
            this.renderData.add(data);
            this.bufferedParticlesCount += ((ParticleControllerRenderData)data).controller.particles.size;
        }
    }

    @Override
    public void end() {
        if (this.bufferedParticlesCount > 0) {
            this.ensureCapacity(this.bufferedParticlesCount);
            this.flush(this.sorter.sort(this.renderData));
        }
    }

    public void ensureCapacity(int capacity) {
        if (this.currentCapacity >= capacity) {
            return;
        }
        this.sorter.ensureCapacity(capacity);
        this.allocParticlesData(capacity);
        this.currentCapacity = capacity;
    }

    public void resetCapacity() {
        this.bufferedParticlesCount = 0;
        this.currentCapacity = 0;
    }

    protected abstract void allocParticlesData(int var1);

    public void setCamera(Camera camera) {
        this.camera = camera;
        this.sorter.setCamera(camera);
    }

    public ParticleSorter getSorter() {
        return this.sorter;
    }

    public void setSorter(ParticleSorter sorter) {
        this.sorter = sorter;
        sorter.setCamera(this.camera);
        sorter.ensureCapacity(this.currentCapacity);
    }

    protected abstract void flush(int[] var1);

    public int getBufferedCount() {
        return this.bufferedParticlesCount;
    }
}

