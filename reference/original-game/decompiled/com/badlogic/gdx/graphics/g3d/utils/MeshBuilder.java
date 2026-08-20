/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CapsuleShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.PatchShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ShortArray;

public class MeshBuilder
implements MeshPartBuilder {
    private static final ShortArray tmpIndices = new ShortArray();
    private static final FloatArray tmpVertices = new FloatArray();
    private final MeshPartBuilder.VertexInfo vertTmp1 = new MeshPartBuilder.VertexInfo();
    private final MeshPartBuilder.VertexInfo vertTmp2 = new MeshPartBuilder.VertexInfo();
    private final MeshPartBuilder.VertexInfo vertTmp3 = new MeshPartBuilder.VertexInfo();
    private final MeshPartBuilder.VertexInfo vertTmp4 = new MeshPartBuilder.VertexInfo();
    private final Color tempC1 = new Color();
    private VertexAttributes attributes;
    private FloatArray vertices = new FloatArray();
    private ShortArray indices = new ShortArray();
    private int stride;
    private short vindex;
    private int istart;
    private int posOffset;
    private int posSize;
    private int norOffset;
    private int biNorOffset;
    private int tangentOffset;
    private int colOffset;
    private int colSize;
    private int cpOffset;
    private int uvOffset;
    private MeshPart part;
    private Array<MeshPart> parts = new Array();
    private final Color color = new Color(Color.WHITE);
    private boolean hasColor = false;
    private int primitiveType;
    private float uOffset = 0.0f;
    private float uScale = 1.0f;
    private float vOffset = 0.0f;
    private float vScale = 1.0f;
    private boolean hasUVTransform = false;
    private float[] vertex;
    private boolean vertexTransformationEnabled = false;
    private final Matrix4 positionTransform = new Matrix4();
    private final Matrix3 normalTransform = new Matrix3();
    private final BoundingBox bounds = new BoundingBox();
    private short lastIndex = (short)-1;
    private static final Vector3 vTmp = new Vector3();
    private final Vector3 tmpNormal = new Vector3();
    private static IntIntMap indicesMap = null;

    public static VertexAttributes createAttributes(long usage) {
        Array<VertexAttribute> attrs = new Array<VertexAttribute>();
        if ((usage & 1L) == 1L) {
            attrs.add(new VertexAttribute(1, 3, "a_position"));
        }
        if ((usage & 2L) == 2L) {
            attrs.add(new VertexAttribute(2, 4, "a_color"));
        }
        if ((usage & 4L) == 4L) {
            attrs.add(new VertexAttribute(4, 4, "a_color"));
        }
        if ((usage & 8L) == 8L) {
            attrs.add(new VertexAttribute(8, 3, "a_normal"));
        }
        if ((usage & 0x10L) == 16L) {
            attrs.add(new VertexAttribute(16, 2, "a_texCoord0"));
        }
        VertexAttribute[] attributes = new VertexAttribute[attrs.size];
        for (int i = 0; i < attributes.length; ++i) {
            attributes[i] = (VertexAttribute)attrs.get(i);
        }
        return new VertexAttributes(attributes);
    }

    public void begin(long attributes) {
        this.begin(MeshBuilder.createAttributes(attributes), -1);
    }

    public void begin(VertexAttributes attributes) {
        this.begin(attributes, -1);
    }

    public void begin(long attributes, int primitiveType) {
        this.begin(MeshBuilder.createAttributes(attributes), primitiveType);
    }

    public void begin(VertexAttributes attributes, int primitiveType) {
        VertexAttribute a;
        if (this.attributes != null) {
            throw new RuntimeException("Call end() first");
        }
        this.attributes = attributes;
        this.vertices.clear();
        this.indices.clear();
        this.parts.clear();
        this.vindex = 0;
        this.lastIndex = (short)-1;
        this.istart = 0;
        this.part = null;
        this.stride = attributes.vertexSize / 4;
        if (this.vertex == null || this.vertex.length < this.stride) {
            this.vertex = new float[this.stride];
        }
        if ((a = attributes.findByUsage(1)) == null) {
            throw new GdxRuntimeException("Cannot build mesh without position attribute");
        }
        this.posOffset = a.offset / 4;
        this.posSize = a.numComponents;
        a = attributes.findByUsage(8);
        this.norOffset = a == null ? -1 : a.offset / 4;
        a = attributes.findByUsage(256);
        this.biNorOffset = a == null ? -1 : a.offset / 4;
        a = attributes.findByUsage(128);
        this.tangentOffset = a == null ? -1 : a.offset / 4;
        a = attributes.findByUsage(2);
        this.colOffset = a == null ? -1 : a.offset / 4;
        this.colSize = a == null ? 0 : a.numComponents;
        a = attributes.findByUsage(4);
        this.cpOffset = a == null ? -1 : a.offset / 4;
        a = attributes.findByUsage(16);
        this.uvOffset = a == null ? -1 : a.offset / 4;
        this.setColor(null);
        this.setVertexTransform(null);
        this.setUVRange(null);
        this.primitiveType = primitiveType;
        this.bounds.inf();
    }

    private void endpart() {
        if (this.part != null) {
            this.bounds.getCenter(this.part.center);
            this.bounds.getDimensions(this.part.halfExtents).scl(0.5f);
            this.part.radius = this.part.halfExtents.len();
            this.bounds.inf();
            this.part.offset = this.istart;
            this.part.size = this.indices.size - this.istart;
            this.istart = this.indices.size;
            this.part = null;
        }
    }

    public MeshPart part(String id, int primitiveType) {
        return this.part(id, primitiveType, new MeshPart());
    }

    public MeshPart part(String id, int primitiveType, MeshPart meshPart) {
        if (this.attributes == null) {
            throw new RuntimeException("Call begin() first");
        }
        this.endpart();
        this.part = meshPart;
        this.part.id = id;
        this.primitiveType = this.part.primitiveType = primitiveType;
        this.parts.add(this.part);
        this.setColor(null);
        this.setVertexTransform(null);
        this.setUVRange(null);
        return this.part;
    }

    public Mesh end(Mesh mesh) {
        this.endpart();
        if (this.attributes == null) {
            throw new GdxRuntimeException("Call begin() first");
        }
        if (!this.attributes.equals(mesh.getVertexAttributes())) {
            throw new GdxRuntimeException("Mesh attributes don't match");
        }
        if (mesh.getMaxVertices() * this.stride < this.vertices.size) {
            throw new GdxRuntimeException("Mesh can't hold enough vertices: " + mesh.getMaxVertices() + " * " + this.stride + " < " + this.vertices.size);
        }
        if (mesh.getMaxIndices() < this.indices.size) {
            throw new GdxRuntimeException("Mesh can't hold enough indices: " + mesh.getMaxIndices() + " < " + this.indices.size);
        }
        mesh.setVertices(this.vertices.items, 0, this.vertices.size);
        mesh.setIndices(this.indices.items, 0, this.indices.size);
        for (MeshPart p : this.parts) {
            p.mesh = mesh;
        }
        this.parts.clear();
        this.attributes = null;
        this.vertices.clear();
        this.indices.clear();
        return mesh;
    }

    public Mesh end() {
        return this.end(new Mesh(true, this.vertices.size / this.stride, this.indices.size, this.attributes));
    }

    public void clear() {
        this.vertices.clear();
        this.indices.clear();
        this.parts.clear();
        this.vindex = 0;
        this.lastIndex = (short)-1;
        this.istart = 0;
        this.part = null;
    }

    public int getFloatsPerVertex() {
        return this.stride;
    }

    public int getNumVertices() {
        return this.vertices.size / this.stride;
    }

    public void getVertices(float[] out, int destOffset) {
        if (this.attributes == null) {
            throw new GdxRuntimeException("Must be called in between #begin and #end");
        }
        if (destOffset < 0 || destOffset > out.length - this.vertices.size) {
            throw new GdxRuntimeException("Array to small or offset out of range");
        }
        System.arraycopy(this.vertices.items, 0, out, destOffset, this.vertices.size);
    }

    protected float[] getVertices() {
        return this.vertices.items;
    }

    public int getNumIndices() {
        return this.indices.size;
    }

    public void getIndices(short[] out, int destOffset) {
        if (this.attributes == null) {
            throw new GdxRuntimeException("Must be called in between #begin and #end");
        }
        if (destOffset < 0 || destOffset > out.length - this.indices.size) {
            throw new GdxRuntimeException("Array to small or offset out of range");
        }
        System.arraycopy(this.indices.items, 0, out, destOffset, this.indices.size);
    }

    protected short[] getIndices() {
        return this.indices.items;
    }

    @Override
    public VertexAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public MeshPart getMeshPart() {
        return this.part;
    }

    @Override
    public int getPrimitiveType() {
        return this.primitiveType;
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        this.hasColor = !this.color.equals(Color.WHITE);
    }

    @Override
    public void setColor(Color color) {
        this.hasColor = color != null;
        this.color.set(!this.hasColor ? Color.WHITE : color);
    }

    @Override
    public void setUVRange(float u1, float v1, float u2, float v2) {
        this.uOffset = u1;
        this.vOffset = v1;
        this.uScale = u2 - u1;
        this.vScale = v2 - v1;
        this.hasUVTransform = !MathUtils.isZero(u1) || !MathUtils.isZero(v1) || !MathUtils.isEqual(u2, 1.0f) || !MathUtils.isEqual(v2, 1.0f);
    }

    @Override
    public void setUVRange(TextureRegion region) {
        this.hasUVTransform = region != null;
        if (!this.hasUVTransform) {
            this.vOffset = 0.0f;
            this.uOffset = 0.0f;
            this.vScale = 1.0f;
            this.uScale = 1.0f;
        } else {
            this.setUVRange(region.getU(), region.getV(), region.getU2(), region.getV2());
        }
    }

    @Override
    public Matrix4 getVertexTransform(Matrix4 out) {
        return out.set(this.positionTransform);
    }

    @Override
    public void setVertexTransform(Matrix4 transform) {
        this.vertexTransformationEnabled = transform != null;
        if (this.vertexTransformationEnabled) {
            this.positionTransform.set(transform);
            this.normalTransform.set(transform).inv().transpose();
        } else {
            this.positionTransform.idt();
            this.normalTransform.idt();
        }
    }

    @Override
    public boolean isVertexTransformationEnabled() {
        return this.vertexTransformationEnabled;
    }

    @Override
    public void setVertexTransformationEnabled(boolean enabled) {
        this.vertexTransformationEnabled = enabled;
    }

    @Override
    public void ensureVertices(int numVertices) {
        this.vertices.ensureCapacity(this.stride * numVertices);
    }

    @Override
    public void ensureIndices(int numIndices) {
        this.indices.ensureCapacity(numIndices);
    }

    @Override
    public void ensureCapacity(int numVertices, int numIndices) {
        this.ensureVertices(numVertices);
        this.ensureIndices(numIndices);
    }

    @Override
    public void ensureTriangleIndices(int numTriangles) {
        if (this.primitiveType == 1) {
            this.ensureIndices(6 * numTriangles);
        } else if (this.primitiveType == 4 || this.primitiveType == 0) {
            this.ensureIndices(3 * numTriangles);
        } else {
            throw new GdxRuntimeException("Incorrect primtive type");
        }
    }

    @Deprecated
    public void ensureTriangles(int numVertices, int numTriangles) {
        this.ensureVertices(numVertices);
        this.ensureTriangleIndices(numTriangles);
    }

    @Deprecated
    public void ensureTriangles(int numTriangles) {
        this.ensureVertices(3 * numTriangles);
        this.ensureTriangleIndices(numTriangles);
    }

    @Override
    public void ensureRectangleIndices(int numRectangles) {
        if (this.primitiveType == 0) {
            this.ensureIndices(4 * numRectangles);
        } else if (this.primitiveType == 1) {
            this.ensureIndices(8 * numRectangles);
        } else {
            this.ensureIndices(6 * numRectangles);
        }
    }

    @Deprecated
    public void ensureRectangles(int numVertices, int numRectangles) {
        this.ensureVertices(numVertices);
        this.ensureRectangleIndices(numRectangles);
    }

    public void ensureRectangles(int numRectangles) {
        this.ensureVertices(4 * numRectangles);
        this.ensureRectangleIndices(numRectangles);
    }

    @Override
    public short lastIndex() {
        return this.lastIndex;
    }

    private static final void transformPosition(float[] values, int offset, int size, Matrix4 transform) {
        if (size > 2) {
            vTmp.set(values[offset], values[offset + 1], values[offset + 2]).mul(transform);
            values[offset] = MeshBuilder.vTmp.x;
            values[offset + 1] = MeshBuilder.vTmp.y;
            values[offset + 2] = MeshBuilder.vTmp.z;
        } else if (size > 1) {
            vTmp.set(values[offset], values[offset + 1], 0.0f).mul(transform);
            values[offset] = MeshBuilder.vTmp.x;
            values[offset + 1] = MeshBuilder.vTmp.y;
        } else {
            values[offset] = MeshBuilder.vTmp.set((float)values[offset], (float)0.0f, (float)0.0f).mul((Matrix4)transform).x;
        }
    }

    private static final void transformNormal(float[] values, int offset, int size, Matrix3 transform) {
        if (size > 2) {
            vTmp.set(values[offset], values[offset + 1], values[offset + 2]).mul(transform).nor();
            values[offset] = MeshBuilder.vTmp.x;
            values[offset + 1] = MeshBuilder.vTmp.y;
            values[offset + 2] = MeshBuilder.vTmp.z;
        } else if (size > 1) {
            vTmp.set(values[offset], values[offset + 1], 0.0f).mul(transform).nor();
            values[offset] = MeshBuilder.vTmp.x;
            values[offset + 1] = MeshBuilder.vTmp.y;
        } else {
            values[offset] = MeshBuilder.vTmp.set((float)values[offset], (float)0.0f, (float)0.0f).mul((Matrix3)transform).nor().x;
        }
    }

    private final void addVertex(float[] values, int offset) {
        int o = this.vertices.size;
        this.vertices.addAll(values, offset, this.stride);
        short s = this.vindex;
        this.vindex = (short)(s + 1);
        this.lastIndex = s;
        if (this.vertexTransformationEnabled) {
            MeshBuilder.transformPosition(this.vertices.items, o + this.posOffset, this.posSize, this.positionTransform);
            if (this.norOffset >= 0) {
                MeshBuilder.transformNormal(this.vertices.items, o + this.norOffset, 3, this.normalTransform);
            }
            if (this.biNorOffset >= 0) {
                MeshBuilder.transformNormal(this.vertices.items, o + this.biNorOffset, 3, this.normalTransform);
            }
            if (this.tangentOffset >= 0) {
                MeshBuilder.transformNormal(this.vertices.items, o + this.tangentOffset, 3, this.normalTransform);
            }
        }
        float x = this.vertices.items[o + this.posOffset];
        float y = this.posSize > 1 ? this.vertices.items[o + this.posOffset + 1] : 0.0f;
        float z = this.posSize > 2 ? this.vertices.items[o + this.posOffset + 2] : 0.0f;
        this.bounds.ext(x, y, z);
        if (this.hasColor) {
            if (this.colOffset >= 0) {
                int n = o + this.colOffset;
                this.vertices.items[n] = this.vertices.items[n] * this.color.r;
                int n2 = o + this.colOffset + 1;
                this.vertices.items[n2] = this.vertices.items[n2] * this.color.g;
                int n3 = o + this.colOffset + 2;
                this.vertices.items[n3] = this.vertices.items[n3] * this.color.b;
                if (this.colSize > 3) {
                    int n4 = o + this.colOffset + 3;
                    this.vertices.items[n4] = this.vertices.items[n4] * this.color.a;
                }
            } else if (this.cpOffset >= 0) {
                this.vertices.items[o + this.cpOffset] = this.tempC1.set(NumberUtils.floatToIntColor(this.vertices.items[o + this.cpOffset])).mul(this.color).toFloatBits();
            }
        }
        if (this.hasUVTransform && this.uvOffset >= 0) {
            this.vertices.items[o + this.uvOffset] = this.uOffset + this.uScale * this.vertices.items[o + this.uvOffset];
            this.vertices.items[o + this.uvOffset + 1] = this.vOffset + this.vScale * this.vertices.items[o + this.uvOffset + 1];
        }
    }

    @Override
    public short vertex(Vector3 pos, Vector3 nor, Color col, Vector2 uv) {
        if (this.vindex >= Short.MAX_VALUE) {
            throw new GdxRuntimeException("Too many vertices used");
        }
        this.vertex[this.posOffset] = pos.x;
        if (this.posSize > 1) {
            this.vertex[this.posOffset + 1] = pos.y;
        }
        if (this.posSize > 2) {
            this.vertex[this.posOffset + 2] = pos.z;
        }
        if (this.norOffset >= 0) {
            if (nor == null) {
                nor = this.tmpNormal.set(pos).nor();
            }
            this.vertex[this.norOffset] = nor.x;
            this.vertex[this.norOffset + 1] = nor.y;
            this.vertex[this.norOffset + 2] = nor.z;
        }
        if (this.colOffset >= 0) {
            if (col == null) {
                col = Color.WHITE;
            }
            this.vertex[this.colOffset] = col.r;
            this.vertex[this.colOffset + 1] = col.g;
            this.vertex[this.colOffset + 2] = col.b;
            if (this.colSize > 3) {
                this.vertex[this.colOffset + 3] = col.a;
            }
        } else if (this.cpOffset > 0) {
            if (col == null) {
                col = Color.WHITE;
            }
            this.vertex[this.cpOffset] = col.toFloatBits();
        }
        if (uv != null && this.uvOffset >= 0) {
            this.vertex[this.uvOffset] = uv.x;
            this.vertex[this.uvOffset + 1] = uv.y;
        }
        this.addVertex(this.vertex, 0);
        return this.lastIndex;
    }

    @Override
    public short vertex(float ... values) {
        int n = values.length - this.stride;
        for (int i = 0; i <= n; i += this.stride) {
            this.addVertex(values, i);
        }
        return this.lastIndex;
    }

    @Override
    public short vertex(MeshPartBuilder.VertexInfo info) {
        return this.vertex(info.hasPosition ? info.position : null, info.hasNormal ? info.normal : null, info.hasColor ? info.color : null, info.hasUV ? info.uv : null);
    }

    @Override
    public void index(short value) {
        this.indices.add(value);
    }

    @Override
    public void index(short value1, short value2) {
        this.ensureIndices(2);
        this.indices.add(value1);
        this.indices.add(value2);
    }

    @Override
    public void index(short value1, short value2, short value3) {
        this.ensureIndices(3);
        this.indices.add(value1);
        this.indices.add(value2);
        this.indices.add(value3);
    }

    @Override
    public void index(short value1, short value2, short value3, short value4) {
        this.ensureIndices(4);
        this.indices.add(value1);
        this.indices.add(value2);
        this.indices.add(value3);
        this.indices.add(value4);
    }

    @Override
    public void index(short value1, short value2, short value3, short value4, short value5, short value6) {
        this.ensureIndices(6);
        this.indices.add(value1);
        this.indices.add(value2);
        this.indices.add(value3);
        this.indices.add(value4);
        this.indices.add(value5);
        this.indices.add(value6);
    }

    @Override
    public void index(short value1, short value2, short value3, short value4, short value5, short value6, short value7, short value8) {
        this.ensureIndices(8);
        this.indices.add(value1);
        this.indices.add(value2);
        this.indices.add(value3);
        this.indices.add(value4);
        this.indices.add(value5);
        this.indices.add(value6);
        this.indices.add(value7);
        this.indices.add(value8);
    }

    @Override
    public void line(short index1, short index2) {
        if (this.primitiveType != 1) {
            throw new GdxRuntimeException("Incorrect primitive type");
        }
        this.index(index1, index2);
    }

    @Override
    public void line(MeshPartBuilder.VertexInfo p1, MeshPartBuilder.VertexInfo p2) {
        this.ensureVertices(2);
        this.line(this.vertex(p1), this.vertex(p2));
    }

    @Override
    public void line(Vector3 p1, Vector3 p2) {
        this.line(this.vertTmp1.set(p1, null, null, null), this.vertTmp2.set(p2, null, null, null));
    }

    @Override
    public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.line(this.vertTmp1.set(null, null, null, null).setPos(x1, y1, z1), this.vertTmp2.set(null, null, null, null).setPos(x2, y2, z2));
    }

    @Override
    public void line(Vector3 p1, Color c1, Vector3 p2, Color c2) {
        this.line(this.vertTmp1.set(p1, null, c1, null), this.vertTmp2.set(p2, null, c2, null));
    }

    @Override
    public void triangle(short index1, short index2, short index3) {
        if (this.primitiveType == 4 || this.primitiveType == 0) {
            this.index(index1, index2, index3);
        } else if (this.primitiveType == 1) {
            this.index(index1, index2, index2, index3, index3, index1);
        } else {
            throw new GdxRuntimeException("Incorrect primitive type");
        }
    }

    @Override
    public void triangle(MeshPartBuilder.VertexInfo p1, MeshPartBuilder.VertexInfo p2, MeshPartBuilder.VertexInfo p3) {
        this.ensureVertices(3);
        this.triangle(this.vertex(p1), this.vertex(p2), this.vertex(p3));
    }

    @Override
    public void triangle(Vector3 p1, Vector3 p2, Vector3 p3) {
        this.triangle(this.vertTmp1.set(p1, null, null, null), this.vertTmp2.set(p2, null, null, null), this.vertTmp3.set(p3, null, null, null));
    }

    @Override
    public void triangle(Vector3 p1, Color c1, Vector3 p2, Color c2, Vector3 p3, Color c3) {
        this.triangle(this.vertTmp1.set(p1, null, c1, null), this.vertTmp2.set(p2, null, c2, null), this.vertTmp3.set(p3, null, c3, null));
    }

    @Override
    public void rect(short corner00, short corner10, short corner11, short corner01) {
        if (this.primitiveType == 4) {
            this.index(corner00, corner10, corner11, corner11, corner01, corner00);
        } else if (this.primitiveType == 1) {
            this.index(corner00, corner10, corner10, corner11, corner11, corner01, corner01, corner00);
        } else if (this.primitiveType == 0) {
            this.index(corner00, corner10, corner11, corner01);
        } else {
            throw new GdxRuntimeException("Incorrect primitive type");
        }
    }

    @Override
    public void rect(MeshPartBuilder.VertexInfo corner00, MeshPartBuilder.VertexInfo corner10, MeshPartBuilder.VertexInfo corner11, MeshPartBuilder.VertexInfo corner01) {
        this.ensureVertices(4);
        this.rect(this.vertex(corner00), this.vertex(corner10), this.vertex(corner11), this.vertex(corner01));
    }

    @Override
    public void rect(Vector3 corner00, Vector3 corner10, Vector3 corner11, Vector3 corner01, Vector3 normal) {
        this.rect(this.vertTmp1.set(corner00, normal, null, null).setUV(0.0f, 1.0f), this.vertTmp2.set(corner10, normal, null, null).setUV(1.0f, 1.0f), this.vertTmp3.set(corner11, normal, null, null).setUV(1.0f, 0.0f), this.vertTmp4.set(corner01, normal, null, null).setUV(0.0f, 0.0f));
    }

    @Override
    public void rect(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ) {
        this.rect(this.vertTmp1.set(null, null, null, null).setPos(x00, y00, z00).setNor(normalX, normalY, normalZ).setUV(0.0f, 1.0f), this.vertTmp2.set(null, null, null, null).setPos(x10, y10, z10).setNor(normalX, normalY, normalZ).setUV(1.0f, 1.0f), this.vertTmp3.set(null, null, null, null).setPos(x11, y11, z11).setNor(normalX, normalY, normalZ).setUV(1.0f, 0.0f), this.vertTmp4.set(null, null, null, null).setPos(x01, y01, z01).setNor(normalX, normalY, normalZ).setUV(0.0f, 0.0f));
    }

    @Override
    public void addMesh(Mesh mesh) {
        this.addMesh(mesh, 0, mesh.getNumIndices());
    }

    @Override
    public void addMesh(MeshPart meshpart) {
        if (meshpart.primitiveType != this.primitiveType) {
            throw new GdxRuntimeException("Primitive type doesn't match");
        }
        this.addMesh(meshpart.mesh, meshpart.offset, meshpart.size);
    }

    @Override
    public void addMesh(Mesh mesh, int indexOffset, int numIndices) {
        if (!this.attributes.equals(mesh.getVertexAttributes())) {
            throw new GdxRuntimeException("Vertex attributes do not match");
        }
        if (numIndices <= 0) {
            return;
        }
        int numFloats = mesh.getNumVertices() * this.stride;
        tmpVertices.clear();
        tmpVertices.ensureCapacity(numFloats);
        MeshBuilder.tmpVertices.size = numFloats;
        mesh.getVertices(MeshBuilder.tmpVertices.items);
        tmpIndices.clear();
        tmpIndices.ensureCapacity(numIndices);
        MeshBuilder.tmpIndices.size = numIndices;
        mesh.getIndices(indexOffset, numIndices, MeshBuilder.tmpIndices.items, 0);
        this.addMesh(MeshBuilder.tmpVertices.items, MeshBuilder.tmpIndices.items, 0, numIndices);
    }

    @Override
    public void addMesh(float[] vertices, short[] indices, int indexOffset, int numIndices) {
        if (indicesMap == null) {
            indicesMap = new IntIntMap(numIndices);
        } else {
            indicesMap.clear();
            indicesMap.ensureCapacity(numIndices);
        }
        this.ensureIndices(numIndices);
        int numVertices = vertices.length / this.stride;
        this.ensureVertices(numVertices < numIndices ? numVertices : numIndices);
        for (int i = 0; i < numIndices; ++i) {
            short sidx = indices[indexOffset + i];
            int didx = indicesMap.get(sidx, -1);
            if (didx < 0) {
                this.addVertex(vertices, sidx * this.stride);
                short s = this.lastIndex;
                didx = s;
                indicesMap.put(sidx, s);
            }
            this.index((short)didx);
        }
    }

    @Override
    public void addMesh(float[] vertices, short[] indices) {
        short offset = (short)(this.lastIndex + 1);
        int numVertices = vertices.length / this.stride;
        this.ensureVertices(numVertices);
        for (int v = 0; v < vertices.length; v += this.stride) {
            this.addVertex(vertices, v);
        }
        this.ensureIndices(indices.length);
        for (int i = 0; i < indices.length; ++i) {
            this.index((short)(indices[i] + offset));
        }
    }

    @Override
    @Deprecated
    public void patch(MeshPartBuilder.VertexInfo corner00, MeshPartBuilder.VertexInfo corner10, MeshPartBuilder.VertexInfo corner11, MeshPartBuilder.VertexInfo corner01, int divisionsU, int divisionsV) {
        PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, divisionsU, divisionsV);
    }

    @Override
    @Deprecated
    public void patch(Vector3 corner00, Vector3 corner10, Vector3 corner11, Vector3 corner01, Vector3 normal, int divisionsU, int divisionsV) {
        PatchShapeBuilder.build(this, corner00, corner10, corner11, corner01, normal, divisionsU, divisionsV);
    }

    @Override
    @Deprecated
    public void patch(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ, int divisionsU, int divisionsV) {
        PatchShapeBuilder.build(this, x00, y00, z00, x10, y10, z10, x11, y11, z11, x01, y01, z01, normalX, normalY, normalZ, divisionsU, divisionsV);
    }

    @Override
    @Deprecated
    public void box(MeshPartBuilder.VertexInfo corner000, MeshPartBuilder.VertexInfo corner010, MeshPartBuilder.VertexInfo corner100, MeshPartBuilder.VertexInfo corner110, MeshPartBuilder.VertexInfo corner001, MeshPartBuilder.VertexInfo corner011, MeshPartBuilder.VertexInfo corner101, MeshPartBuilder.VertexInfo corner111) {
        BoxShapeBuilder.build((MeshPartBuilder)this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111);
    }

    @Override
    @Deprecated
    public void box(Vector3 corner000, Vector3 corner010, Vector3 corner100, Vector3 corner110, Vector3 corner001, Vector3 corner011, Vector3 corner101, Vector3 corner111) {
        BoxShapeBuilder.build((MeshPartBuilder)this, corner000, corner010, corner100, corner110, corner001, corner011, corner101, corner111);
    }

    @Override
    @Deprecated
    public void box(Matrix4 transform) {
        BoxShapeBuilder.build((MeshPartBuilder)this, transform);
    }

    @Override
    @Deprecated
    public void box(float width, float height, float depth) {
        BoxShapeBuilder.build(this, width, height, depth);
    }

    @Override
    @Deprecated
    public void box(float x, float y, float z, float width, float height, float depth) {
        BoxShapeBuilder.build(this, x, y, z, width, height, depth);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build(this, radius, divisions, center, normal);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, radius, divisions, center, normal, tangent, binormal);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, Vector3 center, Vector3 normal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, radius, divisions, center, normal, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal, float angleFrom, float angleTo) {
        this.circle(radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z, binormal.x, binormal.y, binormal.z, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(this, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build(this, width, height, divisions, center, normal);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, divisions, center, normal, tangent, binormal);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, Vector3 center, Vector3 normal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, divisions, center, normal, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, Vector3 center, Vector3 normal, Vector3 tangent, Vector3 binormal, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, divisions, center, normal, tangent, binormal, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(this, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, Vector3 center, Vector3 normal) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, innerWidth, innerHeight, divisions, center, normal);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ) {
        EllipseShapeBuilder.build((MeshPartBuilder)this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo) {
        EllipseShapeBuilder.build(this, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, tangentX, tangentY, tangentZ, binormalX, binormalY, binormalZ, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void cylinder(float width, float height, float depth, int divisions) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions);
    }

    @Override
    @Deprecated
    public void cylinder(float width, float height, float depth, int divisions, float angleFrom, float angleTo) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void cylinder(float width, float height, float depth, int divisions, float angleFrom, float angleTo, boolean close) {
        CylinderShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo, close);
    }

    @Override
    @Deprecated
    public void cone(float width, float height, float depth, int divisions) {
        this.cone(width, height, depth, divisions, 0.0f, 360.0f);
    }

    @Override
    @Deprecated
    public void cone(float width, float height, float depth, int divisions, float angleFrom, float angleTo) {
        ConeShapeBuilder.build(this, width, height, depth, divisions, angleFrom, angleTo);
    }

    @Override
    @Deprecated
    public void sphere(float width, float height, float depth, int divisionsU, int divisionsV) {
        SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV);
    }

    @Override
    @Deprecated
    public void sphere(Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV) {
        SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV);
    }

    @Override
    @Deprecated
    public void sphere(float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        SphereShapeBuilder.build(this, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo);
    }

    @Override
    @Deprecated
    public void sphere(Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo) {
        SphereShapeBuilder.build(this, transform, width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo);
    }

    @Override
    @Deprecated
    public void capsule(float radius, float height, int divisions) {
        CapsuleShapeBuilder.build(this, radius, height, divisions);
    }

    @Override
    @Deprecated
    public void arrow(float x1, float y1, float z1, float x2, float y2, float z2, float capLength, float stemThickness, int divisions) {
        ArrowShapeBuilder.build(this, x1, y1, z1, x2, y2, z2, capLength, stemThickness, divisions);
    }
}

