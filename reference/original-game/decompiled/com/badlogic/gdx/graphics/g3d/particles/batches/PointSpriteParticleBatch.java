/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.batches;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteControllerRenderData;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class PointSpriteParticleBatch
extends BufferedParticleBatch<PointSpriteControllerRenderData> {
    private static boolean pointSpritesEnabled = false;
    protected static final Vector3 TMP_V1 = new Vector3();
    protected static final int sizeAndRotationUsage = 512;
    protected static final VertexAttributes CPU_ATTRIBUTES = new VertexAttributes(new VertexAttribute(1, 3, "a_position"), new VertexAttribute(2, 4, "a_color"), new VertexAttribute(16, 4, "a_region"), new VertexAttribute(512, 3, "a_sizeAndRotation"));
    protected static final int CPU_VERTEX_SIZE = (short)(PointSpriteParticleBatch.CPU_ATTRIBUTES.vertexSize / 4);
    protected static final int CPU_POSITION_OFFSET = (short)(PointSpriteParticleBatch.CPU_ATTRIBUTES.findByUsage((int)1).offset / 4);
    protected static final int CPU_COLOR_OFFSET = (short)(PointSpriteParticleBatch.CPU_ATTRIBUTES.findByUsage((int)2).offset / 4);
    protected static final int CPU_REGION_OFFSET = (short)(PointSpriteParticleBatch.CPU_ATTRIBUTES.findByUsage((int)16).offset / 4);
    protected static final int CPU_SIZE_AND_ROTATION_OFFSET = (short)(PointSpriteParticleBatch.CPU_ATTRIBUTES.findByUsage((int)512).offset / 4);
    private float[] vertices;
    Renderable renderable;

    private static void enablePointSprites() {
        Gdx.gl.glEnable(34370);
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            Gdx.gl.glEnable(34913);
        }
        pointSpritesEnabled = true;
    }

    public PointSpriteParticleBatch() {
        this(1000);
    }

    public PointSpriteParticleBatch(int capacity) {
        this(capacity, new ParticleShader.Config(ParticleShader.ParticleType.Point));
    }

    public PointSpriteParticleBatch(int capacity, ParticleShader.Config shaderConfig) {
        super(PointSpriteControllerRenderData.class);
        if (!pointSpritesEnabled) {
            PointSpriteParticleBatch.enablePointSprites();
        }
        this.allocRenderable();
        this.ensureCapacity(capacity);
        this.renderable.shader = new ParticleShader(this.renderable, shaderConfig);
        this.renderable.shader.init();
    }

    @Override
    protected void allocParticlesData(int capacity) {
        this.vertices = new float[capacity * CPU_VERTEX_SIZE];
        if (this.renderable.meshPart.mesh != null) {
            this.renderable.meshPart.mesh.dispose();
        }
        this.renderable.meshPart.mesh = new Mesh(false, capacity, 0, CPU_ATTRIBUTES);
    }

    protected void allocRenderable() {
        this.renderable = new Renderable();
        this.renderable.meshPart.primitiveType = 0;
        this.renderable.meshPart.offset = 0;
        this.renderable.material = new Material(new BlendingAttribute(1, 771, 1.0f), new DepthTestAttribute(515, false), TextureAttribute.createDiffuse((Texture)null));
    }

    public void setTexture(Texture texture) {
        TextureAttribute attribute = (TextureAttribute)this.renderable.material.get(TextureAttribute.Diffuse);
        attribute.textureDescription.texture = texture;
    }

    public Texture getTexture() {
        TextureAttribute attribute = (TextureAttribute)this.renderable.material.get(TextureAttribute.Diffuse);
        return (Texture)attribute.textureDescription.texture;
    }

    @Override
    protected void flush(int[] offsets) {
        int tp = 0;
        for (PointSpriteControllerRenderData data : this.renderData) {
            ParallelArray.FloatChannel scaleChannel = data.scaleChannel;
            ParallelArray.FloatChannel regionChannel = data.regionChannel;
            ParallelArray.FloatChannel positionChannel = data.positionChannel;
            ParallelArray.FloatChannel colorChannel = data.colorChannel;
            ParallelArray.FloatChannel rotationChannel = data.rotationChannel;
            int p = 0;
            while (p < data.controller.particles.size) {
                int offset = offsets[tp] * CPU_VERTEX_SIZE;
                int regionOffset = p * regionChannel.strideSize;
                int positionOffset = p * positionChannel.strideSize;
                int colorOffset = p * colorChannel.strideSize;
                int rotationOffset = p * rotationChannel.strideSize;
                this.vertices[offset + PointSpriteParticleBatch.CPU_POSITION_OFFSET] = positionChannel.data[positionOffset + 0];
                this.vertices[offset + PointSpriteParticleBatch.CPU_POSITION_OFFSET + 1] = positionChannel.data[positionOffset + 1];
                this.vertices[offset + PointSpriteParticleBatch.CPU_POSITION_OFFSET + 2] = positionChannel.data[positionOffset + 2];
                this.vertices[offset + PointSpriteParticleBatch.CPU_COLOR_OFFSET] = colorChannel.data[colorOffset + 0];
                this.vertices[offset + PointSpriteParticleBatch.CPU_COLOR_OFFSET + 1] = colorChannel.data[colorOffset + 1];
                this.vertices[offset + PointSpriteParticleBatch.CPU_COLOR_OFFSET + 2] = colorChannel.data[colorOffset + 2];
                this.vertices[offset + PointSpriteParticleBatch.CPU_COLOR_OFFSET + 3] = colorChannel.data[colorOffset + 3];
                this.vertices[offset + PointSpriteParticleBatch.CPU_SIZE_AND_ROTATION_OFFSET] = scaleChannel.data[p * scaleChannel.strideSize];
                this.vertices[offset + PointSpriteParticleBatch.CPU_SIZE_AND_ROTATION_OFFSET + 1] = rotationChannel.data[rotationOffset + 0];
                this.vertices[offset + PointSpriteParticleBatch.CPU_SIZE_AND_ROTATION_OFFSET + 2] = rotationChannel.data[rotationOffset + 1];
                this.vertices[offset + PointSpriteParticleBatch.CPU_REGION_OFFSET] = regionChannel.data[regionOffset + 0];
                this.vertices[offset + PointSpriteParticleBatch.CPU_REGION_OFFSET + 1] = regionChannel.data[regionOffset + 1];
                this.vertices[offset + PointSpriteParticleBatch.CPU_REGION_OFFSET + 2] = regionChannel.data[regionOffset + 2];
                this.vertices[offset + PointSpriteParticleBatch.CPU_REGION_OFFSET + 3] = regionChannel.data[regionOffset + 3];
                ++p;
                ++tp;
            }
        }
        this.renderable.meshPart.size = this.bufferedParticlesCount;
        this.renderable.meshPart.mesh.setVertices(this.vertices, 0, this.bufferedParticlesCount * CPU_VERTEX_SIZE);
        this.renderable.meshPart.update();
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        if (this.bufferedParticlesCount > 0) {
            renderables.add(pool.obtain().set(this.renderable));
        }
    }

    @Override
    public void save(AssetManager manager, ResourceData resources) {
        ResourceData.SaveData data = resources.createSaveData("pointSpriteBatch");
        data.saveAsset(manager.getAssetFileName(this.getTexture()), Texture.class);
    }

    @Override
    public void load(AssetManager manager, ResourceData resources) {
        ResourceData.SaveData data = resources.getSaveData("pointSpriteBatch");
        if (data != null) {
            this.setTexture((Texture)manager.get(data.loadAsset()));
        }
    }
}

