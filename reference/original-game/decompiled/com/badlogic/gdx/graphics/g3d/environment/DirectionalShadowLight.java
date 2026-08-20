/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.ShadowMap;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public class DirectionalShadowLight
extends DirectionalLight
implements ShadowMap,
Disposable {
    protected FrameBuffer fbo;
    protected Camera cam;
    protected float halfDepth;
    protected float halfHeight;
    protected final Vector3 tmpV = new Vector3();
    protected final TextureDescriptor textureDesc;

    public DirectionalShadowLight(int shadowMapWidth, int shadowMapHeight, float shadowViewportWidth, float shadowViewportHeight, float shadowNear, float shadowFar) {
        this.fbo = new FrameBuffer(Pixmap.Format.RGBA8888, shadowMapWidth, shadowMapHeight, true);
        this.cam = new OrthographicCamera(shadowViewportWidth, shadowViewportHeight);
        this.cam.near = shadowNear;
        this.cam.far = shadowFar;
        this.halfHeight = shadowViewportHeight * 0.5f;
        this.halfDepth = shadowNear + 0.5f * (shadowFar - shadowNear);
        this.textureDesc = new TextureDescriptor();
        this.textureDesc.minFilter = this.textureDesc.magFilter = Texture.TextureFilter.Nearest;
        this.textureDesc.uWrap = this.textureDesc.vWrap = Texture.TextureWrap.ClampToEdge;
    }

    public void update(Camera camera) {
        this.update(this.tmpV.set(camera.direction).scl(this.halfHeight), camera.direction);
    }

    public void update(Vector3 center, Vector3 forward) {
        this.cam.position.set(this.direction).scl(-this.halfDepth).add(center);
        this.cam.direction.set(this.direction).nor();
        this.cam.normalizeUp();
        this.cam.update();
    }

    public void begin(Camera camera) {
        this.update(camera);
        this.begin();
    }

    public void begin(Vector3 center, Vector3 forward) {
        this.update(center, forward);
        this.begin();
    }

    public void begin() {
        int w = this.fbo.getWidth();
        int h = this.fbo.getHeight();
        this.fbo.begin();
        Gdx.gl.glViewport(0, 0, w, h);
        Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        Gdx.gl.glClear(16640);
        Gdx.gl.glEnable(3089);
        Gdx.gl.glScissor(1, 1, w - 2, h - 2);
    }

    public void end() {
        Gdx.gl.glDisable(3089);
        this.fbo.end();
    }

    public FrameBuffer getFrameBuffer() {
        return this.fbo;
    }

    public Camera getCamera() {
        return this.cam;
    }

    @Override
    public Matrix4 getProjViewTrans() {
        return this.cam.combined;
    }

    @Override
    public TextureDescriptor getDepthMap() {
        this.textureDesc.texture = this.fbo.getColorBufferTexture();
        return this.textureDesc;
    }

    @Override
    public void dispose() {
        if (this.fbo != null) {
            this.fbo.dispose();
        }
        this.fbo = null;
    }
}

