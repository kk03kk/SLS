/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.emitters.Emitter;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class ParticleController
implements Json.Serializable,
ResourceData.Configurable {
    protected static final float DEFAULT_TIME_STEP = 0.016666668f;
    public String name;
    public Emitter emitter;
    public Array<Influencer> influencers;
    public ParticleControllerRenderer<?, ?> renderer;
    public ParallelArray particles;
    public ParticleChannels particleChannels;
    public Matrix4 transform = new Matrix4();
    public Vector3 scale = new Vector3(1.0f, 1.0f, 1.0f);
    protected BoundingBox boundingBox;
    public float deltaTime;
    public float deltaTimeSqr;

    public ParticleController() {
        this.influencers = new Array(true, 3, Influencer.class);
        this.setTimeStep(0.016666668f);
    }

    public ParticleController(String name, Emitter emitter, ParticleControllerRenderer<?, ?> renderer, Influencer ... influencers) {
        this();
        this.name = name;
        this.emitter = emitter;
        this.renderer = renderer;
        this.particleChannels = new ParticleChannels();
        this.influencers = new Array<Influencer>(influencers);
    }

    private void setTimeStep(float timeStep) {
        this.deltaTime = timeStep;
        this.deltaTimeSqr = this.deltaTime * this.deltaTime;
    }

    public void setTransform(Matrix4 transform) {
        this.transform.set(transform);
        transform.getScale(this.scale);
    }

    public void setTransform(float x, float y, float z, float qx, float qy, float qz, float qw, float scale) {
        this.transform.set(x, y, z, qx, qy, qz, qw, scale, scale, scale);
        this.scale.set(scale, scale, scale);
    }

    public void rotate(Quaternion rotation) {
        this.transform.rotate(rotation);
    }

    public void rotate(Vector3 axis, float angle) {
        this.transform.rotate(axis, angle);
    }

    public void translate(Vector3 translation) {
        this.transform.translate(translation);
    }

    public void setTranslation(Vector3 translation) {
        this.transform.setTranslation(translation);
    }

    public void scale(float scaleX, float scaleY, float scaleZ) {
        this.transform.scale(scaleX, scaleY, scaleZ);
        this.transform.getScale(this.scale);
    }

    public void scale(Vector3 scale) {
        this.scale(scale.x, scale.y, scale.z);
    }

    public void mul(Matrix4 transform) {
        this.transform.mul(transform);
        this.transform.getScale(this.scale);
    }

    public void getTransform(Matrix4 transform) {
        transform.set(this.transform);
    }

    public boolean isComplete() {
        return this.emitter.isComplete();
    }

    public void init() {
        this.bind();
        if (this.particles != null) {
            this.end();
            this.particleChannels.resetIds();
        }
        this.allocateChannels(this.emitter.maxParticleCount);
        this.emitter.init();
        for (Influencer influencer : this.influencers) {
            influencer.init();
        }
        this.renderer.init();
    }

    protected void allocateChannels(int maxParticleCount) {
        this.particles = new ParallelArray(maxParticleCount);
        this.emitter.allocateChannels();
        for (Influencer influencer : this.influencers) {
            influencer.allocateChannels();
        }
        this.renderer.allocateChannels();
    }

    protected void bind() {
        this.emitter.set(this);
        for (Influencer influencer : this.influencers) {
            influencer.set(this);
        }
        this.renderer.set(this);
    }

    public void start() {
        this.emitter.start();
        for (Influencer influencer : this.influencers) {
            influencer.start();
        }
    }

    public void reset() {
        this.end();
        this.start();
    }

    public void end() {
        for (Influencer influencer : this.influencers) {
            influencer.end();
        }
        this.emitter.end();
    }

    public void activateParticles(int startIndex, int count) {
        this.emitter.activateParticles(startIndex, count);
        for (Influencer influencer : this.influencers) {
            influencer.activateParticles(startIndex, count);
        }
    }

    public void killParticles(int startIndex, int count) {
        this.emitter.killParticles(startIndex, count);
        for (Influencer influencer : this.influencers) {
            influencer.killParticles(startIndex, count);
        }
    }

    public void update() {
        this.emitter.update();
        for (Influencer influencer : this.influencers) {
            influencer.update();
        }
    }

    public void draw() {
        if (this.particles.size > 0) {
            this.renderer.update();
        }
    }

    public ParticleController copy() {
        Emitter emitter = (Emitter)this.emitter.copy();
        Influencer[] influencers = new Influencer[this.influencers.size];
        int i = 0;
        for (Influencer influencer : this.influencers) {
            influencers[i++] = (Influencer)influencer.copy();
        }
        return new ParticleController(new String(this.name), emitter, (ParticleControllerRenderer)this.renderer.copy(), influencers);
    }

    public void dispose() {
        this.emitter.dispose();
        for (Influencer influencer : this.influencers) {
            influencer.dispose();
        }
    }

    public BoundingBox getBoundingBox() {
        if (this.boundingBox == null) {
            this.boundingBox = new BoundingBox();
        }
        this.calculateBoundingBox();
        return this.boundingBox;
    }

    protected void calculateBoundingBox() {
        this.boundingBox.clr();
        ParallelArray.FloatChannel positionChannel = (ParallelArray.FloatChannel)this.particles.getChannel(ParticleChannels.Position);
        int c = positionChannel.strideSize * this.particles.size;
        for (int pos = 0; pos < c; pos += positionChannel.strideSize) {
            this.boundingBox.ext(positionChannel.data[pos + 0], positionChannel.data[pos + 1], positionChannel.data[pos + 2]);
        }
    }

    private <K extends Influencer> int findIndex(Class<K> type) {
        for (int i = 0; i < this.influencers.size; ++i) {
            Influencer influencer = this.influencers.get(i);
            if (!ClassReflection.isAssignableFrom(type, influencer.getClass())) continue;
            return i;
        }
        return -1;
    }

    public <K extends Influencer> K findInfluencer(Class<K> influencerClass) {
        int index = this.findIndex(influencerClass);
        return (K)(index > -1 ? this.influencers.get(index) : null);
    }

    public <K extends Influencer> void removeInfluencer(Class<K> type) {
        int index = this.findIndex(type);
        if (index > -1) {
            this.influencers.removeIndex(index);
        }
    }

    public <K extends Influencer> boolean replaceInfluencer(Class<K> type, K newInfluencer) {
        int index = this.findIndex(type);
        if (index > -1) {
            this.influencers.insert(index, newInfluencer);
            this.influencers.removeIndex(index + 1);
            return true;
        }
        return false;
    }

    @Override
    public void write(Json json) {
        json.writeValue("name", this.name);
        json.writeValue("emitter", this.emitter, Emitter.class);
        json.writeValue("influencers", this.influencers, Array.class, Influencer.class);
        json.writeValue("renderer", this.renderer, ParticleControllerRenderer.class);
    }

    @Override
    public void read(Json json, JsonValue jsonMap) {
        this.name = json.readValue("name", String.class, jsonMap);
        this.emitter = json.readValue("emitter", Emitter.class, jsonMap);
        this.influencers.addAll(json.readValue("influencers", Array.class, Influencer.class, jsonMap));
        this.renderer = json.readValue("renderer", ParticleControllerRenderer.class, jsonMap);
    }

    public void save(AssetManager manager, ResourceData data) {
        this.emitter.save(manager, data);
        for (Influencer influencer : this.influencers) {
            influencer.save(manager, data);
        }
        this.renderer.save(manager, data);
    }

    public void load(AssetManager manager, ResourceData data) {
        this.emitter.load(manager, data);
        for (Influencer influencer : this.influencers) {
            influencer.load(manager, data);
        }
        this.renderer.load(manager, data);
    }
}

