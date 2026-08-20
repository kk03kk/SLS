/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.decals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.DecalMaterial;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

public class Decal {
    private static final int VERTEX_SIZE = 6;
    public static final int SIZE = 24;
    private static Vector3 tmp = new Vector3();
    private static Vector3 tmp2 = new Vector3();
    public int value;
    protected float[] vertices = new float[24];
    protected Vector3 position = new Vector3();
    protected Quaternion rotation = new Quaternion();
    protected Vector2 scale = new Vector2(1.0f, 1.0f);
    protected Color color = new Color();
    public Vector2 transformationOffset = null;
    protected Vector2 dimensions = new Vector2();
    protected DecalMaterial material;
    protected boolean updated = false;
    static final Vector3 dir = new Vector3();
    public static final int X1 = 0;
    public static final int Y1 = 1;
    public static final int Z1 = 2;
    public static final int C1 = 3;
    public static final int U1 = 4;
    public static final int V1 = 5;
    public static final int X2 = 6;
    public static final int Y2 = 7;
    public static final int Z2 = 8;
    public static final int C2 = 9;
    public static final int U2 = 10;
    public static final int V2 = 11;
    public static final int X3 = 12;
    public static final int Y3 = 13;
    public static final int Z3 = 14;
    public static final int C3 = 15;
    public static final int U3 = 16;
    public static final int V3 = 17;
    public static final int X4 = 18;
    public static final int Y4 = 19;
    public static final int Z4 = 20;
    public static final int C4 = 21;
    public static final int U4 = 22;
    public static final int V4 = 23;
    protected static Quaternion rotator = new Quaternion(0.0f, 0.0f, 0.0f, 0.0f);

    public Decal() {
        this.material = new DecalMaterial();
    }

    public Decal(DecalMaterial material) {
        this.material = material;
    }

    public void setColor(float r, float g, float b, float a) {
        float color;
        this.color.set(r, g, b, a);
        int intBits = (int)(255.0f * a) << 24 | (int)(255.0f * b) << 16 | (int)(255.0f * g) << 8 | (int)(255.0f * r);
        this.vertices[3] = color = NumberUtils.intToFloatColor(intBits);
        this.vertices[9] = color;
        this.vertices[15] = color;
        this.vertices[21] = color;
    }

    public void setColor(Color tint) {
        float color;
        this.color.set(tint);
        this.vertices[3] = color = tint.toFloatBits();
        this.vertices[9] = color;
        this.vertices[15] = color;
        this.vertices[21] = color;
    }

    public void setColor(float color) {
        this.color.set(NumberUtils.floatToIntColor(color));
        this.vertices[3] = color;
        this.vertices[9] = color;
        this.vertices[15] = color;
        this.vertices[21] = color;
    }

    public void setRotationX(float angle) {
        this.rotation.set(Vector3.X, angle);
        this.updated = false;
    }

    public void setRotationY(float angle) {
        this.rotation.set(Vector3.Y, angle);
        this.updated = false;
    }

    public void setRotationZ(float angle) {
        this.rotation.set(Vector3.Z, angle);
        this.updated = false;
    }

    public void rotateX(float angle) {
        rotator.set(Vector3.X, angle);
        this.rotation.mul(rotator);
        this.updated = false;
    }

    public void rotateY(float angle) {
        rotator.set(Vector3.Y, angle);
        this.rotation.mul(rotator);
        this.updated = false;
    }

    public void rotateZ(float angle) {
        rotator.set(Vector3.Z, angle);
        this.rotation.mul(rotator);
        this.updated = false;
    }

    public void setRotation(float yaw, float pitch, float roll) {
        this.rotation.setEulerAngles(yaw, pitch, roll);
        this.updated = false;
    }

