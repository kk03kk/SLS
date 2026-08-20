/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.values.PointSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.SpawnShapeValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SpawnInfluencer
extends Influencer {
    public SpawnShapeValue spawnShapeValue;
    ParallelArray.FloatChannel positionChannel;

    public SpawnInfluencer() {
        this.spawnShapeValue = new PointSpawnShapeValue();
    }

    public SpawnInfluencer(SpawnShapeValue spawnShapeValue) {
        this.spawnShapeValue = spawnShapeValue;
    }

    public SpawnInfluencer(SpawnInfluencer source) {
        this.spawnShapeValue = source.spawnShapeValue.copy();
    }

    @Override
    public void init() {
        this.spawnShapeValue.init();
    }

    @Override
    public void allocateChannels() {
        this.positionChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Position);
    }

    @Override
    public void start() {
        this.spawnShapeValue.start();
    }

    @Override
    public void activateParticles(int startIndex, int count) {
        int i;
        int c = i + count * this.positionChannel.strideSize;
        for (i = startIndex * this.positionChannel.strideSize; i < c; i += this.positionChannel.strideSize) {
            this.spawnShapeValue.spawn(TMP_V1, this.controller.emitter.percent);
            TMP_V1.mul(this.controller.transform);
            this.positionChannel.data[i + 0] = SpawnInfluencer.TMP_V1.x;
            this.positionChannel.data[i + 1] = SpawnInfluencer.TMP_V1.y;
            this.positionChannel.data[i + 2] = SpawnInfluencer.TMP_V1.z;
        }
    }

    @Override
    public SpawnInfluencer copy() {
        return new SpawnInfluencer(this);
    }

    @Override
    public void write(Json json) {
        json.writeValue("spawnShape", this.spawnShapeValue, SpawnShapeValue.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.spawnShapeValue = json.readValue("spawnShape", SpawnShapeValue.class, jsonData);
    }

    @Override
    public void save(AssetManager manager, ResourceData data) {
        this.spawnShapeValue.save(manager, data);
    }

    @Override
    public void load(AssetManager manager, ResourceData data) {
        this.spawnShapeValue.load(manager, data);
    }
}

