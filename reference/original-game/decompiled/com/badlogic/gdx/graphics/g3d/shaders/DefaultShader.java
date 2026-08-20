/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.AmbientCubemap;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class DefaultShader
extends BaseShader {
    private static String defaultVertexShader = null;
    private static String defaultFragmentShader = null;
    protected static long implementedFlags = BlendingAttribute.Type | TextureAttribute.Diffuse | ColorAttribute.Diffuse | ColorAttribute.Specular | FloatAttribute.Shininess;
    @Deprecated
    public static int defaultCullFace = 1029;
    @Deprecated
    public static int defaultDepthFunc = 515;
    public final int u_projTrans;
    public final int u_viewTrans;
    public final int u_projViewTrans;
    public final int u_cameraPosition;
    public final int u_cameraDirection;
    public final int u_cameraUp;
    public final int u_cameraNearFar;
    public final int u_time;
    public final int u_worldTrans;
    public final int u_viewWorldTrans;
    public final int u_projViewWorldTrans;
    public final int u_normalMatrix;
    public final int u_bones;
    public final int u_shininess;
    public final int u_opacity;
    public final int u_diffuseColor;
    public final int u_diffuseTexture;
    public final int u_diffuseUVTransform;
    public final int u_specularColor;
    public final int u_specularTexture;
    public final int u_specularUVTransform;
    public final int u_emissiveColor;
    public final int u_emissiveTexture;
    public final int u_emissiveUVTransform;
    public final int u_reflectionColor;
    public final int u_reflectionTexture;
    public final int u_reflectionUVTransform;
    public final int u_normalTexture;
    public final int u_normalUVTransform;
    public final int u_ambientTexture;
    public final int u_ambientUVTransform;
    public final int u_alphaTest;
    protected final int u_ambientCubemap;
    protected final int u_environmentCubemap;
    protected final int u_dirLights0color = this.register(new BaseShader.Uniform("u_dirLights[0].color"));
    protected final int u_dirLights0direction = this.register(new BaseShader.Uniform("u_dirLights[0].direction"));
    protected final int u_dirLights1color = this.register(new BaseShader.Uniform("u_dirLights[1].color"));
    protected final int u_pointLights0color = this.register(new BaseShader.Uniform("u_pointLights[0].color"));
    protected final int u_pointLights0position = this.register(new BaseShader.Uniform("u_pointLights[0].position"));
    protected final int u_pointLights0intensity = this.register(new BaseShader.Uniform("u_pointLights[0].intensity"));
    protected final int u_pointLights1color = this.register(new BaseShader.Uniform("u_pointLights[1].color"));
    protected final int u_spotLights0color = this.register(new BaseShader.Uniform("u_spotLights[0].color"));
    protected final int u_spotLights0position = this.register(new BaseShader.Uniform("u_spotLights[0].position"));
    protected final int u_spotLights0intensity = this.register(new BaseShader.Uniform("u_spotLights[0].intensity"));
    protected final int u_spotLights0direction = this.register(new BaseShader.Uniform("u_spotLights[0].direction"));
    protected final int u_spotLights0cutoffAngle = this.register(new BaseShader.Uniform("u_spotLights[0].cutoffAngle"));
    protected final int u_spotLights0exponent = this.register(new BaseShader.Uniform("u_spotLights[0].exponent"));
    protected final int u_spotLights1color = this.register(new BaseShader.Uniform("u_spotLights[1].color"));
    protected final int u_fogColor = this.register(new BaseShader.Uniform("u_fogColor"));
    protected final int u_shadowMapProjViewTrans = this.register(new BaseShader.Uniform("u_shadowMapProjViewTrans"));
    protected final int u_shadowTexture = this.register(new BaseShader.Uniform("u_shadowTexture"));
    protected final int u_shadowPCFOffset = this.register(new BaseShader.Uniform("u_shadowPCFOffset"));
    protected int dirLightsLoc;
    protected int dirLightsColorOffset;
    protected int dirLightsDirectionOffset;
    protected int dirLightsSize;
    protected int pointLightsLoc;
    protected int pointLightsColorOffset;
    protected int pointLightsPositionOffset;
    protected int pointLightsIntensityOffset;
    protected int pointLightsSize;
    protected int spotLightsLoc;
    protected int spotLightsColorOffset;
    protected int spotLightsPositionOffset;
    protected int spotLightsDirectionOffset;
    protected int spotLightsIntensityOffset;
    protected int spotLightsCutoffAngleOffset;
    protected int spotLightsExponentOffset;
    protected int spotLightsSize;
    protected final boolean lighting;
    protected final boolean environmentCubemap;
    protected final boolean shadowMap;
    protected final AmbientCubemap ambientCubemap = new AmbientCubemap();
    protected final DirectionalLight[] directionalLights;
    protected final PointLight[] pointLights;
    protected final SpotLight[] spotLights;
    private Renderable renderable;
    protected final long attributesMask;
    private final long vertexMask;
    protected final Config config;
    private static final long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;
    private static final Attributes tmpAttributes = new Attributes();
    private final Matrix3 normalMatrix = new Matrix3();
    private float time;
    private boolean lightsSet;
    private final Vector3 tmpV1 = new Vector3();

    public static String getDefaultVertexShader() {
        if (defaultVertexShader == null) {
            defaultVertexShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl").readString();
        }
        return defaultVertexShader;
    }

    public static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null) {
            defaultFragmentShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl").readString();
        }
        return defaultFragmentShader;
    }

    public DefaultShader(Renderable renderable) {
        this(renderable, new Config());
    }

    public DefaultShader(Renderable renderable, Config config) {
        this(renderable, config, DefaultShader.createPrefix(renderable, config));
    }

    public DefaultShader(Renderable renderable, Config config, String prefix) {
        this(renderable, config, prefix, config.vertexShader != null ? config.vertexShader : DefaultShader.getDefaultVertexShader(), config.fragmentShader != null ? config.fragmentShader : DefaultShader.getDefaultFragmentShader());
    }

    public DefaultShader(Renderable renderable, Config config, String prefix, String vertexShader, String fragmentShader) {
        this(renderable, config, new ShaderProgram(prefix + vertexShader, prefix + fragmentShader));
    }

    public DefaultShader(Renderable renderable, Config config, ShaderProgram shaderProgram) {
        int i;
        Attributes attributes = DefaultShader.combineAttributes(renderable);
        this.config = config;
        this.program = shaderProgram;
        this.lighting = renderable.environment != null;
        this.environmentCubemap = attributes.has(CubemapAttribute.EnvironmentMap) || this.lighting && attributes.has(CubemapAttribute.EnvironmentMap);
        this.shadowMap = this.lighting && renderable.environment.shadowMap != null;
        this.renderable = renderable;
        this.attributesMask = attributes.getMask() | optionalAttributes;
        this.vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMask();
        this.directionalLights = new DirectionalLight[this.lighting && config.numDirectionalLights > 0 ? config.numDirectionalLights : 0];
        for (i = 0; i < this.directionalLights.length; ++i) {
            this.directionalLights[i] = new DirectionalLight();
        }
        this.pointLights = new PointLight[this.lighting && config.numPointLights > 0 ? config.numPointLights : 0];
        for (i = 0; i < this.pointLights.length; ++i) {
            this.pointLights[i] = new PointLight();
        }
        this.spotLights = new SpotLight[this.lighting && config.numSpotLights > 0 ? config.numSpotLights : 0];
        for (i = 0; i < this.spotLights.length; ++i) {
            this.spotLights[i] = new SpotLight();
        }
        if (!config.ignoreUnimplemented && (implementedFlags & this.attributesMask) != this.attributesMask) {
            throw new GdxRuntimeException("Some attributes not implemented yet (" + this.attributesMask + ")");
        }
        this.u_projTrans = this.register(Inputs.projTrans, Setters.projTrans);
        this.u_viewTrans = this.register(Inputs.viewTrans, Setters.viewTrans);
        this.u_projViewTrans = this.register(Inputs.projViewTrans, Setters.projViewTrans);
        this.u_cameraPosition = this.register(Inputs.cameraPosition, Setters.cameraPosition);
        this.u_cameraDirection = this.register(Inputs.cameraDirection, Setters.cameraDirection);
        this.u_cameraUp = this.register(Inputs.cameraUp, Setters.cameraUp);
        this.u_cameraNearFar = this.register(Inputs.cameraNearFar, Setters.cameraNearFar);
        this.u_time = this.register(new BaseShader.Uniform("u_time"));
        this.u_worldTrans = this.register(Inputs.worldTrans, Setters.worldTrans);
        this.u_viewWorldTrans = this.register(Inputs.viewWorldTrans, Setters.viewWorldTrans);
        this.u_projViewWorldTrans = this.register(Inputs.projViewWorldTrans, Setters.projViewWorldTrans);
        this.u_normalMatrix = this.register(Inputs.normalMatrix, Setters.normalMatrix);
        this.u_bones = renderable.bones != null && config.numBones > 0 ? this.register(Inputs.bones, (BaseShader.Setter)new Setters.Bones(config.numBones)) : -1;
        this.u_shininess = this.register(Inputs.shininess, Setters.shininess);
        this.u_opacity = this.register(Inputs.opacity);
        this.u_diffuseColor = this.register(Inputs.diffuseColor, Setters.diffuseColor);
        this.u_diffuseTexture = this.register(Inputs.diffuseTexture, Setters.diffuseTexture);
        this.u_diffuseUVTransform = this.register(Inputs.diffuseUVTransform, Setters.diffuseUVTransform);
        this.u_specularColor = this.register(Inputs.specularColor, Setters.specularColor);
        this.u_specularTexture = this.register(Inputs.specularTexture, Setters.specularTexture);
        this.u_specularUVTransform = this.register(Inputs.specularUVTransform, Setters.specularUVTransform);
        this.u_emissiveColor = this.register(Inputs.emissiveColor, Setters.emissiveColor);
        this.u_emissiveTexture = this.register(Inputs.emissiveTexture, Setters.emissiveTexture);
        this.u_emissiveUVTransform = this.register(Inputs.emissiveUVTransform, Setters.emissiveUVTransform);
        this.u_reflectionColor = this.register(Inputs.reflectionColor, Setters.reflectionColor);
        this.u_reflectionTexture = this.register(Inputs.reflectionTexture, Setters.reflectionTexture);
        this.u_reflectionUVTransform = this.register(Inputs.reflectionUVTransform, Setters.reflectionUVTransform);
        this.u_normalTexture = this.register(Inputs.normalTexture, Setters.normalTexture);
        this.u_normalUVTransform = this.register(Inputs.normalUVTransform, Setters.normalUVTransform);
        this.u_ambientTexture = this.register(Inputs.ambientTexture, Setters.ambientTexture);
        this.u_ambientUVTransform = this.register(Inputs.ambientUVTransform, Setters.ambientUVTransform);
        this.u_alphaTest = this.register(Inputs.alphaTest);
        this.u_ambientCubemap = this.lighting ? this.register(Inputs.ambientCube, (BaseShader.Setter)new Setters.ACubemap(config.numDirectionalLights, config.numPointLights)) : -1;
        this.u_environmentCubemap = this.environmentCubemap ? this.register(Inputs.environmentCubemap, Setters.environmentCubemap) : -1;
    }

    @Override
    public void init() {
        ShaderProgram program = this.program;
        this.program = null;
        this.init(program, this.renderable);
        this.renderable = null;
        this.dirLightsLoc = this.loc(this.u_dirLights0color);
        this.dirLightsColorOffset = this.loc(this.u_dirLights0color) - this.dirLightsLoc;
        this.dirLightsDirectionOffset = this.loc(this.u_dirLights0direction) - this.dirLightsLoc;
        this.dirLightsSize = this.loc(this.u_dirLights1color) - this.dirLightsLoc;
        if (this.dirLightsSize < 0) {
            this.dirLightsSize = 0;
        }
        this.pointLightsLoc = this.loc(this.u_pointLights0color);
        this.pointLightsColorOffset = this.loc(this.u_pointLights0color) - this.pointLightsLoc;
        this.pointLightsPositionOffset = this.loc(this.u_pointLights0position) - this.pointLightsLoc;
        this.pointLightsIntensityOffset = this.has(this.u_pointLights0intensity) ? this.loc(this.u_pointLights0intensity) - this.pointLightsLoc : -1;
        this.pointLightsSize = this.loc(this.u_pointLights1color) - this.pointLightsLoc;
        if (this.pointLightsSize < 0) {
            this.pointLightsSize = 0;
        }
        this.spotLightsLoc = this.loc(this.u_spotLights0color);
        this.spotLightsColorOffset = this.loc(this.u_spotLights0color) - this.spotLightsLoc;
        this.spotLightsPositionOffset = this.loc(this.u_spotLights0position) - this.spotLightsLoc;
        this.spotLightsDirectionOffset = this.loc(this.u_spotLights0direction) - this.spotLightsLoc;
        this.spotLightsIntensityOffset = this.has(this.u_spotLights0intensity) ? this.loc(this.u_spotLights0intensity) - this.spotLightsLoc : -1;
        this.spotLightsCutoffAngleOffset = this.loc(this.u_spotLights0cutoffAngle) - this.spotLightsLoc;
        this.spotLightsExponentOffset = this.loc(this.u_spotLights0exponent) - this.spotLightsLoc;
        this.spotLightsSize = this.loc(this.u_spotLights1color) - this.spotLightsLoc;
        if (this.spotLightsSize < 0) {
            this.spotLightsSize = 0;
        }
    }

    private static final boolean and(long mask, long flag) {
        return (mask & flag) == flag;
    }

    private static final boolean or(long mask, long flag) {
        return (mask & flag) != 0L;
    }

    private static final Attributes combineAttributes(Renderable renderable) {
        tmpAttributes.clear();
        if (renderable.environment != null) {
            tmpAttributes.set(renderable.environment);
        }
        if (renderable.material != null) {
            tmpAttributes.set(renderable.material);
        }
        return tmpAttributes;
    }

    public static String createPrefix(Renderable renderable, Config config) {
        Attributes attributes = DefaultShader.combineAttributes(renderable);
        String prefix = "";
        long attributesMask = attributes.getMask();
        long vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMask();
        if (DefaultShader.and(vertexMask, 1L)) {
            prefix = prefix + "#define positionFlag\n";
        }
        if (DefaultShader.or(vertexMask, 6L)) {
            prefix = prefix + "#define colorFlag\n";
        }
        if (DefaultShader.and(vertexMask, 256L)) {
            prefix = prefix + "#define binormalFlag\n";
        }
        if (DefaultShader.and(vertexMask, 128L)) {
            prefix = prefix + "#define tangentFlag\n";
        }
        if (DefaultShader.and(vertexMask, 8L)) {
            prefix = prefix + "#define normalFlag\n";
        }
        if ((DefaultShader.and(vertexMask, 8L) || DefaultShader.and(vertexMask, 384L)) && renderable.environment != null) {
            prefix = prefix + "#define lightingFlag\n";
            prefix = prefix + "#define ambientCubemapFlag\n";
            prefix = prefix + "#define numDirectionalLights " + config.numDirectionalLights + "\n";
            prefix = prefix + "#define numPointLights " + config.numPointLights + "\n";
            prefix = prefix + "#define numSpotLights " + config.numSpotLights + "\n";
            if (attributes.has(ColorAttribute.Fog)) {
                prefix = prefix + "#define fogFlag\n";
            }
            if (renderable.environment.shadowMap != null) {
                prefix = prefix + "#define shadowMapFlag\n";
            }
            if (attributes.has(CubemapAttribute.EnvironmentMap)) {
                prefix = prefix + "#define environmentCubemapFlag\n";
            }
        }
        int n = renderable.meshPart.mesh.getVertexAttributes().size();
        for (int i = 0; i < n; ++i) {
            VertexAttribute attr = renderable.meshPart.mesh.getVertexAttributes().get(i);
            if (attr.usage == 64) {
                prefix = prefix + "#define boneWeight" + attr.unit + "Flag\n";
                continue;
            }
            if (attr.usage != 16) continue;
            prefix = prefix + "#define texCoord" + attr.unit + "Flag\n";
        }
        if ((attributesMask & BlendingAttribute.Type) == BlendingAttribute.Type) {
            prefix = prefix + "#define blendedFlag\n";
        }
        if ((attributesMask & TextureAttribute.Diffuse) == TextureAttribute.Diffuse) {
            prefix = prefix + "#define diffuseTextureFlag\n";
            prefix = prefix + "#define diffuseTextureCoord texCoord0\n";
        }
        if ((attributesMask & TextureAttribute.Specular) == TextureAttribute.Specular) {
            prefix = prefix + "#define specularTextureFlag\n";
            prefix = prefix + "#define specularTextureCoord texCoord0\n";
        }
        if ((attributesMask & TextureAttribute.Normal) == TextureAttribute.Normal) {
            prefix = prefix + "#define normalTextureFlag\n";
            prefix = prefix + "#define normalTextureCoord texCoord0\n";
        }
        if ((attributesMask & TextureAttribute.Emissive) == TextureAttribute.Emissive) {
            prefix = prefix + "#define emissiveTextureFlag\n";
            prefix = prefix + "#define emissiveTextureCoord texCoord0\n";
        }
        if ((attributesMask & TextureAttribute.Reflection) == TextureAttribute.Reflection) {
            prefix = prefix + "#define reflectionTextureFlag\n";
            prefix = prefix + "#define reflectionTextureCoord texCoord0\n";
        }
        if ((attributesMask & TextureAttribute.Ambient) == TextureAttribute.Ambient) {
            prefix = prefix + "#define ambientTextureFlag\n";
            prefix = prefix + "#define ambientTextureCoord texCoord0\n";
        }
        if ((attributesMask & ColorAttribute.Diffuse) == ColorAttribute.Diffuse) {
            prefix = prefix + "#define diffuseColorFlag\n";
        }
        if ((attributesMask & ColorAttribute.Specular) == ColorAttribute.Specular) {
            prefix = prefix + "#define specularColorFlag\n";
        }
        if ((attributesMask & ColorAttribute.Emissive) == ColorAttribute.Emissive) {
            prefix = prefix + "#define emissiveColorFlag\n";
        }
        if ((attributesMask & ColorAttribute.Reflection) == ColorAttribute.Reflection) {
            prefix = prefix + "#define reflectionColorFlag\n";
        }
        if ((attributesMask & FloatAttribute.Shininess) == FloatAttribute.Shininess) {
            prefix = prefix + "#define shininessFlag\n";
        }
        if ((attributesMask & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest) {
            prefix = prefix + "#define alphaTestFlag\n";
        }
        if (renderable.bones != null && config.numBones > 0) {
            prefix = prefix + "#define numBones " + config.numBones + "\n";
        }
        return prefix;
    }

    @Override
    public boolean canRender(Renderable renderable) {
        Attributes attributes = DefaultShader.combineAttributes(renderable);
        return this.attributesMask == (attributes.getMask() | optionalAttributes) && this.vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMask() && renderable.environment != null == this.lighting;
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
        return obj instanceof DefaultShader ? this.equals((DefaultShader)obj) : false;
    }

    public boolean equals(DefaultShader obj) {
        return obj == this;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);
        for (DirectionalLight directionalLight : this.directionalLights) {
            directionalLight.set(0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
        }
        for (BaseLight baseLight : this.pointLights) {
            ((PointLight)baseLight).set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        for (BaseLight baseLight : this.spotLights) {
            ((SpotLight)baseLight).set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        }
        this.lightsSet = false;
        if (this.has(this.u_time)) {
            this.set(this.u_time, this.time += Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        if (!combinedAttributes.has(BlendingAttribute.Type)) {
            this.context.setBlending(false, 770, 771);
        }
        this.bindMaterial(combinedAttributes);
        if (this.lighting) {
            this.bindLights(renderable, combinedAttributes);
        }
        super.render(renderable, combinedAttributes);
    }

    @Override
    public void end() {
        super.end();
    }

    protected void bindMaterial(Attributes attributes) {
        int cullFace = this.config.defaultCullFace == -1 ? defaultCullFace : this.config.defaultCullFace;
        int depthFunc = this.config.defaultDepthFunc == -1 ? defaultDepthFunc : this.config.defaultDepthFunc;
        float depthRangeNear = 0.0f;
        float depthRangeFar = 1.0f;
        boolean depthMask = true;
        for (Attribute attr : attributes) {
            long t = attr.type;
            if (BlendingAttribute.is(t)) {
                this.context.setBlending(true, ((BlendingAttribute)attr).sourceFunction, ((BlendingAttribute)attr).destFunction);
                this.set(this.u_opacity, ((BlendingAttribute)attr).opacity);
                continue;
            }
            if ((t & IntAttribute.CullFace) == IntAttribute.CullFace) {
                cullFace = ((IntAttribute)attr).value;
                continue;
            }
            if ((t & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest) {
                this.set(this.u_alphaTest, ((FloatAttribute)attr).value);
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

    protected void bindLights(Renderable renderable, Attributes attributes) {
        int idx;
        int i;
        Array<SpotLight> spots;
        Environment lights = renderable.environment;
        DirectionalLightsAttribute dla = attributes.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
        Array<DirectionalLight> dirs = dla == null ? null : dla.lights;
        PointLightsAttribute pla = attributes.get(PointLightsAttribute.class, PointLightsAttribute.Type);
        Array<PointLight> points = pla == null ? null : pla.lights;
        SpotLightsAttribute sla = attributes.get(SpotLightsAttribute.class, SpotLightsAttribute.Type);
        Array<SpotLight> array = spots = sla == null ? null : sla.lights;
        if (this.dirLightsLoc >= 0) {
            for (i = 0; i < this.directionalLights.length; ++i) {
                if (dirs == null || i >= dirs.size) {
                    if (this.lightsSet && this.directionalLights[i].color.r == 0.0f && this.directionalLights[i].color.g == 0.0f && this.directionalLights[i].color.b == 0.0f) continue;
                    this.directionalLights[i].color.set(0.0f, 0.0f, 0.0f, 1.0f);
                } else {
                    if (this.lightsSet && this.directionalLights[i].equals(dirs.get(i))) continue;
                    this.directionalLights[i].set(dirs.get(i));
                }
                idx = this.dirLightsLoc + i * this.dirLightsSize;
                this.program.setUniformf(idx + this.dirLightsColorOffset, this.directionalLights[i].color.r, this.directionalLights[i].color.g, this.directionalLights[i].color.b);
                this.program.setUniformf(idx + this.dirLightsDirectionOffset, this.directionalLights[i].direction.x, this.directionalLights[i].direction.y, this.directionalLights[i].direction.z);
                if (this.dirLightsSize <= 0) break;
            }
        }
        if (this.pointLightsLoc >= 0) {
            for (i = 0; i < this.pointLights.length; ++i) {
                if (points == null || i >= points.size) {
                    if (this.lightsSet && this.pointLights[i].intensity == 0.0f) continue;
                    this.pointLights[i].intensity = 0.0f;
                } else {
                    if (this.lightsSet && this.pointLights[i].equals(points.get(i))) continue;
                    this.pointLights[i].set(points.get(i));
                }
                idx = this.pointLightsLoc + i * this.pointLightsSize;
                this.program.setUniformf(idx + this.pointLightsColorOffset, this.pointLights[i].color.r * this.pointLights[i].intensity, this.pointLights[i].color.g * this.pointLights[i].intensity, this.pointLights[i].color.b * this.pointLights[i].intensity);
                this.program.setUniformf(idx + this.pointLightsPositionOffset, this.pointLights[i].position.x, this.pointLights[i].position.y, this.pointLights[i].position.z);
                if (this.pointLightsIntensityOffset >= 0) {
                    this.program.setUniformf(idx + this.pointLightsIntensityOffset, this.pointLights[i].intensity);
                }
                if (this.pointLightsSize <= 0) break;
            }
        }
        if (this.spotLightsLoc >= 0) {
            for (i = 0; i < this.spotLights.length; ++i) {
                if (spots == null || i >= spots.size) {
                    if (this.lightsSet && this.spotLights[i].intensity == 0.0f) continue;
                    this.spotLights[i].intensity = 0.0f;
                } else {
                    if (this.lightsSet && this.spotLights[i].equals(spots.get(i))) continue;
                    this.spotLights[i].set(spots.get(i));
                }
                idx = this.spotLightsLoc + i * this.spotLightsSize;
                this.program.setUniformf(idx + this.spotLightsColorOffset, this.spotLights[i].color.r * this.spotLights[i].intensity, this.spotLights[i].color.g * this.spotLights[i].intensity, this.spotLights[i].color.b * this.spotLights[i].intensity);
                this.program.setUniformf(idx + this.spotLightsPositionOffset, this.spotLights[i].position);
                this.program.setUniformf(idx + this.spotLightsDirectionOffset, this.spotLights[i].direction);
                this.program.setUniformf(idx + this.spotLightsCutoffAngleOffset, this.spotLights[i].cutoffAngle);
                this.program.setUniformf(idx + this.spotLightsExponentOffset, this.spotLights[i].exponent);
                if (this.spotLightsIntensityOffset >= 0) {
                    this.program.setUniformf(idx + this.spotLightsIntensityOffset, this.spotLights[i].intensity);
                }
                if (this.spotLightsSize <= 0) break;
            }
        }
        if (attributes.has(ColorAttribute.Fog)) {
            this.set(this.u_fogColor, ((ColorAttribute)attributes.get((long)ColorAttribute.Fog)).color);
        }
        if (lights != null && lights.shadowMap != null) {
            this.set(this.u_shadowMapProjViewTrans, lights.shadowMap.getProjViewTrans());
            this.set(this.u_shadowTexture, lights.shadowMap.getDepthMap());
            this.set(this.u_shadowPCFOffset, 1.0f / (2.0f * (float)((GLTexture)lights.shadowMap.getDepthMap().texture).getWidth()));
        }
        this.lightsSet = true;
    }

    @Override
    public void dispose() {
        this.program.dispose();
        super.dispose();
    }

    public int getDefaultCullFace() {
        return this.config.defaultCullFace == -1 ? defaultCullFace : this.config.defaultCullFace;
    }

    public void setDefaultCullFace(int cullFace) {
        this.config.defaultCullFace = cullFace;
    }

    public int getDefaultDepthFunc() {
        return this.config.defaultDepthFunc == -1 ? defaultDepthFunc : this.config.defaultDepthFunc;
    }

    public void setDefaultDepthFunc(int depthFunc) {
        this.config.defaultDepthFunc = depthFunc;
    }

    public static class Setters {
        public static final BaseShader.Setter projTrans = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.projection);
            }
        };
        public static final BaseShader.Setter viewTrans = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.view);
            }
        };
        public static final BaseShader.Setter projViewTrans = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.combined);
            }
        };
        public static final BaseShader.Setter cameraPosition = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.position.x, shader.camera.position.y, shader.camera.position.z, 1.1881f / (shader.camera.far * shader.camera.far));
            }
        };
        public static final BaseShader.Setter cameraDirection = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.direction);
            }
        };
        public static final BaseShader.Setter cameraUp = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.up);
            }
        };
        public static final BaseShader.Setter cameraNearFar = new BaseShader.GlobalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.near, shader.camera.far);
            }
        };
        public static final BaseShader.Setter worldTrans = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, renderable.worldTransform);
            }
        };
        public static final BaseShader.Setter viewWorldTrans = new BaseShader.LocalSetter(){
            final Matrix4 temp = new Matrix4();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, this.temp.set(shader.camera.view).mul(renderable.worldTransform));
            }
        };
        public static final BaseShader.Setter projViewWorldTrans = new BaseShader.LocalSetter(){
            final Matrix4 temp = new Matrix4();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, this.temp.set(shader.camera.combined).mul(renderable.worldTransform));
            }
        };
        public static final BaseShader.Setter normalMatrix = new BaseShader.LocalSetter(){
            private final Matrix3 tmpM = new Matrix3();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, this.tmpM.set(renderable.worldTransform).inv().transpose());
            }
        };
        public static final BaseShader.Setter shininess = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((FloatAttribute)combinedAttributes.get((long)FloatAttribute.Shininess)).value);
            }
        };
        public static final BaseShader.Setter diffuseColor = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute)combinedAttributes.get((long)ColorAttribute.Diffuse)).color);
            }
        };
        public static final BaseShader.Setter diffuseTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Diffuse)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter diffuseUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Diffuse);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter specularColor = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute)combinedAttributes.get((long)ColorAttribute.Specular)).color);
            }
        };
        public static final BaseShader.Setter specularTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Specular)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter specularUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Specular);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter emissiveColor = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute)combinedAttributes.get((long)ColorAttribute.Emissive)).color);
            }
        };
        public static final BaseShader.Setter emissiveTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Emissive)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter emissiveUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Emissive);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter reflectionColor = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute)combinedAttributes.get((long)ColorAttribute.Reflection)).color);
            }
        };
        public static final BaseShader.Setter reflectionTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Reflection)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter reflectionUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Reflection);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter normalTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Normal)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter normalUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Normal);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter ambientTexture = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                int unit = shader.context.textureBinder.bind(((TextureAttribute)combinedAttributes.get((long)TextureAttribute.Ambient)).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public static final BaseShader.Setter ambientUVTransform = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                TextureAttribute ta = (TextureAttribute)combinedAttributes.get(TextureAttribute.Ambient);
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public static final BaseShader.Setter environmentCubemap = new BaseShader.LocalSetter(){

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(CubemapAttribute.EnvironmentMap)) {
                    shader.set(inputID, shader.context.textureBinder.bind(((CubemapAttribute)combinedAttributes.get((long)CubemapAttribute.EnvironmentMap)).textureDescription));
                }
            }
        };

        public static class ACubemap
        extends BaseShader.LocalSetter {
            private static final float[] ones = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
            private final AmbientCubemap cacheAmbientCubemap = new AmbientCubemap();
            private static final Vector3 tmpV1 = new Vector3();
            public final int dirLightsOffset;
            public final int pointLightsOffset;

            public ACubemap(int dirLightsOffset, int pointLightsOffset) {
                this.dirLightsOffset = dirLightsOffset;
                this.pointLightsOffset = pointLightsOffset;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                if (renderable.environment == null) {
                    shader.program.setUniform3fv(shader.loc(inputID), ones, 0, ones.length);
                } else {
                    int i;
                    Array<BaseLight> lights;
                    renderable.worldTransform.getTranslation(tmpV1);
                    if (combinedAttributes.has(ColorAttribute.AmbientLight)) {
                        this.cacheAmbientCubemap.set(((ColorAttribute)combinedAttributes.get((long)ColorAttribute.AmbientLight)).color);
                    }
                    if (combinedAttributes.has(DirectionalLightsAttribute.Type)) {
                        lights = ((DirectionalLightsAttribute)combinedAttributes.get((long)DirectionalLightsAttribute.Type)).lights;
                        for (i = this.dirLightsOffset; i < lights.size; ++i) {
                            this.cacheAmbientCubemap.add(((DirectionalLight)lights.get((int)i)).color, ((DirectionalLight)lights.get((int)i)).direction);
                        }
                    }
                    if (combinedAttributes.has(PointLightsAttribute.Type)) {
                        lights = ((PointLightsAttribute)combinedAttributes.get((long)PointLightsAttribute.Type)).lights;
                        for (i = this.pointLightsOffset; i < lights.size; ++i) {
                            this.cacheAmbientCubemap.add(((PointLight)lights.get((int)i)).color, ((PointLight)lights.get((int)i)).position, tmpV1, ((PointLight)lights.get((int)i)).intensity);
                        }
                    }
                    this.cacheAmbientCubemap.clamp();
                    shader.program.setUniform3fv(shader.loc(inputID), this.cacheAmbientCubemap.data, 0, this.cacheAmbientCubemap.data.length);
                }
            }
        }

        public static class Bones
        extends BaseShader.LocalSetter {
            private static final Matrix4 idtMatrix = new Matrix4();
            public final float[] bones;

            public Bones(int numBones) {
                this.bones = new float[numBones * 16];
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                for (int i = 0; i < this.bones.length; ++i) {
                    int idx = i / 16;
                    this.bones[i] = renderable.bones == null || idx >= renderable.bones.length || renderable.bones[idx] == null ? Bones.idtMatrix.val[i % 16] : renderable.bones[idx].val[i % 16];
                }
                shader.program.setUniformMatrix4fv(shader.loc(inputID), this.bones, 0, this.bones.length);
            }
        }
    }

    public static class Inputs {
        public static final BaseShader.Uniform projTrans = new BaseShader.Uniform("u_projTrans");
        public static final BaseShader.Uniform viewTrans = new BaseShader.Uniform("u_viewTrans");
        public static final BaseShader.Uniform projViewTrans = new BaseShader.Uniform("u_projViewTrans");
        public static final BaseShader.Uniform cameraPosition = new BaseShader.Uniform("u_cameraPosition");
        public static final BaseShader.Uniform cameraDirection = new BaseShader.Uniform("u_cameraDirection");
        public static final BaseShader.Uniform cameraUp = new BaseShader.Uniform("u_cameraUp");
        public static final BaseShader.Uniform cameraNearFar = new BaseShader.Uniform("u_cameraNearFar");
        public static final BaseShader.Uniform worldTrans = new BaseShader.Uniform("u_worldTrans");
        public static final BaseShader.Uniform viewWorldTrans = new BaseShader.Uniform("u_viewWorldTrans");
        public static final BaseShader.Uniform projViewWorldTrans = new BaseShader.Uniform("u_projViewWorldTrans");
        public static final BaseShader.Uniform normalMatrix = new BaseShader.Uniform("u_normalMatrix");
        public static final BaseShader.Uniform bones = new BaseShader.Uniform("u_bones");
        public static final BaseShader.Uniform shininess = new BaseShader.Uniform("u_shininess", FloatAttribute.Shininess);
        public static final BaseShader.Uniform opacity = new BaseShader.Uniform("u_opacity", BlendingAttribute.Type);
        public static final BaseShader.Uniform diffuseColor = new BaseShader.Uniform("u_diffuseColor", ColorAttribute.Diffuse);
        public static final BaseShader.Uniform diffuseTexture = new BaseShader.Uniform("u_diffuseTexture", TextureAttribute.Diffuse);
        public static final BaseShader.Uniform diffuseUVTransform = new BaseShader.Uniform("u_diffuseUVTransform", TextureAttribute.Diffuse);
        public static final BaseShader.Uniform specularColor = new BaseShader.Uniform("u_specularColor", ColorAttribute.Specular);
        public static final BaseShader.Uniform specularTexture = new BaseShader.Uniform("u_specularTexture", TextureAttribute.Specular);
        public static final BaseShader.Uniform specularUVTransform = new BaseShader.Uniform("u_specularUVTransform", TextureAttribute.Specular);
        public static final BaseShader.Uniform emissiveColor = new BaseShader.Uniform("u_emissiveColor", ColorAttribute.Emissive);
        public static final BaseShader.Uniform emissiveTexture = new BaseShader.Uniform("u_emissiveTexture", TextureAttribute.Emissive);
        public static final BaseShader.Uniform emissiveUVTransform = new BaseShader.Uniform("u_emissiveUVTransform", TextureAttribute.Emissive);
        public static final BaseShader.Uniform reflectionColor = new BaseShader.Uniform("u_reflectionColor", ColorAttribute.Reflection);
        public static final BaseShader.Uniform reflectionTexture = new BaseShader.Uniform("u_reflectionTexture", TextureAttribute.Reflection);
        public static final BaseShader.Uniform reflectionUVTransform = new BaseShader.Uniform("u_reflectionUVTransform", TextureAttribute.Reflection);
        public static final BaseShader.Uniform normalTexture = new BaseShader.Uniform("u_normalTexture", TextureAttribute.Normal);
        public static final BaseShader.Uniform normalUVTransform = new BaseShader.Uniform("u_normalUVTransform", TextureAttribute.Normal);
        public static final BaseShader.Uniform ambientTexture = new BaseShader.Uniform("u_ambientTexture", TextureAttribute.Ambient);
        public static final BaseShader.Uniform ambientUVTransform = new BaseShader.Uniform("u_ambientUVTransform", TextureAttribute.Ambient);
        public static final BaseShader.Uniform alphaTest = new BaseShader.Uniform("u_alphaTest");
        public static final BaseShader.Uniform ambientCube = new BaseShader.Uniform("u_ambientCubemap");
        public static final BaseShader.Uniform dirLights = new BaseShader.Uniform("u_dirLights");
        public static final BaseShader.Uniform pointLights = new BaseShader.Uniform("u_pointLights");
        public static final BaseShader.Uniform spotLights = new BaseShader.Uniform("u_spotLights");
        public static final BaseShader.Uniform environmentCubemap = new BaseShader.Uniform("u_environmentCubemap");
    }

    public static class Config {
        public String vertexShader = null;
        public String fragmentShader = null;
        public int numDirectionalLights = 2;
        public int numPointLights = 5;
        public int numSpotLights = 0;
        public int numBones = 12;
        public boolean ignoreUnimplemented = true;
        public int defaultCullFace = -1;
        public int defaultDepthFunc = -1;

        public Config() {
        }

        public Config(String vertexShader, String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }
    }
}

