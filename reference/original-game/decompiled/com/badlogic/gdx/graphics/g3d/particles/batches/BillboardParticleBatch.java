/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles.batches;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray;
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.graphics.g3d.particles.batches.BufferedParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardControllerRenderData;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class BillboardParticleBatch
extends BufferedParticleBatch<BillboardControllerRenderData> {
    protected static final Vector3 TMP_V1 = new Vector3();
    protected static final Vector3 TMP_V2 = new Vector3();
    protected static final Vector3 TMP_V3 = new Vector3();
    protected static final Vector3 TMP_V4 = new Vector3();
    protected static final Vector3 TMP_V5 = new Vector3();
    protected static final Vector3 TMP_V6 = new Vector3();
    protected static final Matrix3 TMP_M3 = new Matrix3();
    protected static final int sizeAndRotationUsage = 512;
    protected static final int directionUsage = 1024;
    private static final VertexAttributes GPU_ATTRIBUTES = new VertexAttributes(new VertexAttribute(1, 3, "a_position"), new VertexAttribute(16, 2, "a_texCoord0"), new VertexAttribute(2, 4, "a_color"), new VertexAttribute(512, 4, "a_sizeAndRotation"));
    private static final VertexAttributes CPU_ATTRIBUTES = new VertexAttributes(new VertexAttribute(1, 3, "a_position"), new VertexAttribute(16, 2, "a_texCoord0"), new VertexAttribute(2, 4, "a_color"));
    private static final int GPU_POSITION_OFFSET = (short)(BillboardParticleBatch.GPU_ATTRIBUTES.findByUsage((int)1).offset / 4);
    private static final int GPU_UV_OFFSET = (short)(BillboardParticleBatch.GPU_ATTRIBUTES.findByUsage((int)16).offset / 4);
    private static final int GPU_SIZE_ROTATION_OFFSET = (short)(BillboardParticleBatch.GPU_ATTRIBUTES.findByUsage((int)512).offset / 4);
    private static final int GPU_COLOR_OFFSET = (short)(BillboardParticleBatch.GPU_ATTRIBUTES.findByUsage((int)2).offset / 4);
    private static final int GPU_VERTEX_SIZE = BillboardParticleBatch.GPU_ATTRIBUTES.vertexSize / 4;
    private static final int CPU_POSITION_OFFSET = (short)(BillboardParticleBatch.CPU_ATTRIBUTES.findByUsage((int)1).offset / 4);
    private static final int CPU_UV_OFFSET = (short)(BillboardParticleBatch.CPU_ATTRIBUTES.findByUsage((int)16).offset / 4);
    private static final int CPU_COLOR_OFFSET = (short)(BillboardParticleBatch.CPU_ATTRIBUTES.findByUsage((int)2).offset / 4);
    private static final int CPU_VERTEX_SIZE = BillboardParticleBatch.CPU_ATTRIBUTES.vertexSize / 4;
    private static final int MAX_PARTICLES_PER_MESH = 8191;
    private static final int MAX_VERTICES_PER_MESH = 32764;
    private RenderablePool renderablePool;
    private Array<Renderable> renderables;
    private float[] vertices;
    private short[] indices;
    private int currentVertexSize = 0;
    private VertexAttributes currentAttributes;
    protected boolean useGPU = false;
    protected ParticleShader.AlignMode mode = ParticleShader.AlignMode.Screen;
    protected Texture texture;
    protected BlendingAttribute blendingAttribute;
    protected DepthTestAttribute depthTestAttribute;
    Shader shader;

    public BillboardParticleBatch(ParticleShader.AlignMode mode, boolean useGPU, int capacity, BlendingAttribute blendingAttribute, DepthTestAttribute depthTestAttribute) {
        super(BillboardControllerRenderData.class);
        this.renderables = new Array();
        this.renderablePool = new RenderablePool();
        this.blendingAttribute = blendingAttribute;
        this.depthTestAttribute = depthTestAttribute;
        if (this.blendingAttribute == null) {
            this.blendingAttribute = new BlendingAttribute(1, 771, 1.0f);
        }
        if (this.depthTestAttribute == null) {
            this.depthTestAttribute = new DepthTestAttribute(515, false);
        }
        this.allocIndices();
        this.initRenderData();
        this.ensureCapacity(capacity);
        this.setUseGpu(useGPU);
        this.setAlignMode(mode);
    }

    public BillboardParticleBatch(ParticleShader.AlignMode mode, boolean useGPU, int capacity) {
        this(mode, useGPU, capacity, null, null);
    }

    public BillboardParticleBatch() {
        this(ParticleShader.AlignMode.Screen, false, 100);
    }

    public BillboardParticleBatch(int capacity) {
        this(ParticleShader.AlignMode.Screen, false, capacity);
    }

    @Override
    public void allocParticlesData(int capacity) {
        this.vertices = new float[this.currentVertexSize * 4 * capacity];
        this.allocRenderables(capacity);
    }

    protected Renderable allocRenderable() {
        Renderable renderable = new Renderable();
        renderable.meshPart.primitiveType = 4;
        renderable.meshPart.offset = 0;
        renderable.material = new Material(this.blendingAttribute, this.depthTestAttribute, TextureAttribute.createDiffuse(this.texture));
        renderable.meshPart.mesh = new Mesh(false, 32764, 49146, this.currentAttributes);
        renderable.meshPart.mesh.setIndices(this.indices);
        renderable.shader = this.shader;
        return renderable;
    }

    private void allocIndices() {
        int indicesCount = 49146;
        this.indices = new short[indicesCount];
        int i = 0;
        int vertex = 0;
        while (i < indicesCount) {
            this.indices[i] = (short)vertex;
            this.indices[i + 1] = (short)(vertex + 1);
            this.indices[i + 2] = (short)(vertex + 2);
            this.indices[i + 3] = (short)(vertex + 2);
            this.indices[i + 4] = (short)(vertex + 3);
            this.indices[i + 5] = (short)vertex;
            i += 6;
            vertex += 4;
        }
    }

    private void allocRenderables(int capacity) {
        int meshCount = MathUtils.ceil(capacity / 8191);
        int free = this.renderablePool.getFree();
        if (free < meshCount) {
            int left = meshCount - free;
            for (int i = 0; i < left; ++i) {
                this.renderablePool.free(this.renderablePool.newObject());
            }
        }
    }

    private Shader getShader(Renderable renderable) {
        BaseShader shader = this.useGPU ? new ParticleShader(renderable, new ParticleShader.Config(this.mode)) : new DefaultShader(renderable);
        shader.init();
        return shader;
    }

    private void allocShader() {
        Renderable newRenderable = this.allocRenderable();
        this.shader = newRenderable.shader = this.getShader(newRenderable);
        this.renderablePool.free(newRenderable);
    }

    private void clearRenderablesPool() {
        this.renderablePool.freeAll(this.renderables);
        int free = this.renderablePool.getFree();
        for (int i = 0; i < free; ++i) {
            Renderable renderable = (Renderable)this.renderablePool.obtain();
            renderable.meshPart.mesh.dispose();
        }
        this.renderables.clear();
    }

    public void setVertexData() {
        if (this.useGPU) {
            this.currentAttributes = GPU_ATTRIBUTES;
            this.currentVertexSize = GPU_VERTEX_SIZE;
        } else {
            this.currentAttributes = CPU_ATTRIBUTES;
            this.currentVertexSize = CPU_VERTEX_SIZE;
        }
    }

    private void initRenderData() {
        this.setVertexData();
        this.clearRenderablesPool();
        this.allocShader();
        this.resetCapacity();
    }

    public void setAlignMode(ParticleShader.AlignMode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            if (this.useGPU) {
                this.initRenderData();
                this.allocRenderables(this.bufferedParticlesCount);
            }
        }
    }

    public ParticleShader.AlignMode getAlignMode() {
        return this.mode;
    }

    public void setUseGpu(boolean useGPU) {
        if (this.useGPU != useGPU) {
            this.useGPU = useGPU;
            this.initRenderData();
            this.allocRenderables(this.bufferedParticlesCount);
        }
    }

    public boolean isUseGPU() {
        return this.useGPU;
    }

    public void setTexture(Texture texture) {
        this.renderablePool.freeAll(this.renderables);
        this.renderables.clear();
        int free = this.renderablePool.getFree();
        for (int i = 0; i < free; ++i) {
            Renderable renderable = (Renderable)this.renderablePool.obtain();
            TextureAttribute attribute = (TextureAttribute)renderable.material.get(TextureAttribute.Diffuse);
            attribute.textureDescription.texture = texture;
        }
        this.texture = texture;
    }

    public Texture getTexture() {
        return this.texture;
    }

    @Override
    public void begin() {
        super.begin();
        this.renderablePool.freeAll(this.renderables);
        this.renderables.clear();
    }

    private static void putVertex(float[] vertices, int offset, float x, float y, float z, float u, float v, float scaleX, float scaleY, float cosRotation, float sinRotation, float r, float g, float b, float a) {
        vertices[offset + BillboardParticleBatch.GPU_POSITION_OFFSET] = x;
        vertices[offset + BillboardParticleBatch.GPU_POSITION_OFFSET + 1] = y;
        vertices[offset + BillboardParticleBatch.GPU_POSITION_OFFSET + 2] = z;
        vertices[offset + BillboardParticleBatch.GPU_UV_OFFSET] = u;
        vertices[offset + BillboardParticleBatch.GPU_UV_OFFSET + 1] = v;
        vertices[offset + BillboardParticleBatch.GPU_SIZE_ROTATION_OFFSET] = scaleX;
        vertices[offset + BillboardParticleBatch.GPU_SIZE_ROTATION_OFFSET + 1] = scaleY;
        vertices[offset + BillboardParticleBatch.GPU_SIZE_ROTATION_OFFSET + 2] = cosRotation;
        vertices[offset + BillboardParticleBatch.GPU_SIZE_ROTATION_OFFSET + 3] = sinRotation;
        vertices[offset + BillboardParticleBatch.GPU_COLOR_OFFSET] = r;
        vertices[offset + BillboardParticleBatch.GPU_COLOR_OFFSET + 1] = g;
        vertices[offset + BillboardParticleBatch.GPU_COLOR_OFFSET + 2] = b;
        vertices[offset + BillboardParticleBatch.GPU_COLOR_OFFSET + 3] = a;
    }

    private static void putVertex(float[] vertices, int offset, Vector3 p, float u, float v, float r, float g, float b, float a) {
        vertices[offset + BillboardParticleBatch.CPU_POSITION_OFFSET] = p.x;
        vertices[offset + BillboardParticleBatch.CPU_POSITION_OFFSET + 1] = p.y;
        vertices[offset + BillboardParticleBatch.CPU_POSITION_OFFSET + 2] = p.z;
        vertices[offset + BillboardParticleBatch.CPU_UV_OFFSET] = u;
        vertices[offset + BillboardParticleBatch.CPU_UV_OFFSET + 1] = v;
        vertices[offset + BillboardParticleBatch.CPU_COLOR_OFFSET] = r;
        vertices[offset + BillboardParticleBatch.CPU_COLOR_OFFSET + 1] = g;
        vertices[offset + BillboardParticleBatch.CPU_COLOR_OFFSET + 2] = b;
        vertices[offset + BillboardParticleBatch.CPU_COLOR_OFFSET + 3] = a;
    }

    private void fillVerticesGPU(int[] particlesOffset) {
        int tp = 0;
        for (BillboardControllerRenderData data : this.renderData) {
            ParallelArray.FloatChannel scaleChannel = data.scaleChannel;
            ParallelArray.FloatChannel regionChannel = data.regionChannel;
            ParallelArray.FloatChannel positionChannel = data.positionChannel;
            ParallelArray.FloatChannel colorChannel = data.colorChannel;
            ParallelArray.FloatChannel rotationChannel = data.rotationChannel;
            int p = 0;
            int c = data.controller.particles.size;
            while (p < c) {
                int baseOffset = particlesOffset[tp] * this.currentVertexSize * 4;
                float scale = scaleChannel.data[p * scaleChannel.strideSize];
                int regionOffset = p * regionChannel.strideSize;
                int positionOffset = p * positionChannel.strideSize;
                int colorOffset = p * colorChannel.strideSize;
                int rotationOffset = p * rotationChannel.strideSize;
                float px = positionChannel.data[positionOffset + 0];
                float py = positionChannel.data[positionOffset + 1];
                float pz = positionChannel.data[positionOffset + 2];
                float u = regionChannel.data[regionOffset + 0];
                float v = regionChannel.data[regionOffset + 1];
                float u2 = regionChannel.data[regionOffset + 2];
                float v2 = regionChannel.data[regionOffset + 3];
                float sx = regionChannel.data[regionOffset + 4] * scale;
                float sy = regionChannel.data[regionOffset + 5] * scale;
                float r = colorChannel.data[colorOffset + 0];
                float g = colorChannel.data[colorOffset + 1];
                float b = colorChannel.data[colorOffset + 2];
                float a = colorChannel.data[colorOffset + 3];
                float cosRotation = rotationChannel.data[rotationOffset + 0];
                float sinRotation = rotationChannel.data[rotationOffset + 1];
                BillboardParticleBatch.putVertex(this.vertices, baseOffset, px, py, pz, u, v2, -sx, -sy, cosRotation, sinRotation, r, g, b, a);
                BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, px, py, pz, u2, v2, sx, -sy, cosRotation, sinRotation, r, g, b, a);
                BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, px, py, pz, u2, v, sx, sy, cosRotation, sinRotation, r, g, b, a);
                BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, px, py, pz, u, v, -sx, sy, cosRotation, sinRotation, r, g, b, a);
                baseOffset += this.currentVertexSize;
                ++p;
                ++tp;
            }
        }
    }

    private void fillVerticesToViewPointCPU(int[] particlesOffset) {
        int tp = 0;
        for (BillboardControllerRenderData data : this.renderData) {
            ParallelArray.FloatChannel scaleChannel = data.scaleChannel;
            ParallelArray.FloatChannel regionChannel = data.regionChannel;
            ParallelArray.FloatChannel positionChannel = data.positionChannel;
            ParallelArray.FloatChannel colorChannel = data.colorChannel;
            ParallelArray.FloatChannel rotationChannel = data.rotationChannel;
            int p = 0;
            int c = data.controller.particles.size;
            while (p < c) {
                int baseOffset = particlesOffset[tp] * this.currentVertexSize * 4;
                float scale = scaleChannel.data[p * scaleChannel.strideSize];
                int regionOffset = p * regionChannel.strideSize;
                int positionOffset = p * positionChannel.strideSize;
                int colorOffset = p * colorChannel.strideSize;
                int rotationOffset = p * rotationChannel.strideSize;
                float px = positionChannel.data[positionOffset + 0];
                float py = positionChannel.data[positionOffset + 1];
                float pz = positionChannel.data[positionOffset + 2];
                float u = regionChannel.data[regionOffset + 0];
                float v = regionChannel.data[regionOffset + 1];
                float u2 = regionChannel.data[regionOffset + 2];
                float v2 = regionChannel.data[regionOffset + 3];
                float sx = regionChannel.data[regionOffset + 4] * scale;
                float sy = regionChannel.data[regionOffset + 5] * scale;
                float r = colorChannel.data[colorOffset + 0];
                float g = colorChannel.data[colorOffset + 1];
                float b = colorChannel.data[colorOffset + 2];
                float a = colorChannel.data[colorOffset + 3];
                float cosRotation = rotationChannel.data[rotationOffset + 0];
                float sinRotation = rotationChannel.data[rotationOffset + 1];
                Vector3 look = TMP_V3.set(this.camera.position).sub(px, py, pz).nor();
                Vector3 right = TMP_V1.set(this.camera.up).crs(look).nor();
                Vector3 up = TMP_V2.set(look).crs(right);
                right.scl(sx);
                up.scl(sy);
                if (cosRotation != 1.0f) {
                    TMP_M3.setToRotation(look, cosRotation, sinRotation);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x, -BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y, -BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x, BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y, BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x, BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y, BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x, -BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y, -BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v, r, g, b, a);
                } else {
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x + px, -BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y + py, -BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z + pz), u, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x + px, BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y + py, BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z + pz), u2, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x + px, BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y + py, BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z + pz), u2, v, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x + px, -BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y + py, -BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z + pz), u, v, r, g, b, a);
                }
                ++p;
                ++tp;
            }
        }
    }

    private void fillVerticesToScreenCPU(int[] particlesOffset) {
        Vector3 look = TMP_V3.set(this.camera.direction).scl(-1.0f);
        Vector3 right = TMP_V4.set(this.camera.up).crs(look).nor();
        Vector3 up = this.camera.up;
        int tp = 0;
        for (BillboardControllerRenderData data : this.renderData) {
            ParallelArray.FloatChannel scaleChannel = data.scaleChannel;
            ParallelArray.FloatChannel regionChannel = data.regionChannel;
            ParallelArray.FloatChannel positionChannel = data.positionChannel;
            ParallelArray.FloatChannel colorChannel = data.colorChannel;
            ParallelArray.FloatChannel rotationChannel = data.rotationChannel;
            int p = 0;
            int c = data.controller.particles.size;
            while (p < c) {
                int baseOffset = particlesOffset[tp] * this.currentVertexSize * 4;
                float scale = scaleChannel.data[p * scaleChannel.strideSize];
                int regionOffset = p * regionChannel.strideSize;
                int positionOffset = p * positionChannel.strideSize;
                int colorOffset = p * colorChannel.strideSize;
                int rotationOffset = p * rotationChannel.strideSize;
                float px = positionChannel.data[positionOffset + 0];
                float py = positionChannel.data[positionOffset + 1];
                float pz = positionChannel.data[positionOffset + 2];
                float u = regionChannel.data[regionOffset + 0];
                float v = regionChannel.data[regionOffset + 1];
                float u2 = regionChannel.data[regionOffset + 2];
                float v2 = regionChannel.data[regionOffset + 3];
                float sx = regionChannel.data[regionOffset + 4] * scale;
                float sy = regionChannel.data[regionOffset + 5] * scale;
                float r = colorChannel.data[colorOffset + 0];
                float g = colorChannel.data[colorOffset + 1];
                float b = colorChannel.data[colorOffset + 2];
                float a = colorChannel.data[colorOffset + 3];
                float cosRotation = rotationChannel.data[rotationOffset + 0];
                float sinRotation = rotationChannel.data[rotationOffset + 1];
                TMP_V1.set(right).scl(sx);
                TMP_V2.set(up).scl(sy);
                if (cosRotation != 1.0f) {
                    TMP_M3.setToRotation(look, cosRotation, sinRotation);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x, -BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y, -BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x, BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y, BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x, BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y, BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u2, v, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x, -BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y, -BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z).mul(TMP_M3).add(px, py, pz), u, v, r, g, b, a);
                } else {
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x + px, -BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y + py, -BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z + pz), u, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x - BillboardParticleBatch.TMP_V2.x + px, BillboardParticleBatch.TMP_V1.y - BillboardParticleBatch.TMP_V2.y + py, BillboardParticleBatch.TMP_V1.z - BillboardParticleBatch.TMP_V2.z + pz), u2, v2, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x + px, BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y + py, BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z + pz), u2, v, r, g, b, a);
                    BillboardParticleBatch.putVertex(this.vertices, baseOffset += this.currentVertexSize, TMP_V6.set(-BillboardParticleBatch.TMP_V1.x + BillboardParticleBatch.TMP_V2.x + px, -BillboardParticleBatch.TMP_V1.y + BillboardParticleBatch.TMP_V2.y + py, -BillboardParticleBatch.TMP_V1.z + BillboardParticleBatch.TMP_V2.z + pz), u, v, r, g, b, a);
                }
                ++p;
                ++tp;
            }
        }
    }

    @Override
    protected void flush(int[] offsets) {
        if (this.useGPU) {
            this.fillVerticesGPU(offsets);
        } else if (this.mode == ParticleShader.AlignMode.Screen) {
            this.fillVerticesToScreenCPU(offsets);
        } else if (this.mode == ParticleShader.AlignMode.ViewPoint) {
            this.fillVerticesToViewPointCPU(offsets);
        }
        int addedVertexCount = 0;
        int vCount = this.bufferedParticlesCount * 4;
        for (int v = 0; v < vCount; v += addedVertexCount) {
            addedVertexCount = Math.min(vCount - v, 32764);
            Renderable renderable = (Renderable)this.renderablePool.obtain();
            renderable.meshPart.size = addedVertexCount / 4 * 6;
            renderable.meshPart.mesh.setVertices(this.vertices, this.currentVertexSize * v, this.currentVertexSize * addedVertexCount);
            renderable.meshPart.update();
            this.renderables.add(renderable);
        }
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        for (Renderable renderable : this.renderables) {
            renderables.add(pool.obtain().set(renderable));
        }
    }

    @Override
    public void save(AssetManager manager, ResourceData resources) {
        ResourceData.SaveData data = resources.createSaveData("billboardBatch");
        data.save("cfg", new Config(this.useGPU, this.mode));
        data.saveAsset(manager.getAssetFileName(this.texture), Texture.class);
    }

    @Override
    public void load(AssetManager manager, ResourceData resources) {
        ResourceData.SaveData data = resources.getSaveData("billboardBatch");
        if (data != null) {
            this.setTexture((Texture)manager.get(data.loadAsset()));
            Config cfg = (Config)data.load("cfg");
            this.setUseGpu(cfg.useGPU);
            this.setAlignMode(cfg.mode);
        }
    }

    public static class Config {
        boolean useGPU;
        ParticleShader.AlignMode mode;

        public Config() {
        }

        public Config(boolean useGPU, ParticleShader.AlignMode mode) {
            this.useGPU = useGPU;
            this.mode = mode;
        }
    }

    private class RenderablePool
    extends Pool<Renderable> {
        @Override
        public Renderable newObject() {
            return BillboardParticleBatch.this.allocRenderable();
        }
    }
}

