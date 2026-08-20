/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.NumberUtils;

public class SpriteBatch
implements Batch {
    @Deprecated
    public static Mesh.VertexDataType defaultVertexDataType = Mesh.VertexDataType.VertexArray;
    private Mesh mesh;
    final float[] vertices;
    int idx = 0;
    Texture lastTexture = null;
    float invTexWidth = 0.0f;
    float invTexHeight = 0.0f;
    boolean drawing = false;
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();
    private boolean blendingDisabled = false;
    private int blendSrcFunc = 770;
    private int blendDstFunc = 771;
    private final ShaderProgram shader;
    private ShaderProgram customShader = null;
    private boolean ownsShader;
    float color = Color.WHITE.toFloatBits();
    private Color tempColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public int renderCalls = 0;
    public int totalRenderCalls = 0;
    public int maxSpritesInBatch = 0;

    public SpriteBatch() {
        this(1000, null);
    }

    public SpriteBatch(int size) {
        this(size, null);
    }

    public SpriteBatch(int size, ShaderProgram defaultShader) {
        if (size > 8191) {
            throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);
        }
        Mesh.VertexDataType vertexDataType = Gdx.gl30 != null ? Mesh.VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;
        this.mesh = new Mesh(vertexDataType, false, size * 4, size * 6, new VertexAttribute(1, 2, "a_position"), new VertexAttribute(4, 4, "a_color"), new VertexAttribute(16, 2, "a_texCoord0"));
        this.projectionMatrix.setToOrtho2D(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.vertices = new float[size * 20];
        int len = size * 6;
        short[] indices = new short[len];
        short j = 0;
        int i = 0;
        while (i < len) {
            indices[i] = j;
            indices[i + 1] = (short)(j + 1);
            indices[i + 2] = (short)(j + 2);
            indices[i + 3] = (short)(j + 2);
            indices[i + 4] = (short)(j + 3);
            indices[i + 5] = j;
            i += 6;
            j = (short)(j + 4);
        }
        this.mesh.setIndices(indices);
        if (defaultShader == null) {
            this.shader = SpriteBatch.createDefaultShader();
            this.ownsShader = true;
        } else {
            this.shader = defaultShader;
        }
    }

    public static ShaderProgram createDefaultShader() {
        String vertexShader = "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\nuniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_texCoords;\n\nvoid main()\n{\n   v_color = a_color;\n   v_color.a = v_color.a * (255.0/254.0);\n   v_texCoords = a_texCoord0;\n   gl_Position =  u_projTrans * a_position;\n}\n";
        String fragmentShader = "#ifdef GL_ES\n#define LOWP lowp\nprecision mediump float;\n#else\n#define LOWP \n#endif\nvarying LOWP vec4 v_color;\nvarying vec2 v_texCoords;\nuniform sampler2D u_texture;\nvoid main()\n{\n  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n}";
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        }
        return shader;
    }

    @Override
    public void begin() {
        if (this.drawing) {
            throw new IllegalStateException("SpriteBatch.end must be called before begin.");
        }
        this.renderCalls = 0;
        Gdx.gl.glDepthMask(false);
        if (this.customShader != null) {
            this.customShader.begin();
        } else {
            this.shader.begin();
        }
        this.setupMatrices();
        this.drawing = true;
    }

    @Override
    public void end() {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before end.");
        }
        if (this.idx > 0) {
            this.flush();
        }
        this.lastTexture = null;
        this.drawing = false;
        GL20 gl = Gdx.gl;
        gl.glDepthMask(true);
        if (this.isBlendingEnabled()) {
            gl.glDisable(3042);
        }
        if (this.customShader != null) {
            this.customShader.end();
        } else {
            this.shader.end();
        }
    }

    @Override
    public void setColor(Color tint) {
        this.color = tint.toFloatBits();
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        int intBits = (int)(255.0f * a) << 24 | (int)(255.0f * b) << 16 | (int)(255.0f * g) << 8 | (int)(255.0f * r);
        this.color = NumberUtils.intToFloatColor(intBits);
    }

    @Override
    public void setColor(float color) {
        this.color = color;
    }

    @Override
    public Color getColor() {
        int intBits = NumberUtils.floatToIntColor(this.color);
        Color color = this.tempColor;
        color.r = (float)(intBits & 0xFF) / 255.0f;
        color.g = (float)(intBits >>> 8 & 0xFF) / 255.0f;
        color.b = (float)(intBits >>> 16 & 0xFF) / 255.0f;
        color.a = (float)(intBits >>> 24 & 0xFF) / 255.0f;
        return color;
    }

    @Override
    public float getPackedColor() {
        return this.color;
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        float tmp;
        float y4;
        float x4;
        float y3;
        float x3;
        float y2;
        float x2;
        float y1;
        float x1;
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }
        float p1x = fx;
        float p1y = fy;
        float p2x = fx;
        float p2y = fy2;
        float p3x = fx2;
        float p3y = fy2;
        float p4x = fx2;
        float p4y = fy;
        if (rotation != 0.0f) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }
        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;
        float u = (float)srcX * this.invTexWidth;
        float v = (float)(srcY + srcHeight) * this.invTexHeight;
        float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
        float v2 = (float)srcY * this.invTexHeight;
        if (flipX) {
            tmp = u;
            u = u2;
            u2 = tmp;
        }
        if (flipY) {
            tmp = v;
            v = v2;
            v2 = tmp;
        }
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x1;
        vertices[idx + 1] = y1;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x2;
        vertices[idx + 6] = y2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = x3;
        vertices[idx + 11] = y3;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = x4;
        vertices[idx + 16] = y4;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        float tmp;
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float u = (float)srcX * this.invTexWidth;
        float v = (float)(srcY + srcHeight) * this.invTexHeight;
        float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
        float v2 = (float)srcY * this.invTexHeight;
        float fx2 = x + width;
        float fy2 = y + height;
        if (flipX) {
            tmp = u;
            u = u2;
            u2 = tmp;
        }
        if (flipY) {
            tmp = v;
            v = v2;
            v2 = tmp;
        }
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float u = (float)srcX * this.invTexWidth;
        float v = (float)(srcY + srcHeight) * this.invTexHeight;
        float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
        float v2 = (float)srcY * this.invTexHeight;
        float fx2 = x + (float)srcWidth;
        float fy2 = y + (float)srcHeight;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float fx2 = x + width;
        float fy2 = y + height;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        this.draw(texture, x, y, (float)texture.getWidth(), (float)texture.getHeight());
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float fx2 = x + width;
        float fy2 = y + height;
        float u = 0.0f;
        float v = 1.0f;
        float u2 = 1.0f;
        float v2 = 0.0f;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = 0.0f;
        vertices[idx + 4] = 1.0f;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = 0.0f;
        vertices[idx + 9] = 0.0f;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = 1.0f;
        vertices[idx + 14] = 0.0f;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = 1.0f;
        vertices[idx + 19] = 1.0f;
        this.idx = idx + 20;
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        int verticesLength;
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        int remainingVertices = verticesLength = this.vertices.length;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if ((remainingVertices -= this.idx) == 0) {
            this.flush();
            remainingVertices = verticesLength;
        }
        int copyCount = Math.min(remainingVertices, count);
        System.arraycopy(spriteVertices, offset, this.vertices, this.idx, copyCount);
        this.idx += copyCount;
        count -= copyCount;
        while (count > 0) {
            this.flush();
            copyCount = Math.min(verticesLength, count);
            System.arraycopy(spriteVertices, offset += copyCount, this.vertices, 0, copyCount);
            this.idx += copyCount;
            count -= copyCount;
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        this.draw(region, x, y, (float)region.getRegionWidth(), (float)region.getRegionHeight());
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        Texture texture = region.texture;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float fx2 = x + width;
        float fy2 = y + height;
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x;
        vertices[idx + 1] = y;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x;
        vertices[idx + 6] = fy2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = fx2;
        vertices[idx + 11] = fy2;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = fx2;
        vertices[idx + 16] = y;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        float y4;
        float x4;
        float y3;
        float x3;
        float y2;
        float x2;
        float y1;
        float x1;
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        Texture texture = region.texture;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }
        float p1x = fx;
        float p1y = fy;
        float p2x = fx;
        float p2y = fy2;
        float p3x = fx2;
        float p3y = fy2;
        float p4x = fx2;
        float p4y = fy;
        if (rotation != 0.0f) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }
        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x1;
        vertices[idx + 1] = y1;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x2;
        vertices[idx + 6] = y2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = x3;
        vertices[idx + 11] = y3;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = x4;
        vertices[idx + 16] = y4;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        float v4;
        float u4;
        float v3;
        float u3;
        float v2;
        float u2;
        float v1;
        float u1;
        float y4;
        float x4;
        float y3;
        float x3;
        float y2;
        float x2;
        float y1;
        float x1;
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        Texture texture = region.texture;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float worldOriginX = x + originX;
        float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;
        if (scaleX != 1.0f || scaleY != 1.0f) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }
        float p1x = fx;
        float p1y = fy;
        float p2x = fx;
        float p2y = fy2;
        float p3x = fx2;
        float p3y = fy2;
        float p4x = fx2;
        float p4y = fy;
        if (rotation != 0.0f) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }
        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;
        if (clockwise) {
            u1 = region.u2;
            v1 = region.v2;
            u2 = region.u;
            v2 = region.v2;
            u3 = region.u;
            v3 = region.v;
            u4 = region.u2;
            v4 = region.v;
        } else {
            u1 = region.u;
            v1 = region.v;
            u2 = region.u2;
            v2 = region.v;
            u3 = region.u2;
            v3 = region.v2;
            u4 = region.u;
            v4 = region.v2;
        }
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x1;
        vertices[idx + 1] = y1;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u1;
        vertices[idx + 4] = v1;
        vertices[idx + 5] = x2;
        vertices[idx + 6] = y2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u2;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = x3;
        vertices[idx + 11] = y3;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u3;
        vertices[idx + 14] = v3;
        vertices[idx + 15] = x4;
        vertices[idx + 16] = y4;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u4;
        vertices[idx + 19] = v4;
        this.idx = idx + 20;
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        if (!this.drawing) {
            throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
        }
        float[] vertices = this.vertices;
        Texture texture = region.texture;
        if (texture != this.lastTexture) {
            this.switchTexture(texture);
        } else if (this.idx == vertices.length) {
            this.flush();
        }
        float x1 = transform.m02;
        float y1 = transform.m12;
        float x2 = transform.m01 * height + transform.m02;
        float y2 = transform.m11 * height + transform.m12;
        float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
        float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
        float x4 = transform.m00 * width + transform.m02;
        float y4 = transform.m10 * width + transform.m12;
        float u = region.u;
        float v = region.v2;
        float u2 = region.u2;
        float v2 = region.v;
        float color = this.color;
        int idx = this.idx;
        vertices[idx] = x1;
        vertices[idx + 1] = y1;
        vertices[idx + 2] = color;
        vertices[idx + 3] = u;
        vertices[idx + 4] = v;
        vertices[idx + 5] = x2;
        vertices[idx + 6] = y2;
        vertices[idx + 7] = color;
        vertices[idx + 8] = u;
        vertices[idx + 9] = v2;
        vertices[idx + 10] = x3;
        vertices[idx + 11] = y3;
        vertices[idx + 12] = color;
        vertices[idx + 13] = u2;
        vertices[idx + 14] = v2;
        vertices[idx + 15] = x4;
        vertices[idx + 16] = y4;
        vertices[idx + 17] = color;
        vertices[idx + 18] = u2;
        vertices[idx + 19] = v;
        this.idx = idx + 20;
    }

    @Override
    public void flush() {
        if (this.idx == 0) {
            return;
        }
        ++this.renderCalls;
        ++this.totalRenderCalls;
        int spritesInBatch = this.idx / 20;
        if (spritesInBatch > this.maxSpritesInBatch) {
            this.maxSpritesInBatch = spritesInBatch;
        }
        int count = spritesInBatch * 6;
        this.lastTexture.bind();
        Mesh mesh = this.mesh;
        mesh.setVertices(this.vertices, 0, this.idx);
        mesh.getIndicesBuffer().position(0);
        mesh.getIndicesBuffer().limit(count);
        if (this.blendingDisabled) {
            Gdx.gl.glDisable(3042);
        } else {
            Gdx.gl.glEnable(3042);
            if (this.blendSrcFunc != -1) {
                Gdx.gl.glBlendFunc(this.blendSrcFunc, this.blendDstFunc);
            }
        }
        mesh.render(this.customShader != null ? this.customShader : this.shader, 4, 0, count);
        this.idx = 0;
    }

    @Override
    public void disableBlending() {
        if (this.blendingDisabled) {
            return;
        }
        this.flush();
        this.blendingDisabled = true;
    }

    @Override
    public void enableBlending() {
        if (!this.blendingDisabled) {
            return;
        }
        this.flush();
        this.blendingDisabled = false;
    }

    @Override
    public void setBlendFunction(int srcFunc, int dstFunc) {
        if (this.blendSrcFunc == srcFunc && this.blendDstFunc == dstFunc) {
            return;
        }
        this.flush();
        this.blendSrcFunc = srcFunc;
        this.blendDstFunc = dstFunc;
    }

    @Override
    public int getBlendSrcFunc() {
        return this.blendSrcFunc;
    }

    @Override
    public int getBlendDstFunc() {
        return this.blendDstFunc;
    }

    @Override
    public void dispose() {
        this.mesh.dispose();
        if (this.ownsShader && this.shader != null) {
            this.shader.dispose();
        }
    }

    @Override
    public Matrix4 getProjectionMatrix() {
        return this.projectionMatrix;
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return this.transformMatrix;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        if (this.drawing) {
            this.flush();
        }
        this.projectionMatrix.set(projection);
        if (this.drawing) {
            this.setupMatrices();
        }
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        if (this.drawing) {
            this.flush();
        }
        this.transformMatrix.set(transform);
        if (this.drawing) {
            this.setupMatrices();
        }
    }

    private void setupMatrices() {
        this.combinedMatrix.set(this.projectionMatrix).mul(this.transformMatrix);
        if (this.customShader != null) {
            this.customShader.setUniformMatrix("u_projTrans", this.combinedMatrix);
            this.customShader.setUniformi("u_texture", 0);
        } else {
            this.shader.setUniformMatrix("u_projTrans", this.combinedMatrix);
            this.shader.setUniformi("u_texture", 0);
        }
    }

    protected void switchTexture(Texture texture) {
        this.flush();
        this.lastTexture = texture;
        this.invTexWidth = 1.0f / (float)texture.getWidth();
        this.invTexHeight = 1.0f / (float)texture.getHeight();
    }

    @Override
    public void setShader(ShaderProgram shader) {
        if (this.drawing) {
            this.flush();
            if (this.customShader != null) {
                this.customShader.end();
            } else {
                this.shader.end();
            }
        }
        this.customShader = shader;
        if (this.drawing) {
            if (this.customShader != null) {
                this.customShader.begin();
            } else {
                this.shader.begin();
            }
            this.setupMatrices();
        }
    }

    @Override
    public ShaderProgram getShader() {
        if (this.customShader == null) {
            return this.shader;
        }
        return this.customShader;
    }

    @Override
    public boolean isBlendingEnabled() {
        return !this.blendingDisabled;
    }

    @Override
    public boolean isDrawing() {
        return this.drawing;
    }
}

