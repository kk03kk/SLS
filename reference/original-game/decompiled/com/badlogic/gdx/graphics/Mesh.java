/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.IndexArray;
import com.badlogic.gdx.graphics.glutils.IndexBufferObject;
import com.badlogic.gdx.graphics.glutils.IndexBufferObjectSubData;
import com.badlogic.gdx.graphics.glutils.IndexData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexArray;
import com.badlogic.gdx.graphics.glutils.VertexBufferObject;
import com.badlogic.gdx.graphics.glutils.VertexBufferObjectSubData;
import com.badlogic.gdx.graphics.glutils.VertexBufferObjectWithVAO;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class Mesh
implements Disposable {
    static final Map<Application, Array<Mesh>> meshes = new HashMap<Application, Array<Mesh>>();
    final VertexData vertices;
    final IndexData indices;
    boolean autoBind = true;
    final boolean isVertexArray;
    private final Vector3 tmpV = new Vector3();

    protected Mesh(VertexData vertices, IndexData indices, boolean isVertexArray) {
        this.vertices = vertices;
        this.indices = indices;
        this.isVertexArray = isVertexArray;
        Mesh.addManagedMesh(Gdx.app, this);
    }

    public Mesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute ... attributes) {
        this.vertices = this.makeVertexBuffer(isStatic, maxVertices, new VertexAttributes(attributes));
        this.indices = new IndexBufferObject(isStatic, maxIndices);
        this.isVertexArray = false;
        Mesh.addManagedMesh(Gdx.app, this);
    }

    public Mesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttributes attributes) {
        this.vertices = this.makeVertexBuffer(isStatic, maxVertices, attributes);
        this.indices = new IndexBufferObject(isStatic, maxIndices);
        this.isVertexArray = false;
        Mesh.addManagedMesh(Gdx.app, this);
    }

    public Mesh(boolean staticVertices, boolean staticIndices, int maxVertices, int maxIndices, VertexAttributes attributes) {
        this.vertices = this.makeVertexBuffer(staticVertices, maxVertices, attributes);
        this.indices = new IndexBufferObject(staticIndices, maxIndices);
        this.isVertexArray = false;
        Mesh.addManagedMesh(Gdx.app, this);
    }

    private VertexData makeVertexBuffer(boolean isStatic, int maxVertices, VertexAttributes vertexAttributes) {
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectWithVAO(isStatic, maxVertices, vertexAttributes);
        }
        return new VertexBufferObject(isStatic, maxVertices, vertexAttributes);
    }

    public Mesh(VertexDataType type, boolean isStatic, int maxVertices, int maxIndices, VertexAttribute ... attributes) {
        switch (type) {
            case VertexBufferObject: {
                this.vertices = new VertexBufferObject(isStatic, maxVertices, attributes);
                this.indices = new IndexBufferObject(isStatic, maxIndices);
                this.isVertexArray = false;
                break;
            }
            case VertexBufferObjectSubData: {
                this.vertices = new VertexBufferObjectSubData(isStatic, maxVertices, attributes);
                this.indices = new IndexBufferObjectSubData(isStatic, maxIndices);
                this.isVertexArray = false;
                break;
            }
            case VertexBufferObjectWithVAO: {
                this.vertices = new VertexBufferObjectWithVAO(isStatic, maxVertices, attributes);
                this.indices = new IndexBufferObjectSubData(isStatic, maxIndices);
                this.isVertexArray = false;
                break;
            }
            default: {
                this.vertices = new VertexArray(maxVertices, attributes);
                this.indices = new IndexArray(maxIndices);
                this.isVertexArray = true;
            }
        }
        Mesh.addManagedMesh(Gdx.app, this);
    }

    public Mesh setVertices(float[] vertices) {
        this.vertices.setVertices(vertices, 0, vertices.length);
        return this;
    }

    public Mesh setVertices(float[] vertices, int offset, int count) {
        this.vertices.setVertices(vertices, offset, count);
        return this;
    }

    public Mesh updateVertices(int targetOffset, float[] source) {
        return this.updateVertices(targetOffset, source, 0, source.length);
    }

    public Mesh updateVertices(int targetOffset, float[] source, int sourceOffset, int count) {
        this.vertices.updateVertices(targetOffset, source, sourceOffset, count);
        return this;
    }

    public float[] getVertices(float[] vertices) {
        return this.getVertices(0, -1, vertices);
    }

    public float[] getVertices(int srcOffset, float[] vertices) {
        return this.getVertices(srcOffset, -1, vertices);
    }

    public float[] getVertices(int srcOffset, int count, float[] vertices) {
        return this.getVertices(srcOffset, count, vertices, 0);
    }

    public float[] getVertices(int srcOffset, int count, float[] vertices, int destOffset) {
        int max = this.getNumVertices() * this.getVertexSize() / 4;
        if (count == -1 && (count = max - srcOffset) > vertices.length - destOffset) {
            count = vertices.length - destOffset;
        }
        if (srcOffset < 0 || count <= 0 || srcOffset + count > max || destOffset < 0 || destOffset >= vertices.length) {
            throw new IndexOutOfBoundsException();
        }
        if (vertices.length - destOffset < count) {
            throw new IllegalArgumentException("not enough room in vertices array, has " + vertices.length + " floats, needs " + count);
        }
        int pos = this.getVerticesBuffer().position();
        this.getVerticesBuffer().position(srcOffset);
        this.getVerticesBuffer().get(vertices, destOffset, count);
        this.getVerticesBuffer().position(pos);
        return vertices;
    }

    public Mesh setIndices(short[] indices) {
        this.indices.setIndices(indices, 0, indices.length);
        return this;
    }

    public Mesh setIndices(short[] indices, int offset, int count) {
        this.indices.setIndices(indices, offset, count);
        return this;
    }

    public void getIndices(short[] indices) {
        this.getIndices(indices, 0);
    }

    public void getIndices(short[] indices, int destOffset) {
        this.getIndices(0, indices, destOffset);
    }

    public void getIndices(int srcOffset, short[] indices, int destOffset) {
        this.getIndices(srcOffset, -1, indices, destOffset);
    }

    public void getIndices(int srcOffset, int count, short[] indices, int destOffset) {
        int max = this.getNumIndices();
        if (count < 0) {
            count = max - srcOffset;
        }
        if (srcOffset < 0 || srcOffset >= max || srcOffset + count > max) {
            throw new IllegalArgumentException("Invalid range specified, offset: " + srcOffset + ", count: " + count + ", max: " + max);
        }
        if (indices.length - destOffset < count) {
            throw new IllegalArgumentException("not enough room in indices array, has " + indices.length + " shorts, needs " + count);
        }
        int pos = this.getIndicesBuffer().position();
        this.getIndicesBuffer().position(srcOffset);
        this.getIndicesBuffer().get(indices, destOffset, count);
        this.getIndicesBuffer().position(pos);
    }

    public int getNumIndices() {
        return this.indices.getNumIndices();
    }

    public int getNumVertices() {
        return this.vertices.getNumVertices();
    }

    public int getMaxVertices() {
        return this.vertices.getNumMaxVertices();
    }

    public int getMaxIndices() {
        return this.indices.getNumMaxIndices();
    }

    public int getVertexSize() {
        return this.vertices.getAttributes().vertexSize;
    }

    public void setAutoBind(boolean autoBind) {
        this.autoBind = autoBind;
    }

    public void bind(ShaderProgram shader) {
        this.bind(shader, null);
    }

    public void bind(ShaderProgram shader, int[] locations) {
        this.vertices.bind(shader, locations);
        if (this.indices.getNumIndices() > 0) {
            this.indices.bind();
        }
    }

    public void unbind(ShaderProgram shader) {
        this.unbind(shader, null);
    }

    public void unbind(ShaderProgram shader, int[] locations) {
        this.vertices.unbind(shader, locations);
        if (this.indices.getNumIndices() > 0) {
            this.indices.unbind();
        }
    }

    public void render(ShaderProgram shader, int primitiveType) {
        this.render(shader, primitiveType, 0, this.indices.getNumMaxIndices() > 0 ? this.getNumIndices() : this.getNumVertices(), this.autoBind);
    }

    public void render(ShaderProgram shader, int primitiveType, int offset, int count) {
        this.render(shader, primitiveType, offset, count, this.autoBind);
    }

    public void render(ShaderProgram shader, int primitiveType, int offset, int count, boolean autoBind) {
        if (count == 0) {
            return;
        }
        if (autoBind) {
            this.bind(shader);
        }
        if (this.isVertexArray) {
            if (this.indices.getNumIndices() > 0) {
                ShortBuffer buffer = this.indices.getBuffer();
                int oldPosition = buffer.position();
                int oldLimit = buffer.limit();
                buffer.position(offset);
                buffer.limit(offset + count);
                Gdx.gl20.glDrawElements(primitiveType, count, 5123, buffer);
                buffer.position(oldPosition);
                buffer.limit(oldLimit);
            } else {
                Gdx.gl20.glDrawArrays(primitiveType, offset, count);
            }
        } else if (this.indices.getNumIndices() > 0) {
            Gdx.gl20.glDrawElements(primitiveType, count, 5123, offset * 2);
        } else {
            Gdx.gl20.glDrawArrays(primitiveType, offset, count);
        }
        if (autoBind) {
            this.unbind(shader);
        }
    }

    @Override
    public void dispose() {
        if (meshes.get(Gdx.app) != null) {
            meshes.get(Gdx.app).removeValue(this, true);
        }
        this.vertices.dispose();
        this.indices.dispose();
    }

    public VertexAttribute getVertexAttribute(int usage) {
        VertexAttributes attributes = this.vertices.getAttributes();
        int len = attributes.size();
        for (int i = 0; i < len; ++i) {
            if (attributes.get((int)i).usage != usage) continue;
            return attributes.get(i);
        }
        return null;
    }

    public VertexAttributes getVertexAttributes() {
        return this.vertices.getAttributes();
    }

    public FloatBuffer getVerticesBuffer() {
        return this.vertices.getBuffer();
    }

    public BoundingBox calculateBoundingBox() {
        BoundingBox bbox = new BoundingBox();
        this.calculateBoundingBox(bbox);
        return bbox;
    }

    public void calculateBoundingBox(BoundingBox bbox) {
        int numVertices = this.getNumVertices();
        if (numVertices == 0) {
            throw new GdxRuntimeException("No vertices defined");
        }
        FloatBuffer verts = this.vertices.getBuffer();
        bbox.inf();
        VertexAttribute posAttrib = this.getVertexAttribute(1);
        int offset = posAttrib.offset / 4;
        int vertexSize = this.vertices.getAttributes().vertexSize / 4;
        int idx = offset;
        switch (posAttrib.numComponents) {
            case 1: {
                for (int i = 0; i < numVertices; ++i) {
                    bbox.ext(verts.get(idx), 0.0f, 0.0f);
                    idx += vertexSize;
                }
                break;
            }
            case 2: {
                for (int i = 0; i < numVertices; ++i) {
                    bbox.ext(verts.get(idx), verts.get(idx + 1), 0.0f);
                    idx += vertexSize;
                }
                break;
            }
            case 3: {
                for (int i = 0; i < numVertices; ++i) {
                    bbox.ext(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                    idx += vertexSize;
                }
                break;
            }
        }
    }

    public BoundingBox calculateBoundingBox(BoundingBox out, int offset, int count) {
        return this.extendBoundingBox(out.inf(), offset, count);
    }

    public BoundingBox calculateBoundingBox(BoundingBox out, int offset, int count, Matrix4 transform) {
        return this.extendBoundingBox(out.inf(), offset, count, transform);
    }

    public BoundingBox extendBoundingBox(BoundingBox out, int offset, int count) {
        return this.extendBoundingBox(out, offset, count, null);
    }

    public BoundingBox extendBoundingBox(BoundingBox out, int offset, int count, Matrix4 transform) {
        int max;
        int numIndices = this.getNumIndices();
        int numVertices = this.getNumVertices();
        int n = max = numIndices == 0 ? numVertices : numIndices;
        if (offset < 0 || count < 1 || offset + count > max) {
            throw new GdxRuntimeException("Invalid part specified ( offset=" + offset + ", count=" + count + ", max=" + max + " )");
        }
        FloatBuffer verts = this.vertices.getBuffer();
        ShortBuffer index = this.indices.getBuffer();
        VertexAttribute posAttrib = this.getVertexAttribute(1);
        int posoff = posAttrib.offset / 4;
        int vertexSize = this.vertices.getAttributes().vertexSize / 4;
        int end = offset + count;
        switch (posAttrib.numComponents) {
            case 1: {
                if (numIndices > 0) {
                    for (int i = offset; i < end; ++i) {
                        int idx = index.get(i) * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), 0.0f, 0.0f);
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                } else {
                    for (int i = offset; i < end; ++i) {
                        int idx = i * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), 0.0f, 0.0f);
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                }
                break;
            }
            case 2: {
                if (numIndices > 0) {
                    for (int i = offset; i < end; ++i) {
                        int idx = index.get(i) * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), verts.get(idx + 1), 0.0f);
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                } else {
                    for (int i = offset; i < end; ++i) {
                        int idx = i * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), verts.get(idx + 1), 0.0f);
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                }
                break;
            }
            case 3: {
                if (numIndices > 0) {
                    for (int i = offset; i < end; ++i) {
                        int idx = index.get(i) * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                } else {
                    for (int i = offset; i < end; ++i) {
                        int idx = i * vertexSize + posoff;
                        this.tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                        if (transform != null) {
                            this.tmpV.mul(transform);
                        }
                        out.ext(this.tmpV);
                    }
                }
                break;
            }
        }
        return out;
    }

    public float calculateRadiusSquared(float centerX, float centerY, float centerZ, int offset, int count, Matrix4 transform) {
        int numIndices = this.getNumIndices();
        if (offset < 0 || count < 1 || offset + count > numIndices) {
            throw new GdxRuntimeException("Not enough indices");
        }
        FloatBuffer verts = this.vertices.getBuffer();
        ShortBuffer index = this.indices.getBuffer();
        VertexAttribute posAttrib = this.getVertexAttribute(1);
        int posoff = posAttrib.offset / 4;
        int vertexSize = this.vertices.getAttributes().vertexSize / 4;
        int end = offset + count;
        float result = 0.0f;
        switch (posAttrib.numComponents) {
            case 1: {
                for (int i = offset; i < end; ++i) {
                    float r;
                    int idx = index.get(i) * vertexSize + posoff;
                    this.tmpV.set(verts.get(idx), 0.0f, 0.0f);
                    if (transform != null) {
                        this.tmpV.mul(transform);
                    }
                    if (!((r = this.tmpV.sub(centerX, centerY, centerZ).len2()) > result)) continue;
                    result = r;
                }
                break;
            }
            case 2: {
                for (int i = offset; i < end; ++i) {
                    float r;
                    int idx = index.get(i) * vertexSize + posoff;
                    this.tmpV.set(verts.get(idx), verts.get(idx + 1), 0.0f);
                    if (transform != null) {
                        this.tmpV.mul(transform);
                    }
                    if (!((r = this.tmpV.sub(centerX, centerY, centerZ).len2()) > result)) continue;
                    result = r;
                }
                break;
            }
            case 3: {
                for (int i = offset; i < end; ++i) {
                    float r;
                    int idx = index.get(i) * vertexSize + posoff;
                    this.tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                    if (transform != null) {
                        this.tmpV.mul(transform);
                    }
                    if (!((r = this.tmpV.sub(centerX, centerY, centerZ).len2()) > result)) continue;
                    result = r;
                }
                break;
            }
        }
        return result;
    }

    public float calculateRadius(float centerX, float centerY, float centerZ, int offset, int count, Matrix4 transform) {
        return (float)Math.sqrt(this.calculateRadiusSquared(centerX, centerY, centerZ, offset, count, transform));
    }

    public float calculateRadius(Vector3 center, int offset, int count, Matrix4 transform) {
        return this.calculateRadius(center.x, center.y, center.z, offset, count, transform);
    }

    public float calculateRadius(float centerX, float centerY, float centerZ, int offset, int count) {
        return this.calculateRadius(centerX, centerY, centerZ, offset, count, null);
    }

    public float calculateRadius(Vector3 center, int offset, int count) {
        return this.calculateRadius(center.x, center.y, center.z, offset, count, null);
    }

    public float calculateRadius(float centerX, float centerY, float centerZ) {
        return this.calculateRadius(centerX, centerY, centerZ, 0, this.getNumIndices(), null);
    }

    public float calculateRadius(Vector3 center) {
        return this.calculateRadius(center.x, center.y, center.z, 0, this.getNumIndices(), null);
    }

    public ShortBuffer getIndicesBuffer() {
        return this.indices.getBuffer();
    }

    private static void addManagedMesh(Application app, Mesh mesh) {
        Array<Mesh> managedResources = meshes.get(app);
        if (managedResources == null) {
            managedResources = new Array();
        }
        managedResources.add(mesh);
        meshes.put(app, managedResources);
    }

    public static void invalidateAllMeshes(Application app) {
        Array<Mesh> meshesArray = meshes.get(app);
        if (meshesArray == null) {
            return;
        }
        for (int i = 0; i < meshesArray.size; ++i) {
            meshesArray.get((int)i).vertices.invalidate();
            meshesArray.get((int)i).indices.invalidate();
        }
    }

    public static void clearAllMeshes(Application app) {
        meshes.remove(app);
    }

    public static String getManagedStatus() {
        StringBuilder builder = new StringBuilder();
        boolean i = false;
        builder.append("Managed meshes/app: { ");
        for (Application app : meshes.keySet()) {
            builder.append(Mesh.meshes.get((Object)app).size);
            builder.append(" ");
        }
        builder.append("}");
        return builder.toString();
    }

    public void scale(float scaleX, float scaleY, float scaleZ) {
        VertexAttribute posAttr = this.getVertexAttribute(1);
        int offset = posAttr.offset / 4;
        int numComponents = posAttr.numComponents;
        int numVertices = this.getNumVertices();
        int vertexSize = this.getVertexSize() / 4;
        float[] vertices = new float[numVertices * vertexSize];
        this.getVertices(vertices);
        int idx = offset;
        switch (numComponents) {
            case 1: {
                for (int i = 0; i < numVertices; ++i) {
                    int n = idx;
                    vertices[n] = vertices[n] * scaleX;
                    idx += vertexSize;
                }
                break;
            }
            case 2: {
                for (int i = 0; i < numVertices; ++i) {
                    int n = idx;
                    vertices[n] = vertices[n] * scaleX;
                    int n2 = idx + 1;
                    vertices[n2] = vertices[n2] * scaleY;
                    idx += vertexSize;
                }
                break;
            }
            case 3: {
                for (int i = 0; i < numVertices; ++i) {
                    int n = idx;
                    vertices[n] = vertices[n] * scaleX;
                    int n3 = idx + 1;
                    vertices[n3] = vertices[n3] * scaleY;
                    int n4 = idx + 2;
                    vertices[n4] = vertices[n4] * scaleZ;
                    idx += vertexSize;
                }
                break;
            }
        }
        this.setVertices(vertices);
    }

    public void transform(Matrix4 matrix) {
        this.transform(matrix, 0, this.getNumVertices());
    }

    public void transform(Matrix4 matrix, int start, int count) {
        VertexAttribute posAttr = this.getVertexAttribute(1);
        int posOffset = posAttr.offset / 4;
        int stride = this.getVertexSize() / 4;
        int numComponents = posAttr.numComponents;
        int numVertices = this.getNumVertices();
        float[] vertices = new float[count * stride];
        this.getVertices(start * stride, count * stride, vertices);
        Mesh.transform(matrix, vertices, stride, posOffset, numComponents, 0, count);
        this.updateVertices(start * stride, vertices);
    }

    public static void transform(Matrix4 matrix, float[] vertices, int vertexSize, int offset, int dimensions, int start, int count) {
        if (offset < 0 || dimensions < 1 || offset + dimensions > vertexSize) {
            throw new IndexOutOfBoundsException();
        }
        if (start < 0 || count < 1 || (start + count) * vertexSize > vertices.length) {
            throw new IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize + ", length = " + vertices.length);
        }
        Vector3 tmp = new Vector3();
        int idx = offset + start * vertexSize;
        switch (dimensions) {
            case 1: {
                for (int i = 0; i < count; ++i) {
                    tmp.set(vertices[idx], 0.0f, 0.0f).mul(matrix);
                    vertices[idx] = tmp.x;
                    idx += vertexSize;
                }
                break;
            }
            case 2: {
                for (int i = 0; i < count; ++i) {
                    tmp.set(vertices[idx], vertices[idx + 1], 0.0f).mul(matrix);
                    vertices[idx] = tmp.x;
                    vertices[idx + 1] = tmp.y;
                    idx += vertexSize;
                }
                break;
            }
            case 3: {
                for (int i = 0; i < count; ++i) {
                    tmp.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]).mul(matrix);
                    vertices[idx] = tmp.x;
                    vertices[idx + 1] = tmp.y;
                    vertices[idx + 2] = tmp.z;
                    idx += vertexSize;
                }
                break;
            }
        }
    }

    public void transformUV(Matrix3 matrix) {
        this.transformUV(matrix, 0, this.getNumVertices());
    }

    protected void transformUV(Matrix3 matrix, int start, int count) {
        VertexAttribute posAttr = this.getVertexAttribute(16);
        int offset = posAttr.offset / 4;
        int vertexSize = this.getVertexSize() / 4;
        int numVertices = this.getNumVertices();
        float[] vertices = new float[numVertices * vertexSize];
        this.getVertices(0, vertices.length, vertices);
        Mesh.transformUV(matrix, vertices, vertexSize, offset, start, count);
        this.setVertices(vertices, 0, vertices.length);
    }

    public static void transformUV(Matrix3 matrix, float[] vertices, int vertexSize, int offset, int start, int count) {
        if (start < 0 || count < 1 || (start + count) * vertexSize > vertices.length) {
            throw new IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize + ", length = " + vertices.length);
        }
        Vector2 tmp = new Vector2();
        int idx = offset + start * vertexSize;
        for (int i = 0; i < count; ++i) {
            tmp.set(vertices[idx], vertices[idx + 1]).mul(matrix);
            vertices[idx] = tmp.x;
            vertices[idx + 1] = tmp.y;
            idx += vertexSize;
        }
    }

    public Mesh copy(boolean isStatic, boolean removeDuplicates, int[] usage) {
        int i;
        int vertexSize = this.getVertexSize() / 4;
        int numVertices = this.getNumVertices();
        float[] vertices = new float[numVertices * vertexSize];
        this.getVertices(0, vertices.length, vertices);
        short[] checks = null;
        VertexAttribute[] attrs = null;
        int newVertexSize = 0;
        if (usage != null) {
            int size = 0;
            int as = 0;
            for (int i2 = 0; i2 < usage.length; ++i2) {
                if (this.getVertexAttribute(usage[i2]) == null) continue;
                size += this.getVertexAttribute((int)usage[i2]).numComponents;
                ++as;
            }
            if (size > 0) {
                attrs = new VertexAttribute[as];
                checks = new short[size];
                int idx = -1;
                int ai = -1;
                for (i = 0; i < usage.length; ++i) {
                    VertexAttribute a = this.getVertexAttribute(usage[i]);
                    if (a == null) continue;
                    for (int j = 0; j < a.numComponents; ++j) {
                        checks[++idx] = (short)(a.offset + j);
                    }
                    attrs[++ai] = new VertexAttribute(a.usage, a.numComponents, a.alias);
                    newVertexSize += a.numComponents;
                }
            }
        }
        if (checks == null) {
            checks = new short[vertexSize];
            for (int i3 = 0; i3 < vertexSize; i3 = (int)((short)(i3 + 1))) {
                checks[i3] = i3;
            }
            newVertexSize = vertexSize;
        }
        int numIndices = this.getNumIndices();
        short[] indices = null;
        if (numIndices > 0) {
            indices = new short[numIndices];
            this.getIndices(indices);
            if (removeDuplicates || newVertexSize != vertexSize) {
                float[] tmp = new float[vertices.length];
                int size = 0;
                for (i = 0; i < numIndices; ++i) {
                    int idx1 = indices[i] * vertexSize;
                    int newIndex = -1;
                    if (removeDuplicates) {
                        for (int j = 0; j < size && newIndex < 0; j = (int)((short)(j + 1))) {
                            int idx2 = j * newVertexSize;
                            boolean found = true;
                            for (int k = 0; k < checks.length && found; ++k) {
                                if (tmp[idx2 + k] == vertices[idx1 + checks[k]]) continue;
                                found = false;
                            }
                            if (!found) continue;
                            newIndex = j;
                        }
                    }
                    if (newIndex > 0) {
                        indices[i] = newIndex;
                        continue;
                    }
                    int idx = size * newVertexSize;
                    for (int j = 0; j < checks.length; ++j) {
                        tmp[idx + j] = vertices[idx1 + checks[j]];
                    }
                    indices[i] = (short)size;
                    ++size;
                }
                vertices = tmp;
                numVertices = size;
            }
        }
        Mesh result = attrs == null ? new Mesh(isStatic, numVertices, indices == null ? 0 : indices.length, this.getVertexAttributes()) : new Mesh(isStatic, numVertices, indices == null ? 0 : indices.length, attrs);
        result.setVertices(vertices, 0, numVertices * newVertexSize);
        result.setIndices(indices);
        return result;
    }

    public Mesh copy(boolean isStatic) {
        return this.copy(isStatic, false, null);
    }

    public static enum VertexDataType {
        VertexArray,
        VertexBufferObject,
        VertexBufferObjectSubData,
        VertexBufferObjectWithVAO;

    }
}