    public void setRotation(Vector3 dir, Vector3 up) {
        tmp.set(up).crs(dir).nor();
        tmp2.set(dir).crs(tmp).nor();
        this.rotation.setFromAxes(Decal.tmp.x, Decal.tmp2.x, dir.x, Decal.tmp.y, Decal.tmp2.y, dir.y, Decal.tmp.z, Decal.tmp2.z, dir.z);
        this.updated = false;
    }

    public void setRotation(Quaternion q) {
        this.rotation.set(q);
        this.updated = false;
    }

    public Quaternion getRotation() {
        return this.rotation;
    }

    public void translateX(float units) {
        this.position.x += units;
        this.updated = false;
    }

    public void setX(float x) {
        this.position.x = x;
        this.updated = false;
    }

    public float getX() {
        return this.position.x;
    }

    public void translateY(float units) {
        this.position.y += units;
        this.updated = false;
    }

    public void setY(float y) {
        this.position.y = y;
        this.updated = false;
    }

    public float getY() {
        return this.position.y;
    }

    public void translateZ(float units) {
        this.position.z += units;
        this.updated = false;
    }

    public void setZ(float z) {
        this.position.z = z;
        this.updated = false;
    }

    public float getZ() {
        return this.position.z;
    }

    public void translate(float x, float y, float z) {
        this.position.add(x, y, z);
        this.updated = false;
    }

    public void translate(Vector3 trans) {
        this.position.add(trans);
        this.updated = false;
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.updated = false;
    }

    public void setPosition(Vector3 pos) {
        this.position.set(pos);
        this.updated = false;
    }

    public Color getColor() {
        return this.color;
    }

    public Vector3 getPosition() {
        return this.position;
    }

    public void setScaleX(float scale) {
        this.scale.x = scale;
        this.updated = false;
    }

    public float getScaleX() {
        return this.scale.x;
    }

    public void setScaleY(float scale) {
        this.scale.y = scale;
        this.updated = false;
    }

    public float getScaleY() {
        return this.scale.y;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scale.set(scaleX, scaleY);
        this.updated = false;
    }

    public void setScale(float scale) {
        this.scale.set(scale, scale);
        this.updated = false;
    }

    public void setWidth(float width) {
        this.dimensions.x = width;
        this.updated = false;
    }

    public float getWidth() {
        return this.dimensions.x;
    }

    public void setHeight(float height) {
        this.dimensions.y = height;
        this.updated = false;
    }

    public float getHeight() {
        return this.dimensions.y;
    }

    public void setDimensions(float width, float height) {
        this.dimensions.set(width, height);
        this.updated = false;
    }

    public float[] getVertices() {
        return this.vertices;
    }

    protected void update() {
        if (!this.updated) {
            this.resetVertices();
            this.transformVertices();
        }
    }

