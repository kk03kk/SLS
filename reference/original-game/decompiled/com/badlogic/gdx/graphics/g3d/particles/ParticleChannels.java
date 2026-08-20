/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import java.util.Arrays;

public class ParticleChannels {
    private static int currentGlobalId;
    public static final ParallelArray.ChannelDescriptor Life;
    public static final ParallelArray.ChannelDescriptor Position;
    public static final ParallelArray.ChannelDescriptor PreviousPosition;
    public static final ParallelArray.ChannelDescriptor Color;
    public static final ParallelArray.ChannelDescriptor TextureRegion;
    public static final ParallelArray.ChannelDescriptor Rotation2D;
    public static final ParallelArray.ChannelDescriptor Rotation3D;
    public static final ParallelArray.ChannelDescriptor Scale;
    public static final ParallelArray.ChannelDescriptor ModelInstance;
    public static final ParallelArray.ChannelDescriptor ParticleController;
    public static final ParallelArray.ChannelDescriptor Acceleration;
    public static final ParallelArray.ChannelDescriptor AngularVelocity2D;
    public static final ParallelArray.ChannelDescriptor AngularVelocity3D;
    public static final ParallelArray.ChannelDescriptor Interpolation;
    public static final ParallelArray.ChannelDescriptor Interpolation4;
    public static final ParallelArray.ChannelDescriptor Interpolation6;
    public static final int CurrentLifeOffset = 0;
    public static final int TotalLifeOffset = 1;
    public static final int LifePercentOffset = 2;
    public static final int RedOffset = 0;
    public static final int GreenOffset = 1;
    public static final int BlueOffset = 2;
    public static final int AlphaOffset = 3;
    public static final int InterpolationStartOffset = 0;
    public static final int InterpolationDiffOffset = 1;
    public static final int VelocityStrengthStartOffset = 0;
    public static final int VelocityStrengthDiffOffset = 1;
    public static final int VelocityThetaStartOffset = 0;
    public static final int VelocityThetaDiffOffset = 1;
    public static final int VelocityPhiStartOffset = 2;
    public static final int VelocityPhiDiffOffset = 3;
    public static final int XOffset = 0;
    public static final int YOffset = 1;
    public static final int ZOffset = 2;
    public static final int WOffset = 3;
    public static final int UOffset = 0;
    public static final int VOffset = 1;
    public static final int U2Offset = 2;
    public static final int V2Offset = 3;
    public static final int HalfWidthOffset = 4;
    public static final int HalfHeightOffset = 5;
    public static final int CosineOffset = 0;
    public static final int SineOffset = 1;
    private int currentId;

    public static int newGlobalId() {
        return currentGlobalId++;
    }

    public ParticleChannels() {
        this.resetIds();
    }

    public int newId() {
        return this.currentId++;
    }

    protected void resetIds() {
        this.currentId = currentGlobalId;
    }

    static {
        Life = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 3);
        Position = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 3);
        PreviousPosition = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 3);
        Color = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 4);
        TextureRegion = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 6);
        Rotation2D = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 2);
        Rotation3D = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 4);
        Scale = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 1);
        ModelInstance = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), ModelInstance.class, 1);
        ParticleController = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), ParticleController.class, 1);
        Acceleration = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 3);
        AngularVelocity2D = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 1);
        AngularVelocity3D = new ParallelArray.ChannelDescriptor(ParticleChannels.newGlobalId(), Float.TYPE, 3);
        Interpolation = new ParallelArray.ChannelDescriptor(-1, Float.TYPE, 2);
        Interpolation4 = new ParallelArray.ChannelDescriptor(-1, Float.TYPE, 4);
        Interpolation6 = new ParallelArray.ChannelDescriptor(-1, Float.TYPE, 6);
    }

    public static class Rotation3dInitializer
    implements ParallelArray.ChannelInitializer<ParallelArray.FloatChannel> {
        private static Rotation3dInitializer instance;

        public static Rotation3dInitializer get() {
            if (instance == null) {
                instance = new Rotation3dInitializer();
            }
            return instance;
        }

        @Override
        public void init(ParallelArray.FloatChannel channel) {
            int c = channel.data.length;
            for (int i = 0; i < c; i += channel.strideSize) {
                channel.data[i + 2] = 0.0f;
                channel.data[i + 1] = 0.0f;
                channel.data[i + 0] = 0.0f;
                channel.data[i + 3] = 1.0f;
            }
        }
    }

    public static class Rotation2dInitializer
    implements ParallelArray.ChannelInitializer<ParallelArray.FloatChannel> {
        private static Rotation2dInitializer instance;

        public static Rotation2dInitializer get() {
            if (instance == null) {
                instance = new Rotation2dInitializer();
            }
            return instance;
        }

        @Override
        public void init(ParallelArray.FloatChannel channel) {
            int c = channel.data.length;
            for (int i = 0; i < c; i += channel.strideSize) {
                channel.data[i + 0] = 1.0f;
                channel.data[i + 1] = 0.0f;
            }
        }
    }

    public static class ScaleInitializer
    implements ParallelArray.ChannelInitializer<ParallelArray.FloatChannel> {
        private static ScaleInitializer instance;

        public static ScaleInitializer get() {
            if (instance == null) {
                instance = new ScaleInitializer();
            }
            return instance;
        }

        @Override
        public void init(ParallelArray.FloatChannel channel) {
            Arrays.fill(channel.data, 0, channel.data.length, 1.0f);
        }
    }

    public static class ColorInitializer
    implements ParallelArray.ChannelInitializer<ParallelArray.FloatChannel> {
        private static ColorInitializer instance;

        public static ColorInitializer get() {
            if (instance == null) {
                instance = new ColorInitializer();
            }
            return instance;
        }

        @Override
        public void init(ParallelArray.FloatChannel channel) {
            Arrays.fill(channel.data, 0, channel.data.length, 1.0f);
        }
    }

    public static class TextureRegionInitializer
    implements ParallelArray.ChannelInitializer<ParallelArray.FloatChannel> {
        private static TextureRegionInitializer instance;

        public static TextureRegionInitializer get() {
            if (instance == null) {
                instance = new TextureRegionInitializer();
            }
            return instance;
        }

        @Override
        public void init(ParallelArray.FloatChannel channel) {
            int c = channel.data.length;
            for (int i = 0; i < c; i += channel.strideSize) {
                channel.data[i + 0] = 0.0f;
                channel.data[i + 1] = 0.0f;
                channel.data[i + 2] = 1.0f;
                channel.data[i + 3] = 1.0f;
                channel.data[i + 4] = 0.5f;
                channel.data[i + 5] = 0.5f;
            }
        }
    }
}

