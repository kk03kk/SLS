/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import java.util.Iterator;

public abstract class ParticleControllerInfluencer
extends Influencer {
    public Array<ParticleController> templates;
    ParallelArray.ObjectChannel<ParticleController> particleControllerChannel;

    public ParticleControllerInfluencer() {
        this.templates = new Array(true, 1, ParticleController.class);
    }

    public ParticleControllerInfluencer(ParticleController ... templates) {
        this.templates = new Array<ParticleController>(templates);
    }

    public ParticleControllerInfluencer(ParticleControllerInfluencer influencer) {
        this((ParticleController[])influencer.templates.items);
    }

    @Override
    public void allocateChannels() {
        this.particleControllerChannel = (ParallelArray.ObjectChannel)this.controller.particles.addChannel(ParticleChannels.ParticleController);
    }

    @Override
    public void end() {
        for (int i = 0; i < this.controller.particles.size; ++i) {
            ((ParticleController[])this.particleControllerChannel.data)[i].end();
        }
    }

    @Override
    public void dispose() {
        if (this.controller != null) {
            for (int i = 0; i < this.controller.particles.size; ++i) {
                ParticleController controller = ((ParticleController[])this.particleControllerChannel.data)[i];
                if (controller == null) continue;
                controller.dispose();
                ((ParticleController[])this.particleControllerChannel.data)[i] = null;
            }
        }
    }

    @Override
    public void save(AssetManager manager, ResourceData resources) {
        ResourceData.SaveData data = resources.createSaveData();
        Array<ParticleEffect> effects = manager.getAll(ParticleEffect.class, new Array());
        Array<ParticleController> controllers = new Array<ParticleController>(this.templates);
        Array<IntArray> effectsIndices = new Array<IntArray>();
        for (int i = 0; i < effects.size && controllers.size > 0; ++i) {
            ParticleEffect effect = effects.get(i);
            Array<ParticleController> effectControllers = effect.getControllers();
            Iterator<ParticleController> iterator = controllers.iterator();
            IntArray indices = null;
            while (iterator.hasNext()) {
                ParticleController controller = iterator.next();
                int index = -1;
                index = effectControllers.indexOf(controller, true);
                if (index <= -1) continue;
                if (indices == null) {
                    indices = new IntArray();
                }
                iterator.remove();
                indices.add(index);
            }
            if (indices == null) continue;
            data.saveAsset(manager.getAssetFileName(effect), ParticleEffect.class);
            effectsIndices.add(indices);
        }
        data.save("indices", effectsIndices);
    }

    @Override
    public void load(AssetManager manager, ResourceData resources) {
        AssetDescriptor descriptor;
        ResourceData.SaveData data = resources.getSaveData();
        Array effectsIndices = (Array)data.load("indices");
        Iterator iterator = effectsIndices.iterator();
        while ((descriptor = data.loadAsset()) != null) {
            ParticleEffect effect = (ParticleEffect)manager.get(descriptor);
            if (effect == null) {
                throw new RuntimeException("Template is null");
            }
            Array<ParticleController> effectControllers = effect.getControllers();
            IntArray effectIndices = (IntArray)iterator.next();
            int n = effectIndices.size;
            for (int i = 0; i < n; ++i) {
                this.templates.add(effectControllers.get(effectIndices.get(i)));
            }
        }
    }

    public static class Random
    extends ParticleControllerInfluencer {
        ParticleControllerPool pool = new ParticleControllerPool();

        public Random() {
        }

        public Random(ParticleController ... templates) {
            super(templates);
        }

        public Random(Random particleControllerRandom) {
            super(particleControllerRandom);
        }

        @Override
        public void init() {
            this.pool.clear();
            for (int i = 0; i < this.controller.emitter.maxParticleCount; ++i) {
                this.pool.free(this.pool.newObject());
            }
        }

        @Override
        public void dispose() {
            this.pool.clear();
            super.dispose();
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int c = startIndex + count;
            for (int i = startIndex; i < c; ++i) {
                ParticleController controller = (ParticleController)this.pool.obtain();
                controller.start();
                ((ParticleController[])this.particleControllerChannel.data)[i] = controller;
            }
        }

        @Override
        public void killParticles(int startIndex, int count) {
            int c = startIndex + count;
            for (int i = startIndex; i < c; ++i) {
                ParticleController controller = ((ParticleController[])this.particleControllerChannel.data)[i];
                controller.end();
                this.pool.free(controller);
                ((ParticleController[])this.particleControllerChannel.data)[i] = null;
            }
        }

        @Override
        public Random copy() {
            return new Random(this);
        }

        private class ParticleControllerPool
        extends Pool<ParticleController> {
            @Override
            public ParticleController newObject() {
                ParticleController controller = ((ParticleController)Random.this.templates.random()).copy();
                controller.init();
                return controller;
            }

            @Override
            public void clear() {
                int free = Random.this.pool.getFree();
                for (int i = 0; i < free; ++i) {
                    ((ParticleController)Random.this.pool.obtain()).dispose();
                }
                super.clear();
            }
        }
    }

    public static class Single
    extends ParticleControllerInfluencer {
        public Single(ParticleController ... templates) {
            super(templates);
        }

        public Single() {
        }

        public Single(Single particleControllerSingle) {
            super(particleControllerSingle);
        }

        @Override
        public void init() {
            ParticleController first = (ParticleController)this.templates.first();
            int c = this.controller.particles.capacity;
            for (int i = 0; i < c; ++i) {
                ParticleController copy = first.copy();
                copy.init();
                ((ParticleController[])this.particleControllerChannel.data)[i] = copy;
            }
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int c = startIndex + count;
            for (int i = startIndex; i < c; ++i) {
                ((ParticleController[])this.particleControllerChannel.data)[i].start();
            }
        }

        @Override
        public void killParticles(int startIndex, int count) {
            int c = startIndex + count;
            for (int i = startIndex; i < c; ++i) {
                ((ParticleController[])this.particleControllerChannel.data)[i].end();
            }
        }

        @Override
        public Single copy() {
            return new Single(this);
        }
    }
}

