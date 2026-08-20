/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

public class DistanceFieldFont
extends BitmapFont {
    private float distanceFieldSmoothing;

    public DistanceFieldFont(BitmapFont.BitmapFontData data, Array<TextureRegion> pageRegions, boolean integer) {
        super(data, pageRegions, integer);
    }

    public DistanceFieldFont(BitmapFont.BitmapFontData data, TextureRegion region, boolean integer) {
        super(data, region, integer);
    }

    public DistanceFieldFont(FileHandle fontFile, boolean flip) {
        super(fontFile, flip);
    }

    public DistanceFieldFont(FileHandle fontFile, FileHandle imageFile, boolean flip, boolean integer) {
        super(fontFile, imageFile, flip, integer);
    }

    public DistanceFieldFont(FileHandle fontFile, FileHandle imageFile, boolean flip) {
        super(fontFile, imageFile, flip);
    }

    public DistanceFieldFont(FileHandle fontFile, TextureRegion region, boolean flip) {
        super(fontFile, region, flip);
    }

    public DistanceFieldFont(FileHandle fontFile, TextureRegion region) {
        super(fontFile, region);
    }

    public DistanceFieldFont(FileHandle fontFile) {
        super(fontFile);
    }

    @Override
    protected void load(BitmapFont.BitmapFontData data) {
        super.load(data);
        Array<TextureRegion> regions = this.getRegions();
        for (TextureRegion region : regions) {
            region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    @Override
    public BitmapFontCache newFontCache() {
        return new DistanceFieldFontCache(this, this.integer);
    }

    public float getDistanceFieldSmoothing() {
        return this.distanceFieldSmoothing;
    }

    public void setDistanceFieldSmoothing(float distanceFieldSmoothing) {
        this.distanceFieldSmoothing = distanceFieldSmoothing;
    }

    public static ShaderProgram createDistanceFieldShader() {
        String vertexShader = "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\nuniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_texCoords;\n\nvoid main() {\n\tv_color = a_color;\n\tv_color.a = v_color.a * (255.0/254.0);\n\tv_texCoords = a_texCoord0;\n\tgl_Position =  u_projTrans * a_position;\n}\n";
        String fragmentShader = "#ifdef GL_ES\n\tprecision mediump float;\n\tprecision mediump int;\n#endif\n\nuniform sampler2D u_texture;\nuniform float u_smoothing;\nvarying vec4 v_color;\nvarying vec2 v_texCoords;\n\nvoid main() {\n\tif (u_smoothing > 0.0) {\n\t\tfloat smoothing = 0.25 / u_smoothing;\n\t\tfloat distance = texture2D(u_texture, v_texCoords).a;\n\t\tfloat alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n\t\tgl_FragColor = vec4(v_color.rgb, alpha * v_color.a);\n\t} else {\n\t\tgl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n\t}\n}\n";
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) {
            throw new IllegalArgumentException("Error compiling distance field shader: " + shader.getLog());
        }
        return shader;
    }

    private static class DistanceFieldFontCache
    extends BitmapFontCache {
        public DistanceFieldFontCache(DistanceFieldFont font) {
            super(font, font.usesIntegerPositions());
        }

        public DistanceFieldFontCache(DistanceFieldFont font, boolean integer) {
            super(font, integer);
        }

        private float getSmoothingFactor() {
            DistanceFieldFont font = (DistanceFieldFont)super.getFont();
            return font.getDistanceFieldSmoothing() * font.getScaleX();
        }

        private void setSmoothingUniform(Batch spriteBatch, float smoothing) {
            spriteBatch.flush();
            spriteBatch.getShader().setUniformf("u_smoothing", smoothing);
        }

        @Override
        public void draw(Batch spriteBatch) {
            this.setSmoothingUniform(spriteBatch, this.getSmoothingFactor());
            super.draw(spriteBatch);
            this.setSmoothingUniform(spriteBatch, 0.0f);
        }

        @Override
        public void draw(Batch spriteBatch, int start, int end) {
            this.setSmoothingUniform(spriteBatch, this.getSmoothingFactor());
            super.draw(spriteBatch, start, end);
            this.setSmoothingUniform(spriteBatch, 0.0f);
        }
    }
}

