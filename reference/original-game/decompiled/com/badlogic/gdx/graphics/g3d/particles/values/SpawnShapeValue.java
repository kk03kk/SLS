/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.values;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.graphics.g3d.particles.values.RangedNumericValue;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class SpawnShapeValue
extends ParticleValue
implements ResourceData.Configurable,
Json.Serializable {
    public RangedNumericValue xOffsetValue = new RangedNumericValue();
    public RangedNumericValue yOffsetValue = new RangedNumericValue();
    public RangedNumericValue zOffsetValue = new RangedNumericValue();

    public SpawnShapeValue() {
    }

    public SpawnShapeValue(SpawnShapeValue spawnShapeValue) {
        this();
    }

    public abstract void spawnAux(Vector3 var1, float var2);

    public final Vector3 spawn(Vector3 vector, float percent) {
        this.spawnAux(vector, percent);
        if (this.xOffsetValue.active) {
            vector.x += this.xOffsetValue.newLowValue();
        }
        if (this.yOffsetValue.active) {
            vector.y += this.yOffsetValue.newLowValue();
        }
        if (this.zOffsetValue.active) {
            vector.z += this.zOffsetValue.newLowValue();
        }
        return vector;
    }

    public void init() {
    }

    public void start() {
    }

    @Override
    public void load(ParticleValue value) {
        super.load(value);
        SpawnShapeValue shape = (SpawnShapeValue)value;
        this.xOffsetValue.load(shape.xOffsetValue);
        this.yOffsetValue.load(shape.yOffsetValue);
        this.zOffsetValue.load(shape.zOffsetValue);
    }

    public abstract SpawnShapeValue copy();

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("xOffsetValue", this.xOffsetValue);
        json.writeValue("yOffsetValue", this.yOffsetValue);
        json.writeValue("zOffsetValue", this.zOffsetValue);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.xOffsetValue = json.readValue("xOffsetValue", RangedNumericValue.class, jsonData);
        this.yOffsetValue = json.readValue("yOffsetValue", RangedNumericValue.class, jsonData);
        this.zOffsetValue = json.readValue("zOffsetValue", RangedNumericValue.class, jsonData);
    }

    public void save(AssetManager manager, ResourceData data) {
    }

    public void load(AssetManager manager, ResourceData data) {
    }
}

