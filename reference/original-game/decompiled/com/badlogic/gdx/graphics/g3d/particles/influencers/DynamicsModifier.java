/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class DynamicsModifier
extends Influencer {
    protected static final Vector3 TMP_V1 = new Vector3();
    protected static final Vector3 TMP_V2 = new Vector3();
    protected static final Vector3 TMP_V3 = new Vector3();
    protected static final Quaternion TMP_Q = new Quaternion();
    public boolean isGlobal = false;
    protected ParallelArray.FloatChannel lifeChannel;

    public DynamicsModifier() {
    }

    public DynamicsModifier(DynamicsModifier modifier) {
        this.isGlobal = modifier.isGlobal;
    }

    @Override
    public void allocateChannels() {
        this.lifeChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Life);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("isGlobal", this.isGlobal);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.isGlobal = json.readValue("isGlobal", Boolean.TYPE, jsonData);
    }

    public static class BrownianAcceleration
    extends Strength {
        ParallelArray.FloatChannel accelerationChannel;

        public BrownianAcceleration() {
        }

        public BrownianAcceleration(BrownianAcceleration rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.accelerationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Acceleration);
        }

        @Override
        public void update() {
            int lifeOffset = 2;
            int strengthOffset = 0;
            int forceOffset = 0;
            int i = 0;
            int c = this.controller.particles.size;
            while (i < c) {
                float strength = this.strengthChannel.data[strengthOffset + 0] + this.strengthChannel.data[strengthOffset + 1] * this.strengthValue.getScale(this.lifeChannel.data[lifeOffset]);
                TMP_V3.set(MathUtils.random(-1.0f, 1.0f), MathUtils.random(-1.0f, 1.0f), MathUtils.random(-1.0f, 1.0f)).nor().scl(strength);
                int n = forceOffset + 0;
                this.accelerationChannel.data[n] = this.accelerationChannel.data[n] + BrownianAcceleration.TMP_V3.x;
                int n2 = forceOffset + 1;
                this.accelerationChannel.data[n2] = this.accelerationChannel.data[n2] + BrownianAcceleration.TMP_V3.y;
                int n3 = forceOffset + 2;
                this.accelerationChannel.data[n3] = this.accelerationChannel.data[n3] + BrownianAcceleration.TMP_V3.z;
                ++i;
                strengthOffset += this.strengthChannel.strideSize;
                forceOffset += this.accelerationChannel.strideSize;
                lifeOffset += this.lifeChannel.strideSize;
            }
        }

        @Override
        public BrownianAcceleration copy() {
            return new BrownianAcceleration(this);
        }
    }

    public static class TangentialAcceleration
    extends Angular {
        ParallelArray.FloatChannel directionalVelocityChannel;
        ParallelArray.FloatChannel positionChannel;

        public TangentialAcceleration() {
        }

        public TangentialAcceleration(TangentialAcceleration rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.directionalVelocityChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Acceleration);
            this.positionChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Position);
        }

        @Override
        public void update() {
            int i = 0;
            int l = 2;
            int s = 0;
            int a = 0;
            int positionOffset = 0;
            int c = i + this.controller.particles.size * this.directionalVelocityChannel.strideSize;
            while (i < c) {
                float lifePercent = this.lifeChannel.data[l];
                float strength = this.strengthChannel.data[s + 0] + this.strengthChannel.data[s + 1] * this.strengthValue.getScale(lifePercent);
                float phi = this.angularChannel.data[a + 2] + this.angularChannel.data[a + 3] * this.phiValue.getScale(lifePercent);
                float theta = this.angularChannel.data[a + 0] + this.angularChannel.data[a + 1] * this.thetaValue.getScale(lifePercent);
                float cosTheta = MathUtils.cosDeg(theta);
                float sinTheta = MathUtils.sinDeg(theta);
                float cosPhi = MathUtils.cosDeg(phi);
                float sinPhi = MathUtils.sinDeg(phi);
                TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi).crs(this.positionChannel.data[positionOffset + 0], this.positionChannel.data[positionOffset + 1], this.positionChannel.data[positionOffset + 2]).nor().scl(strength);
                int n = i + 0;
                this.directionalVelocityChannel.data[n] = this.directionalVelocityChannel.data[n] + TangentialAcceleration.TMP_V3.x;
                int n2 = i + 1;
                this.directionalVelocityChannel.data[n2] = this.directionalVelocityChannel.data[n2] + TangentialAcceleration.TMP_V3.y;
                int n3 = i + 2;
                this.directionalVelocityChannel.data[n3] = this.directionalVelocityChannel.data[n3] + TangentialAcceleration.TMP_V3.z;
                s += this.strengthChannel.strideSize;
                i += this.directionalVelocityChannel.strideSize;
                a += this.angularChannel.strideSize;
                l += this.lifeChannel.strideSize;
                positionOffset += this.positionChannel.strideSize;
            }
        }

        @Override
        public TangentialAcceleration copy() {
            return new TangentialAcceleration(this);
        }
    }

    public static class PolarAcceleration
    extends Angular {
        ParallelArray.FloatChannel directionalVelocityChannel;

        public PolarAcceleration() {
        }

        public PolarAcceleration(PolarAcceleration rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.directionalVelocityChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Acceleration);
        }

        @Override
        public void update() {
            int i = 0;
            int l = 2;
            int s = 0;
            int a = 0;
            int c = i + this.controller.particles.size * this.directionalVelocityChannel.strideSize;
            while (i < c) {
                float lifePercent = this.lifeChannel.data[l];
                float strength = this.strengthChannel.data[s + 0] + this.strengthChannel.data[s + 1] * this.strengthValue.getScale(lifePercent);
                float phi = this.angularChannel.data[a + 2] + this.angularChannel.data[a + 3] * this.phiValue.getScale(lifePercent);
                float theta = this.angularChannel.data[a + 0] + this.angularChannel.data[a + 1] * this.thetaValue.getScale(lifePercent);
                float cosTheta = MathUtils.cosDeg(theta);
                float sinTheta = MathUtils.sinDeg(theta);
                float cosPhi = MathUtils.cosDeg(phi);
                float sinPhi = MathUtils.sinDeg(phi);
                TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi).nor().scl(strength);
                int n = i + 0;
                this.directionalVelocityChannel.data[n] = this.directionalVelocityChannel.data[n] + PolarAcceleration.TMP_V3.x;
                int n2 = i + 1;
                this.directionalVelocityChannel.data[n2] = this.directionalVelocityChannel.data[n2] + PolarAcceleration.TMP_V3.y;
                int n3 = i + 2;
                this.directionalVelocityChannel.data[n3] = this.directionalVelocityChannel.data[n3] + PolarAcceleration.TMP_V3.z;
                s += this.strengthChannel.strideSize;
                i += this.directionalVelocityChannel.strideSize;
                a += this.angularChannel.strideSize;
                l += this.lifeChannel.strideSize;
            }
        }

        @Override
        public PolarAcceleration copy() {
            return new PolarAcceleration(this);
        }
    }

    public static class CentripetalAcceleration
    extends Strength {
        ParallelArray.FloatChannel accelerationChannel;
        ParallelArray.FloatChannel positionChannel;

        public CentripetalAcceleration() {
        }

        public CentripetalAcceleration(CentripetalAcceleration rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.accelerationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Acceleration);
            this.positionChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Position);
        }

        @Override
        public void update() {
            float cx = 0.0f;
            float cy = 0.0f;
            float cz = 0.0f;
            if (!this.isGlobal) {
                float[] val = this.controller.transform.val;
                cx = val[12];
                cy = val[13];
                cz = val[14];
            }
            int lifeOffset = 2;
            int strengthOffset = 0;
            int positionOffset = 0;
            int forceOffset = 0;
            int i = 0;
            int c = this.controller.particles.size;
            while (i < c) {
                float strength = this.strengthChannel.data[strengthOffset + 0] + this.strengthChannel.data[strengthOffset + 1] * this.strengthValue.getScale(this.lifeChannel.data[lifeOffset]);
                TMP_V3.set(this.positionChannel.data[positionOffset + 0] - cx, this.positionChannel.data[positionOffset + 1] - cy, this.positionChannel.data[positionOffset + 2] - cz).nor().scl(strength);
                int n = forceOffset + 0;
                this.accelerationChannel.data[n] = this.accelerationChannel.data[n] + CentripetalAcceleration.TMP_V3.x;
                int n2 = forceOffset + 1;
                this.accelerationChannel.data[n2] = this.accelerationChannel.data[n2] + CentripetalAcceleration.TMP_V3.y;
                int n3 = forceOffset + 2;
                this.accelerationChannel.data[n3] = this.accelerationChannel.data[n3] + CentripetalAcceleration.TMP_V3.z;
                ++i;
                positionOffset += this.positionChannel.strideSize;
                strengthOffset += this.strengthChannel.strideSize;
                forceOffset += this.accelerationChannel.strideSize;
                lifeOffset += this.lifeChannel.strideSize;
            }
        }

        @Override
        public CentripetalAcceleration copy() {
            return new CentripetalAcceleration(this);
        }
    }

    public static class Rotational3D
    extends Angular {
        ParallelArray.FloatChannel rotationChannel;
        ParallelArray.FloatChannel rotationalForceChannel;

        public Rotational3D() {
        }

        public Rotational3D(Rotational3D rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.rotationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Rotation3D);
            this.rotationalForceChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.AngularVelocity3D);
        }

        @Override
        public void update() {
            int i = 0;
            int l = 2;
            int s = 0;
            int a = 0;
            int c = this.controller.particles.size * this.rotationalForceChannel.strideSize;
            while (i < c) {
                float lifePercent = this.lifeChannel.data[l];
                float strength = this.strengthChannel.data[s + 0] + this.strengthChannel.data[s + 1] * this.strengthValue.getScale(lifePercent);
                float phi = this.angularChannel.data[a + 2] + this.angularChannel.data[a + 3] * this.phiValue.getScale(lifePercent);
                float theta = this.angularChannel.data[a + 0] + this.angularChannel.data[a + 1] * this.thetaValue.getScale(lifePercent);
                float cosTheta = MathUtils.cosDeg(theta);
                float sinTheta = MathUtils.sinDeg(theta);
                float cosPhi = MathUtils.cosDeg(phi);
                float sinPhi = MathUtils.sinDeg(phi);
                TMP_V3.set(cosTheta * sinPhi, cosPhi, sinTheta * sinPhi);
                TMP_V3.scl(strength * ((float)Math.PI / 180));
                int n = i + 0;
                this.rotationalForceChannel.data[n] = this.rotationalForceChannel.data[n] + Rotational3D.TMP_V3.x;
                int n2 = i + 1;
                this.rotationalForceChannel.data[n2] = this.rotationalForceChannel.data[n2] + Rotational3D.TMP_V3.y;
                int n3 = i + 2;
                this.rotationalForceChannel.data[n3] = this.rotationalForceChannel.data[n3] + Rotational3D.TMP_V3.z;
                s += this.strengthChannel.strideSize;
                i += this.rotationalForceChannel.strideSize;
                a += this.angularChannel.strideSize;
                l += this.lifeChannel.strideSize;
            }
        }

        @Override
        public Rotational3D copy() {
            return new Rotational3D(this);
        }
    }

    public static class Rotational2D
    extends Strength {
        ParallelArray.FloatChannel rotationalVelocity2dChannel;

        public Rotational2D() {
        }

        public Rotational2D(Rotational2D rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            this.rotationalVelocity2dChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.AngularVelocity2D);
        }

        @Override
        public void update() {
            int i = 0;
            int l = 2;
            int s = 0;
            int c = i + this.controller.particles.size * this.rotationalVelocity2dChannel.strideSize;
            while (i < c) {
                int n = i;
                this.rotationalVelocity2dChannel.data[n] = this.rotationalVelocity2dChannel.data[n] + (this.strengthChannel.data[s + 0] + this.strengthChannel.data[s + 1] * this.strengthValue.getScale(this.lifeChannel.data[l]));
                s += this.strengthChannel.strideSize;
                i += this.rotationalVelocity2dChannel.strideSize;
                l += this.lifeChannel.strideSize;
            }
        }

        @Override
        public Rotational2D copy() {
            return new Rotational2D(this);
        }
    }

    public static abstract class Angular
    extends Strength {
        protected ParallelArray.FloatChannel angularChannel;
        public ScaledNumericValue thetaValue = new ScaledNumericValue();
        public ScaledNumericValue phiValue = new ScaledNumericValue();

        public Angular() {
        }

        public Angular(Angular value) {
            super(value);
            this.thetaValue.load(value.thetaValue);
            this.phiValue.load(value.phiValue);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            ParticleChannels.Interpolation4.id = this.controller.particleChannels.newId();
            this.angularChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Interpolation4);
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int i;
            super.activateParticles(startIndex, count);
            int c = i + count * this.angularChannel.strideSize;
            for (i = startIndex * this.angularChannel.strideSize; i < c; i += this.angularChannel.strideSize) {
                float start = this.thetaValue.newLowValue();
                float diff = this.thetaValue.newHighValue();
                if (!this.thetaValue.isRelative()) {
                    diff -= start;
                }
                this.angularChannel.data[i + 0] = start;
                this.angularChannel.data[i + 1] = diff;
                start = this.phiValue.newLowValue();
                diff = this.phiValue.newHighValue();
                if (!this.phiValue.isRelative()) {
                    diff -= start;
                }
                this.angularChannel.data[i + 2] = start;
                this.angularChannel.data[i + 3] = diff;
            }
        }

        @Override
        public void write(Json json) {
            super.write(json);
            json.writeValue("thetaValue", this.thetaValue);
            json.writeValue("phiValue", this.phiValue);
        }

        @Override
        public void read(Json json, JsonValue jsonData) {
            super.read(json, jsonData);
            this.thetaValue = json.readValue("thetaValue", ScaledNumericValue.class, jsonData);
            this.phiValue = json.readValue("phiValue", ScaledNumericValue.class, jsonData);
        }
    }

    public static abstract class Strength
    extends DynamicsModifier {
        protected ParallelArray.FloatChannel strengthChannel;
        public ScaledNumericValue strengthValue = new ScaledNumericValue();

        public Strength() {
        }

        public Strength(Strength rotation) {
            super(rotation);
            this.strengthValue.load(rotation.strengthValue);
        }

        @Override
        public void allocateChannels() {
            super.allocateChannels();
            ParticleChannels.Interpolation.id = this.controller.particleChannels.newId();
            this.strengthChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Interpolation);
        }

        @Override
        public void activateParticles(int startIndex, int count) {
            int i;
            int c = i + count * this.strengthChannel.strideSize;
            for (i = startIndex * this.strengthChannel.strideSize; i < c; i += this.strengthChannel.strideSize) {
                float start = this.strengthValue.newLowValue();
                float diff = this.strengthValue.newHighValue();
                if (!this.strengthValue.isRelative()) {
                    diff -= start;
                }
                this.strengthChannel.data[i + 0] = start;
                this.strengthChannel.data[i + 1] = diff;
            }
        }

        @Override
        public void write(Json json) {
            super.write(json);
            json.writeValue("strengthValue", this.strengthValue);
        }

        @Override
        public void read(Json json, JsonValue jsonData) {
            super.read(json, jsonData);
            this.strengthValue = json.readValue("strengthValue", ScaledNumericValue.class, jsonData);
        }
    }

    public static class FaceDirection
    extends DynamicsModifier {
        ParallelArray.FloatChannel rotationChannel;
        ParallelArray.FloatChannel accellerationChannel;

        public FaceDirection() {
        }

        public FaceDirection(FaceDirection rotation) {
            super(rotation);
        }

        @Override
        public void allocateChannels() {
            this.rotationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Rotation3D);
            this.accellerationChannel = (ParallelArray.FloatChannel)this.controller.particles.addChannel(ParticleChannels.Acceleration);
        }

        @Override
        public void update() {
            int i = 0;
            int accelOffset = 0;
            int c = i + this.controller.particles.size * this.rotationChannel.strideSize;
            while (i < c) {
                Vector3 axisZ = TMP_V1.set(this.accellerationChannel.data[accelOffset + 0], this.accellerationChannel.data[accelOffset + 1], this.accellerationChannel.data[accelOffset + 2]).nor();
                Vector3 axisY = TMP_V2.set(TMP_V1).crs(Vector3.Y).nor().crs(TMP_V1).nor();
                Vector3 axisX = TMP_V3.set(axisY).crs(axisZ).nor();
                TMP_Q.setFromAxes(false, axisX.x, axisY.x, axisZ.x, axisX.y, axisY.y, axisZ.y, axisX.z, axisY.z, axisZ.z);
                this.rotationChannel.data[i + 0] = FaceDirection.TMP_Q.x;
                this.rotationChannel.data[i + 1] = FaceDirection.TMP_Q.y;
                this.rotationChannel.data[i + 2] = FaceDirection.TMP_Q.z;
                this.rotationChannel.data[i + 3] = FaceDirection.TMP_Q.w;
                i += this.rotationChannel.strideSize;
                accelOffset += this.accellerationChannel.strideSize;
            }
        }

        @Override
        public ParticleControllerComponent copy() {
            return new FaceDirection(this);
        }
    }
}

