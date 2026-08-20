/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class GLFrameBuffer<T extends GLTexture>
implements Disposable {
    private static final Map<Application, Array<GLFrameBuffer>> buffers = new HashMap<Application, Array<GLFrameBuffer>>();
    private static final int GL_DEPTH24_STENCIL8_OES = 35056;
    protected T colorTexture;
    private static int defaultFramebufferHandle;
    private static boolean defaultFramebufferHandleInitialized;
    private int framebufferHandle;
    private int depthbufferHandle;
    private int stencilbufferHandle;
    private int depthStencilPackedBufferHandle;
    protected final int width;
    protected final int height;
    protected final boolean hasDepth;
    protected final boolean hasStencil;
    private boolean hasDepthStencilPackedBuffer;
    protected final Pixmap.Format format;

    public GLFrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth) {
        this(format, width, height, hasDepth, false);
    }

    public GLFrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.hasDepth = hasDepth;
        this.hasStencil = hasStencil;
        this.build();
        GLFrameBuffer.addManagedFrameBuffer(Gdx.app, this);
    }

    protected abstract T createColorTexture();

    protected abstract void disposeColorTexture(T var1);

    protected abstract void attachFrameBufferColorTexture();

    private void build() {
        GL20 gl = Gdx.gl20;
        if (!defaultFramebufferHandleInitialized) {
            defaultFramebufferHandleInitialized = true;
            if (Gdx.app.getType() == Application.ApplicationType.iOS) {
                IntBuffer intbuf = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
                gl.glGetIntegerv(36006, intbuf);
                defaultFramebufferHandle = intbuf.get(0);
            } else {
                defaultFramebufferHandle = 0;
            }
        }
        this.colorTexture = this.createColorTexture();
        this.framebufferHandle = gl.glGenFramebuffer();
        if (this.hasDepth) {
            this.depthbufferHandle = gl.glGenRenderbuffer();
        }
        if (this.hasStencil) {
            this.stencilbufferHandle = gl.glGenRenderbuffer();
        }
        gl.glBindTexture(((GLTexture)this.colorTexture).glTarget, ((GLTexture)this.colorTexture).getTextureObjectHandle());
        if (this.hasDepth) {
            gl.glBindRenderbuffer(36161, this.depthbufferHandle);
            gl.glRenderbufferStorage(36161, 33189, ((GLTexture)this.colorTexture).getWidth(), ((GLTexture)this.colorTexture).getHeight());
        }
        if (this.hasStencil) {
            gl.glBindRenderbuffer(36161, this.stencilbufferHandle);
            gl.glRenderbufferStorage(36161, 36168, ((GLTexture)this.colorTexture).getWidth(), ((GLTexture)this.colorTexture).getHeight());
        }
        gl.glBindFramebuffer(36160, this.framebufferHandle);
        this.attachFrameBufferColorTexture();
        if (this.hasDepth) {
            gl.glFramebufferRenderbuffer(36160, 36096, 36161, this.depthbufferHandle);
        }
        if (this.hasStencil) {
            gl.glFramebufferRenderbuffer(36160, 36128, 36161, this.stencilbufferHandle);
        }
        gl.glBindRenderbuffer(36161, 0);
        gl.glBindTexture(((GLTexture)this.colorTexture).glTarget, 0);
        int result = gl.glCheckFramebufferStatus(36160);
        if (result == 36061 && this.hasDepth && this.hasStencil && (Gdx.graphics.supportsExtension("GL_OES_packed_depth_stencil") || Gdx.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))) {
            if (this.hasDepth) {
                gl.glDeleteRenderbuffer(this.depthbufferHandle);
                this.depthbufferHandle = 0;
            }
            if (this.hasStencil) {
                gl.glDeleteRenderbuffer(this.stencilbufferHandle);
                this.stencilbufferHandle = 0;
            }
            this.depthStencilPackedBufferHandle = gl.glGenRenderbuffer();
            this.hasDepthStencilPackedBuffer = true;
            gl.glBindRenderbuffer(36161, this.depthStencilPackedBufferHandle);
            gl.glRenderbufferStorage(36161, 35056, ((GLTexture)this.colorTexture).getWidth(), ((GLTexture)this.colorTexture).getHeight());
            gl.glBindRenderbuffer(36161, 0);
            gl.glFramebufferRenderbuffer(36160, 36096, 36161, this.depthStencilPackedBufferHandle);
            gl.glFramebufferRenderbuffer(36160, 36128, 36161, this.depthStencilPackedBufferHandle);
            result = gl.glCheckFramebufferStatus(36160);
        }
        gl.glBindFramebuffer(36160, defaultFramebufferHandle);
        if (result != 36053) {
            this.disposeColorTexture(this.colorTexture);
            if (this.hasDepthStencilPackedBuffer) {
                gl.glDeleteBuffer(this.depthStencilPackedBufferHandle);
            } else {
                if (this.hasDepth) {
                    gl.glDeleteRenderbuffer(this.depthbufferHandle);
                }
                if (this.hasStencil) {
                    gl.glDeleteRenderbuffer(this.stencilbufferHandle);
                }
            }
            gl.glDeleteFramebuffer(this.framebufferHandle);
            if (result == 36054) {
                throw new IllegalStateException("frame buffer couldn't be constructed: incomplete attachment");
            }
            if (result == 36057) {
                throw new IllegalStateException("frame buffer couldn't be constructed: incomplete dimensions");
            }
            if (result == 36055) {
                throw new IllegalStateException("frame buffer couldn't be constructed: missing attachment");
            }
            if (result == 36061) {
                throw new IllegalStateException("frame buffer couldn't be constructed: unsupported combination of formats");
            }
            throw new IllegalStateException("frame buffer couldn't be constructed: unknown error " + result);
        }
    }

    @Override
    public void dispose() {
        GL20 gl = Gdx.gl20;
        this.disposeColorTexture(this.colorTexture);
        if (this.hasDepthStencilPackedBuffer) {
            gl.glDeleteRenderbuffer(this.depthStencilPackedBufferHandle);
        } else {
            if (this.hasDepth) {
                gl.glDeleteRenderbuffer(this.depthbufferHandle);
            }
            if (this.hasStencil) {
                gl.glDeleteRenderbuffer(this.stencilbufferHandle);
            }
        }
        gl.glDeleteFramebuffer(this.framebufferHandle);
        if (buffers.get(Gdx.app) != null) {
            buffers.get(Gdx.app).removeValue(this, true);
        }
    }

    public void bind() {
        Gdx.gl20.glBindFramebuffer(36160, this.framebufferHandle);
    }

    public static void unbind() {
        Gdx.gl20.glBindFramebuffer(36160, defaultFramebufferHandle);
    }

    public void begin() {
        this.bind();
        this.setFrameBufferViewport();
    }

    protected void setFrameBufferViewport() {
        Gdx.gl20.glViewport(0, 0, ((GLTexture)this.colorTexture).getWidth(), ((GLTexture)this.colorTexture).getHeight());
    }

    public void end() {
        this.end(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    public void end(int x, int y, int width, int height) {
        GLFrameBuffer.unbind();
        Gdx.gl20.glViewport(x, y, width, height);
    }

    public T getColorBufferTexture() {
        return this.colorTexture;
    }

    public int getFramebufferHandle() {
        return this.framebufferHandle;
    }

    public int getDepthBufferHandle() {
        return this.depthbufferHandle;
    }

    public int getStencilBufferHandle() {
        return this.stencilbufferHandle;
    }

    protected int getDepthStencilPackedBuffer() {
        return this.depthStencilPackedBufferHandle;
    }

    public int getHeight() {
        return ((GLTexture)this.colorTexture).getHeight();
    }

    public int getWidth() {
        return ((GLTexture)this.colorTexture).getWidth();
    }

    public int getDepth() {
        return ((GLTexture)this.colorTexture).getDepth();
    }

    private static void addManagedFrameBuffer(Application app, GLFrameBuffer frameBuffer) {
        Array managedResources = buffers.get(app);
        if (managedResources == null) {
            managedResources = new Array();
        }
        managedResources.add(frameBuffer);
        buffers.put(app, managedResources);
    }

    public static void invalidateAllFrameBuffers(Application app) {
        if (Gdx.gl20 == null) {
            return;
        }
        Array<GLFrameBuffer> bufferArray = buffers.get(app);
        if (bufferArray == null) {
            return;
        }
        for (int i = 0; i < bufferArray.size; ++i) {
            bufferArray.get(i).build();
        }
    }

    public static void clearAllFrameBuffers(Application app) {
        buffers.remove(app);
    }

    public static StringBuilder getManagedStatus(StringBuilder builder) {
        builder.append("Managed buffers/app: { ");
        for (Application app : buffers.keySet()) {
            builder.append(GLFrameBuffer.buffers.get((Object)app).size);
            builder.append(" ");
        }
        builder.append("}");
        return builder;
    }

    public static String getManagedStatus() {
        return GLFrameBuffer.getManagedStatus(new StringBuilder()).toString();
    }

    static {
        defaultFramebufferHandleInitialized = false;
    }
}

