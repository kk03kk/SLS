/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderHelper {
    private static ShaderProgram gsShader;
    private static ShaderProgram rsShader;
    private static ShaderProgram wsShader;
    private static ShaderProgram blurShader;
    private static ShaderProgram waterShader;
    private static ShaderProgram outlineShader;

    public static void initializeShaders() {
        ShaderProgram.pedantic = false;
        gsShader = new ShaderProgram(Gdx.files.internal("shaders/grayscale/vertexShader.vs").readString(), Gdx.files.internal("shaders/grayscale/fragShader.fs").readString());
    }

    public static void setShader(SpriteBatch sb, Shader shader) {
        switch (shader) {
            case BLUR: {
                sb.end();
                sb.setShader(blurShader);
                sb.begin();
                break;
            }
            case DEFAULT: {
                sb.end();
                sb.setShader(null);
                sb.begin();
                break;
            }
            case GRAYSCALE: {
                sb.end();
                sb.setShader(gsShader);
                sb.begin();
                break;
            }
            case OUTLINE: {
                sb.end();
                sb.setShader(outlineShader);
                sb.begin();
                break;
            }
            case RED_SILHOUETTE: {
                sb.end();
                sb.setShader(rsShader);
                sb.begin();
                break;
            }
            case WATER: {
                sb.end();
                sb.setShader(waterShader);
                sb.begin();
                break;
            }
            case WHITE_SILHOUETTE: {
                sb.end();
                sb.setShader(wsShader);
                sb.begin();
                break;
            }
            default: {
                sb.end();
                sb.setShader(null);
                sb.begin();
            }
        }
    }

    public static void setShader(PolygonSpriteBatch sb, Shader shader) {
        switch (shader) {
            case BLUR: {
                sb.setShader(blurShader);
                break;
            }
            case DEFAULT: {
                sb.setShader(null);
                break;
            }
            case GRAYSCALE: {
                sb.setShader(gsShader);
                break;
            }
            default: {
                sb.setShader(null);
            }
        }
    }

    public static enum Shader {
        BLUR,
        DEFAULT,
        GRAYSCALE,
        RED_SILHOUETTE,
        WHITE_SILHOUETTE,
        OUTLINE,
        WATER;

    }
}