    protected void transformVertices() {
        float ty;
        float tx;
        if (this.transformationOffset != null) {
            tx = -this.transformationOffset.x;
            ty = -this.transformationOffset.y;
        } else {
            ty = 0.0f;
            tx = 0.0f;
        }
        float x = (this.vertices[0] + tx) * this.scale.x;
        float y = (this.vertices[1] + ty) * this.scale.y;
        float z = this.vertices[2];
        this.vertices[0] = this.rotation.w * x + this.rotation.y * z - this.rotation.z * y;
        this.vertices[1] = this.rotation.w * y + this.rotation.z * x - this.rotation.x * z;
        this.vertices[2] = this.rotation.w * z + this.rotation.x * y - this.rotation.y * x;
        float w = -this.rotation.x * x - this.rotation.y * y - this.rotation.z * z;
        this.rotation.conjugate();
        x = this.vertices[0];
        y = this.vertices[1];
        z = this.vertices[2];
        this.vertices[0] = w * this.rotation.x + x * this.rotation.w + y * this.rotation.z - z * this.rotation.y;
        this.vertices[1] = w * this.rotation.y + y * this.rotation.w + z * this.rotation.x - x * this.rotation.z;
        this.vertices[2] = w * this.rotation.z + z * this.rotation.w + x * this.rotation.y - y * this.rotation.x;
        this.rotation.conjugate();
        this.vertices[0] = this.vertices[0] + (this.position.x - tx);
        this.vertices[1] = this.vertices[1] + (this.position.y - ty);
        this.vertices[2] = this.vertices[2] + this.position.z;
        x = (this.vertices[6] + tx) * this.scale.x;
        y = (this.vertices[7] + ty) * this.scale.y;
        z = this.vertices[8];
        this.vertices[6] = this.rotation.w * x + this.rotation.y * z - this.rotation.z * y;
        this.vertices[7] = this.rotation.w * y + this.rotation.z * x - this.rotation.x * z;
        this.vertices[8] = this.rotation.w * z + this.rotation.x * y - this.rotation.y * x;
        w = -this.rotation.x * x - this.rotation.y * y - this.rotation.z * z;
        this.rotation.conjugate();
        x = this.vertices[6];
        y = this.vertices[7];
        z = this.vertices[8];
        this.vertices[6] = w * this.rotation.x + x * this.rotation.w + y * this.rotation.z - z * this.rotation.y;
        this.vertices[7] = w * this.rotation.y + y * this.rotation.w + z * this.rotation.x - x * this.rotation.z;
        this.vertices[8] = w * this.rotation.z + z * this.rotation.w + x * this.rotation.y - y * this.rotation.x;
        this.rotation.conjugate();
        this.vertices[6] = this.vertices[6] + (this.position.x - tx);
        this.vertices[7] = this.vertices[7] + (this.position.y - ty);
        this.vertices[8] = this.vertices[8] + this.position.z;
        x = (this.vertices[12] + tx) * this.scale.x;
        y = (this.vertices[13] + ty) * this.scale.y;
        z = this.vertices[14];
        this.vertices[12] = this.rotation.w * x + this.rotation.y * z - this.rotation.z * y;
        this.vertices[13] = this.rotation.w * y + this.rotation.z * x - this.rotation.x * z;
        this.vertices[14] = this.rotation.w * z + this.rotation.x * y - this.rotation.y * x;
        w = -this.rotation.x * x - this.rotation.y * y - this.rotation.z * z;
        this.rotation.conjugate();
        x = this.vertices[12];
        y = this.vertices[13];
        z = this.vertices[14];
        this.vertices[12] = w * this.rotation.x + x * this.rotation.w + y * this.rotation.z - z * this.rotation.y;
        this.vertices[13] = w * this.rotation.y + y * this.rotation.w + z * this.rotation.x - x * this.rotation.z;
        this.vertices[14] = w * this.rotation.z + z * this.rotation.w + x * this.rotation.y - y * this.rotation.x;
        this.rotation.conjugate();
        this.vertices[12] = this.vertices[12] + (this.position.x - tx);
        this.vertices[13] = this.vertices[13] + (this.position.y - ty);
        this.vertices[14] = this.vertices[14] + this.position.z;
        x = (this.vertices[18] + tx) * this.scale.x;
        y = (this.vertices[19] + ty) * this.scale.y;
        z = this.vertices[20];
        this.vertices[18] = this.rotation.w * x + this.rotation.y * z - this.rotation.z * y;
        this.vertices[19] = this.rotation.w * y + this.rotation.z * x - this.rotation.x * z;
        this.vertices[20] = this.rotation.w * z + this.rotation.x * y - this.rotation.y * x;
        w = -this.rotation.x * x - this.rotation.y * y - this.rotation.z * z;
        this.rotation.conjugate();
        x = this.vertices[18];
        y = this.vertices[19];
        z = this.vertices[20];
        this.vertices[18] = w * this.rotation.x + x * this.rotation.w + y * this.rotation.z - z * this.rotation.y;
        this.vertices[19] = w * this.rotation.y + y * this.rotation.w + z * this.rotation.x - x * this.rotation.z;
        this.vertices[20] = w * this.rotation.z + z * this.rotation.w + x * this.rotation.y - y * this.rotation.x;
        this.rotation.conjugate();
        this.vertices[18] = this.vertices[18] + (this.position.x - tx);
        this.vertices[19] = this.vertices[19] + (this.position.y - ty);
        this.vertices[20] = this.vertices[20] + this.position.z;
        this.updated = true;
    }

