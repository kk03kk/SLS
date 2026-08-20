/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class MipMapGenerator {
    private static boolean useHWMipMap = true;

    private MipMapGenerator() {
    }

    public static void setUseHardwareMipMap(boolean useHWMipMap) {
        MipMapGenerator.useHWMipMap = useHWMipMap;
    }

    public static void generateMipMap(Pixmap pixmap, int textureWidth, int textureHeight) {
        MipMapGenerator.generateMipMap(3553, pixmap, textureWidth, textureHeight);
    }

    public static void generateMipMap(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
        if (!useHWMipMap) {
            MipMapGenerator.generateMipMapCPU(target, pixmap, textureWidth, textureHeight);
            return;
        }
        if (Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.WebGL || Gdx.app.getType() == Application.ApplicationType.iOS) {
            MipMapGenerator.generateMipMapGLES20(target, pixmap);
        } else {
            MipMapGenerator.generateMipMapDesktop(target, pixmap, textureWidth, textureHeight);
        }
    }

    private static void generateMipMapGLES20(int target, Pixmap pixmap) {
        Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
        Gdx.gl20.glGenerateMipmap(target);
    }

    private static void generateMipMapDesktop(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
        if (Gdx.graphics.supportsExtension("GL_ARB_framebuffer_object") || Gdx.graphics.supportsExtension("GL_EXT_framebuffer_object") || Gdx.gl30 != null) {
            Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
            Gdx.gl20.glGenerateMipmap(target);
        } else {
            MipMapGenerator.generateMipMapCPU(target, pixmap, textureWidth, textureHeight);
        }
    }

    private static void generateMipMapCPU(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
        Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
        if (Gdx.gl20 == null && textureWidth != textureHeight) {
            throw new GdxRuntimeException("texture width and height must be square when using mipmapping.");
        }
        int width = pixmap.getWidth() / 2;
        int height = pixmap.getHeight() / 2;
        int level = 1;
        Pixmap.Blending blending = Pixmap.getBlending();
        Pixmap.setBlending(Pixmap.Blending.None);
        while (width > 0 && height > 0) {
            Pixmap tmp = new Pixmap(width, height, pixmap.getFormat());
            tmp.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(), 0, 0, width, height);
            if (level > 1) {
                pixmap.dispose();
            }
            pixmap = tmp;
            Gdx.gl.glTexImage2D(target, level, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
            width = pixmap.getWidth() / 2;
            height = pixmap.getHeight() / 2;
            ++level;
        }
        Pixmap.setBlending(blending);
    }
}

