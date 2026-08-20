/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ParticleShader
extends BaseShader {
    private static String defaultVertexShader = null;
    private static String defaultFragmentShader = null;
    protected static long implementedFlags = BlendingAttribute.Type | TextureAttribute.Diffuse;
    static final Vector3 TMP_VECTOR3 = new Vector3();
    private Renderable renderable;
    private long materialMask;
    private long vertexMask;
    protected final Config config;
    private static final long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;
    Material currentMaterial;

    public static String getDefaultVertexShader() {
        if (defaultVertexShader == null) {
            defaultVertexShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.vertex.glsl").readString();
        }
        return defaultVertexShader;
    }

    public static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null) {
            defaultFragmentShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.fragment.glsl").readString();
        }
        return defaultFragmentShader;
    }

    public ParticleShader(Renderable renderable) {
        this(renderable, new Config());
    }

    public ParticleShader(Renderable renderable, Config config) {
        this(renderable, config, ParticleShader.createPrefix(renderable, config));
    }

    public ParticleShader(Renderable renderable, Config config, String prefix) {
        this(renderable, config, prefix, config.vertexShader != null ? config.vertexShader : ParticleShader.getDefaultVertexShader(), config.fragmentShader != null ? config.fragmentShader : ParticleShader.getDefaultFragmentShader());
    }

    public ParticleShader(Renderable renderable, Config config, String prefix, String vertexShader, String fragmentShader) {
        this(renderable, config, new ShaderProgram(prefix + vertexShader, prefix + fragmentShader));
    }

    public ParticleShader(Renderable renderable, Config config, ShaderProgram shaderProgram) {
        this.config = config;
        this.program = shaderProgram;
        this.renderable = renderable;
        this.materialMask = renderable.material.getMask() | optionalAttributes;
        this.vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMask();
        if (!config.ignoreUnimplemented && (implementedFlags & this.materialMask) != this.materialMask) {
            throw new GdxRuntimeException("Some attributes not implemented yet (" + this.materialMask + ")");
        }
        this.register(DefaultShader.Inputs.viewTrans, DefaultShader.Setters.viewTrans);
        this.register(DefaultShader.Inputs.projViewTrans, DefaultShader.Setters.projViewTrans);
        this.register(DefaultShader.Inputs.projTrans, DefaultShader.Setters.projTrans);
        this.register(Inputs.screenWidth, Setters.screenWidth);
        this.register(DefaultShader.Inputs.cameraUp, Setters.cameraUp);
        this.register(Inputs.cameraRight, Setters.cameraRight);
        this.register(Inputs.cameraInvDirection, Setters.cameraInvDirection);
        this.register(DefaultShader.Inputs.cameraPosition, Setters.cameraPosition);
        this.register(DefaultShader.Inputs.diffuseTexture, DefaultShader.Setters.diffuseTexture);
    }

    @Override
    public void init() {
        ShaderProgram program = this.program;
        this.program = null;
        this.init(program, this.renderable);
        this.renderable = null;
    }

    public static String createPrefix(Renderable renderable, Config config) {
        String prefix = "";
        prefix = Gdx.app.getType() == Application.ApplicationType.Desktop ? prefix + "#version 120\n" : prefix + "#version 100\n";
        if (config.type == ParticleType.Billboard) {
            prefix = prefix + "#define billboard\n";
            if (config.align == AlignMode.Screen) {
                prefix = prefix + "#define screenFacing\n";
            } else if (config.align == AlignMode.ViewPoint) {
                prefix = prefix + "#define viewPointFacing\n";
            }
        }
        return prefix;
    }

    @Override
    public boolean canRender(Renderable renderable) {
        return this.materialMask == (renderable.material.getMask() | optionalAttributes) && this.vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMask();
    }

    @Override
    public int compareTo(Shader other) {
        if (other == null) {
            return -1;
        }
        if (other == this) {
            return 0;
        }
        return 0;
    }

    public boolean equals(Object obj) {
        return obj instanceof ParticleShader ? this.equals((ParticleShader)obj) : false;
    }

    public boolean equals(ParticleShader obj) {
        return obj == this;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);
    }

    @Override
    public void render(Renderable renderable) {
        if (!renderable.material.has(BlendingAttribute.Type)) {
            this.context.setBlending(false, 770, 771);
        }
        this.bindMaterial(renderable);
        super.render(renderable);
    }

    @Override
    public void end() {
        this.currentMaterial = null;
        super.end();
    }

    protected void bindMaterial(Renderable renderable) {
        if (this.currentMaterial == renderable.material) {
            return;
        }
        int cullFace = this.config.defaultCullFace == -1 ? 1029 : this.config.defaultCullFace;
        int depthFunc = this.config.defaultDepthFunc == -1 ? 515 : this.config.defaultDepthFunc;
        float depthRangeNear = 0.0f;
        float depthRangeFar = 1.0f;
        boolean depthMask = true;
        this.currentMaterial = renderable.material;
        for (Attribute attr : this.currentMaterial) {
            long t = attr.type;
            if (BlendingAttribute.is(t)) {
                this.context.setBlending(true, ((BlendingAttribute)attr).sourceFunction, ((BlendingAttribute)attr).destFunction);
                continue;
            }
            if ((t & DepthTestAttribute.Type) == DepthTestAttribute.Type) {
                DepthTestAttribute dta = (DepthTestAttribute)attr;
                depthFunc = dta.depthFunc;
                depthRangeNear = dta.depthRangeNear;
                depthRangeFar = dta.depthRangeFar;
                depthMask = dta.depthMask;
                continue;
            }
            if (this.config.ignoreUnimplemented) continue;
            throw new GdxRuntimeException("Unknown material attribute: " + attr.toString());
        }
        this.context.setCullFace(cullFace);
        this.context.setDepthTest(depthFunc, depthRangeNear, depthRangeFar);
        this.context.setDepthMask(depthMask);
    }

    @Override
    public void dispose() {
        this.program.dispose();
        super.dispose();
    }

    public int getDefaultCullFace() {
        return this.config.defaultCullFace == -1 ? 1029 : this.config.defaultCullFace;
    }

    public void setDefaultCullFace(int cullFace) {
        this.config.defaultCullFace = cullFace;
    }

    public int getDefaultDepthFunc() {
        return this.config.defaultDepthFunc == -1 ? 515 : this.config.defaultDepthFunc;
    }

    public void setDefaultDepthFunc(int depthFunc) {
        this.config.defaultDepthFunc = depthFunc;
    }

    public static class Setters {
        public static final BaseShader.Setter cameraRight = new BaseShader.Setter(){

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, TMP_VECTOR3.set(shader.camera.direction).crs(shader.camera.up).nor());
            }
        };
        public static final BaseShader.Setter cameraUp = new BaseShader.Setter(){

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, TMP_VECTOR3.set(shader.camera.up).nor());
            }
        };
        public static final BaseShader.Setter cameraInvDirection = new BaseShader.Setter(){

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, TMP_VECTOR3.set(-shader.camera.direction.x, -shader.camera.direction.y, -shader.camera.direction.z).nor());
            }
        };
        public static final BaseShader.Setter cameraPosition = new BaseShader.Setter(){

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.position);
            }
        };
        public static final BaseShader.Setter screenWidth = new BaseShader.Setter(){

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, (float)Gdx.graphics.getWidth());
            }
        };
        public static final BaseShader.Setter worldViewTrans = new BaseShader.Setter(){
            final Matrix4 temp = new Matrix4();

            @Override
            public boolean isGlobal(BaseShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, this.temp.set(shader.camera.view).mul(renderable.worldTransform));
            }
        };
    }

    public static class Inputs {
        public static final BaseShader.Uniform cameraRight = new BaseShader.Uniform("u_cameraRight");
        public static final BaseShader.Uniform cameraInvDirection = new BaseShader.Uniform("u_cameraInvDirection");
        public static final BaseShader.Uniform screenWidth = new BaseShader.Uniform("u_screenWidth");
        public static final BaseShader.Uniform regionSize = new BaseShader.Uniform("u_regionSize");
    }

    public static class Config {
        public String vertexShader = null;
        public String fragmentShader = null;
        public boolean ignoreUnimplemented = true;
        public int defaultCullFace = -1;
        public int defaultDepthFunc = -1;
        public AlignMode align = AlignMode.Screen;
        public ParticleType type = ParticleType.Billboard;

        public Config() {
        }

        public Config(AlignMode align, ParticleType type) {
            this.align = align;
            this.type = type;
        }

        public Config(AlignMode align) {
            this.align = align;
        }

        public Config(ParticleType type) {
            this.type = type;
        }

        public Config(String vertexShader, String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }
    }

    public static enum AlignMode {
        Screen,
        ViewPoint;

    }

    public static enum ParticleType {
        Billboard,
        Point;

    }
}