    protected void resetVertices() {
        float left = -this.dimensions.x / 2.0f;
        float right = left + this.dimensions.x;
        float top = this.dimensions.y / 2.0f;
        float bottom = top - this.dimensions.y;
        this.vertices[0] = left;
        this.vertices[1] = top;
        this.vertices[2] = 0.0f;
        this.vertices[6] = right;
        this.vertices[7] = top;
        this.vertices[8] = 0.0f;
        this.vertices[12] = left;
        this.vertices[13] = bottom;
        this.vertices[14] = 0.0f;
        this.vertices[18] = right;
        this.vertices[19] = bottom;
        this.vertices[20] = 0.0f;
        this.updated = false;
    }

    protected void updateUVs() {
        TextureRegion tr = this.material.textureRegion;
        this.vertices[4] = tr.getU();
        this.vertices[5] = tr.getV();
        this.vertices[10] = tr.getU2();
        this.vertices[11] = tr.getV();
        this.vertices[16] = tr.getU();
        this.vertices[17] = tr.getV2();
        this.vertices[22] = tr.getU2();
        this.vertices[23] = tr.getV2();
    }

    public void setTextureRegion(TextureRegion textureRegion) {
        this.material.textureRegion = textureRegion;
        this.updateUVs();
    }

    public TextureRegion getTextureRegion() {
        return this.material.textureRegion;
    }

    public void setBlending(int srcBlendFactor, int dstBlendFactor) {
        this.material.srcBlendFactor = srcBlendFactor;
        this.material.dstBlendFactor = dstBlendFactor;
    }

    public DecalMaterial getMaterial() {
        return this.material;
    }

    public void setMaterial(DecalMaterial material) {
        this.material = material;
    }

    public void lookAt(Vector3 position, Vector3 up) {
        dir.set(position).sub(this.position).nor();
        this.setRotation(dir, up);
    }

    public static Decal newDecal(TextureRegion textureRegion) {
        return Decal.newDecal(textureRegion.getRegionWidth(), textureRegion.getRegionHeight(), textureRegion, -1, -1);
    }

    public static Decal newDecal(TextureRegion textureRegion, boolean hasTransparency) {
        return Decal.newDecal(textureRegion.getRegionWidth(), textureRegion.getRegionHeight(), textureRegion, hasTransparency ? 770 : -1, hasTransparency ? 771 : -1);
    }

    public static Decal newDecal(float width, float height, TextureRegion textureRegion) {
        return Decal.newDecal(width, height, textureRegion, -1, -1);
    }

    public static Decal newDecal(float width, float height, TextureRegion textureRegion, boolean hasTransparency) {
        return Decal.newDecal(width, height, textureRegion, hasTransparency ? 770 : -1, hasTransparency ? 771 : -1);
    }

    public static Decal newDecal(float width, float height, TextureRegion textureRegion, int srcBlendFactor, int dstBlendFactor) {
        Decal decal = new Decal();
        decal.setTextureRegion(textureRegion);
        decal.setBlending(srcBlendFactor, dstBlendFactor);
        decal.dimensions.x = width;
        decal.dimensions.y = height;
        decal.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        return decal;
    }

    public static Decal newDecal(float width, float height, TextureRegion textureRegion, int srcBlendFactor, int dstBlendFactor, DecalMaterial material) {
        Decal decal = new Decal(material);
        decal.setTextureRegion(textureRegion);
        decal.setBlending(srcBlendFactor, dstBlendFactor);
        decal.dimensions.x = width;
        decal.dimensions.y = height;
        decal.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        return decal;
    }
}

