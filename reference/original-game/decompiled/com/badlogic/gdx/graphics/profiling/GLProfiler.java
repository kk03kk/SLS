/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.profiling;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.profiling.GL20Profiler;
import com.badlogic.gdx.graphics.profiling.GL30Profiler;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;
import com.badlogic.gdx.math.FloatCounter;

public abstract class GLProfiler {
    public static int calls;
    public static int textureBindings;
    public static int drawCalls;
    public static int shaderSwitches;
    public static final FloatCounter vertexCount;
    public static GLErrorListener listener;

    public static String resolveErrorNumber(int error) {
        switch (error) {
            case 1281: {
                return "GL_INVALID_VALUE";
            }
            case 1282: {
                return "GL_INVALID_OPERATION";
            }
            case 1286: {
                return "GL_INVALID_FRAMEBUFFER_OPERATION";
            }
            case 1280: {
                return "GL_INVALID_ENUM";
            }
            case 1285: {
                return "GL_OUT_OF_MEMORY";
            }
        }
        return "number " + error;
    }

    public static void enable() {
        if (!GLProfiler.isEnabled()) {
            Gdx.gl30 = Gdx.gl30 == null ? null : new GL30Profiler(Gdx.gl30);
            Gdx.gl = Gdx.gl20 = Gdx.gl30 != null ? Gdx.gl30 : new GL20Profiler(Gdx.gl20);
        }
    }

    public static void disable() {
        if (Gdx.gl30 != null && Gdx.gl30 instanceof GL30Profiler) {
            Gdx.gl30 = ((GL30Profiler)Gdx.gl30).gl30;
        }
        if (Gdx.gl20 != null && Gdx.gl20 instanceof GL20Profiler) {
            Gdx.gl20 = ((GL20Profiler)Gdx.gl).gl20;
        }
        if (Gdx.gl != null && Gdx.gl instanceof GL20Profiler) {
            Gdx.gl = ((GL20Profiler)Gdx.gl).gl20;
        }
    }

    public static boolean isEnabled() {
        return Gdx.gl30 instanceof GL30Profiler || Gdx.gl20 instanceof GL20Profiler;
    }

    public static void reset() {
        calls = 0;
        textureBindings = 0;
        drawCalls = 0;
        shaderSwitches = 0;
        vertexCount.reset();
    }

    static {
        vertexCount = new FloatCounter(0);
        listener = GLErrorListener.LOGGING_LISTENER;
    }
}

