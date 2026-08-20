/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.shaders;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;

public abstract class BaseShader
implements Shader {
    private final Array<String> uniforms = new Array();
    private final Array<Validator> validators = new Array();
    private final Array<Setter> setters = new Array();
    private int[] locations;
    private final IntArray globalUniforms = new IntArray();
    private final IntArray localUniforms = new IntArray();
    private final IntIntMap attributes = new IntIntMap();
    public ShaderProgram program;
    public RenderContext context;
    public Camera camera;
    private Mesh currentMesh;
    private final IntArray tempArray = new IntArray();
    private Attributes combinedAttributes = new Attributes();

    public int register(String alias, Validator validator, Setter setter) {
        if (this.locations != null) {
            throw new GdxRuntimeException("Cannot register an uniform after initialization");
        }
        int existing = this.getUniformID(alias);
        if (existing >= 0) {
            this.validators.set(existing, validator);
            this.setters.set(existing, setter);
            return existing;
        }
        this.uniforms.add(alias);
        this.validators.add(validator);
        this.setters.add(setter);
        return this.uniforms.size - 1;
    }

    public int register(String alias, Validator validator) {
        return this.register(alias, validator, null);
    }

    public int register(String alias, Setter setter) {
        return this.register(alias, null, setter);
    }

    public int register(String alias) {
        return this.register(alias, null, null);
    }

    public int register(Uniform uniform, Setter setter) {
        return this.register(uniform.alias, uniform, setter);
    }

    public int register(Uniform uniform) {
        return this.register(uniform, null);
    }

    public int getUniformID(String alias) {
        int n = this.uniforms.size;
        for (int i = 0; i < n; ++i) {
            if (!this.uniforms.get(i).equals(alias)) continue;
            return i;
        }
        return -1;
    }

    public String getUniformAlias(int id) {
        return this.uniforms.get(id);
    }

    public void init(ShaderProgram program, Renderable renderable) {
        if (this.locations != null) {
            throw new GdxRuntimeException("Already initialized");
        }
        if (!program.isCompiled()) {
            throw new GdxRuntimeException(program.getLog());
        }
        this.program = program;
        int n = this.uniforms.size;
        this.locations = new int[n];
        for (int i = 0; i < n; ++i) {
            String input = this.uniforms.get(i);
            Validator validator = this.validators.get(i);
            Setter setter = this.setters.get(i);
            if (validator != null && !validator.validate(this, i, renderable)) {
                this.locations[i] = -1;
            } else {
                this.locations[i] = program.fetchUniformLocation(input, false);
                if (this.locations[i] >= 0 && setter != null) {
                    if (setter.isGlobal(this, i)) {
                        this.globalUniforms.add(i);
                    } else {
                        this.localUniforms.add(i);
                    }
                }
            }
            if (this.locations[i] >= 0) continue;
            this.validators.set(i, null);
            this.setters.set(i, null);
        }
        if (renderable != null) {
            VertexAttributes attrs = renderable.meshPart.mesh.getVertexAttributes();
            int c = attrs.size();
            for (int i = 0; i < c; ++i) {
                VertexAttribute attr = attrs.get(i);
                int location = program.getAttributeLocation(attr.alias);
                if (location < 0) continue;
                this.attributes.put(attr.getKey(), location);
            }
        }
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        this.program.begin();
        this.currentMesh = null;
        for (int i = 0; i < this.globalUniforms.size; ++i) {
            int u = this.globalUniforms.get(i);
            if (this.setters.get(u) == null) continue;
            this.setters.get(u).set(this, u, null, null);
        }
    }

    private final int[] getAttributeLocations(VertexAttributes attrs) {
        this.tempArray.clear();
        int n = attrs.size();
        for (int i = 0; i < n; ++i) {
            this.tempArray.add(this.attributes.get(attrs.get(i).getKey(), -1));
        }
        return this.tempArray.items;
    }

    @Override
    public void render(Renderable renderable) {
        if (renderable.worldTransform.det3x3() == 0.0f) {
            return;
        }
        this.combinedAttributes.clear();
        if (renderable.environment != null) {
            this.combinedAttributes.set(renderable.environment);
        }
        if (renderable.material != null) {
            this.combinedAttributes.set(renderable.material);
        }
        this.render(renderable, this.combinedAttributes);
    }

    public void render(Renderable renderable, Attributes combinedAttributes) {
        for (int i = 0; i < this.localUniforms.size; ++i) {
            int u = this.localUniforms.get(i);
            if (this.setters.get(u) == null) continue;
            this.setters.get(u).set(this, u, renderable, combinedAttributes);
        }
        if (this.currentMesh != renderable.meshPart.mesh) {
            if (this.currentMesh != null) {
                this.currentMesh.unbind(this.program, this.tempArray.items);
            }
            this.currentMesh = renderable.meshPart.mesh;
            this.currentMesh.bind(this.program, this.getAttributeLocations(renderable.meshPart.mesh.getVertexAttributes()));
        }
        renderable.meshPart.render(this.program, false);
    }

    @Override
    public void end() {
        if (this.currentMesh != null) {
            this.currentMesh.unbind(this.program, this.tempArray.items);
            this.currentMesh = null;
        }
        this.program.end();
    }

    @Override
    public void dispose() {
        this.program = null;
        this.uniforms.clear();
        this.validators.clear();
        this.setters.clear();
        this.localUniforms.clear();
        this.globalUniforms.clear();
        this.locations = null;
    }

    public final boolean has(int inputID) {
        return inputID >= 0 && inputID < this.locations.length && this.locations[inputID] >= 0;
    }

    public final int loc(int inputID) {
        return inputID >= 0 && inputID < this.locations.length ? this.locations[inputID] : -1;
    }

    public final boolean set(int uniform, Matrix4 value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformMatrix(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Matrix3 value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformMatrix(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Vector3 value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Vector2 value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, Color value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, float value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], v1, v2);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2, float v3) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], v1, v2, v3);
        return true;
    }

    public final boolean set(int uniform, float v1, float v2, float v3, float v4) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformf(this.locations[uniform], v1, v2, v3, v4);
        return true;
    }

    public final boolean set(int uniform, int value) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], value);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], v1, v2);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2, int v3) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], v1, v2, v3);
        return true;
    }

    public final boolean set(int uniform, int v1, int v2, int v3, int v4) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], v1, v2, v3, v4);
        return true;
    }

    public final boolean set(int uniform, TextureDescriptor textureDesc) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], this.context.textureBinder.bind(textureDesc));
        return true;
    }

    public final boolean set(int uniform, GLTexture texture) {
        if (this.locations[uniform] < 0) {
            return false;
        }
        this.program.setUniformi(this.locations[uniform], this.context.textureBinder.bind(texture));
        return true;
    }

    public static class Uniform
    implements Validator {
        public final String alias;
        public final long materialMask;
        public final long environmentMask;
        public final long overallMask;

        public Uniform(String alias, long materialMask, long environmentMask, long overallMask) {
            this.alias = alias;
            this.materialMask = materialMask;
            this.environmentMask = environmentMask;
            this.overallMask = overallMask;
        }

        public Uniform(String alias, long materialMask, long environmentMask) {
            this(alias, materialMask, environmentMask, 0L);
        }

        public Uniform(String alias, long overallMask) {
            this(alias, 0L, 0L, overallMask);
        }

        public Uniform(String alias) {
            this(alias, 0L, 0L);
        }

        @Override
        public boolean validate(BaseShader shader, int inputID, Renderable renderable) {
            long matFlags = renderable != null && renderable.material != null ? renderable.material.getMask() : 0L;
            long envFlags = renderable != null && renderable.environment != null ? renderable.environment.getMask() : 0L;
            return (matFlags & this.materialMask) == this.materialMask && (envFlags & this.environmentMask) == this.environmentMask && ((matFlags | envFlags) & this.overallMask) == this.overallMask;
        }
    }

    public static abstract class LocalSetter
    implements Setter {
        @Override
        public boolean isGlobal(BaseShader shader, int inputID) {
            return false;
        }
    }

    public static abstract class GlobalSetter
    implements Setter {
        @Override
        public boolean isGlobal(BaseShader shader, int inputID) {
            return true;
        }
    }

    public static interface Setter {
        public boolean isGlobal(BaseShader var1, int var2);

        public void set(BaseShader var1, int var2, Renderable var3, Attributes var4);
    }

    public static interface Validator {
        public boolean validate(BaseShader var1, int var2, Renderable var3);
    }
}

