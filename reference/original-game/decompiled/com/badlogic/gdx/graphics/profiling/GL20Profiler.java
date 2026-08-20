/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.profiling;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GL20Profiler
extends GLProfiler
implements GL20 {
    public final GL20 gl20;

    protected GL20Profiler(GL20 gl20) {
        this.gl20 = gl20;
    }

    private void check() {
        int error = this.gl20.glGetError();
        while (error != 0) {
            listener.onError(error);
            error = this.gl20.glGetError();
        }
    }

    @Override
    public void glActiveTexture(int texture) {
        ++calls;
        this.gl20.glActiveTexture(texture);
        this.check();
    }

    @Override
    public void glBindTexture(int target, int texture) {
        ++textureBindings;
        ++calls;
        this.gl20.glBindTexture(target, texture);
        this.check();
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        ++calls;
        this.gl20.glBlendFunc(sfactor, dfactor);
        this.check();
    }

    @Override
    public void glClear(int mask) {
        ++calls;
        this.gl20.glClear(mask);
        this.check();
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        ++calls;
        this.gl20.glClearColor(red, green, blue, alpha);
        this.check();
    }

    @Override
    public void glClearDepthf(float depth) {
        ++calls;
        this.gl20.glClearDepthf(depth);
        this.check();
    }

    @Override
    public void glClearStencil(int s) {
        ++calls;
        this.gl20.glClearStencil(s);
        this.check();
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        ++calls;
        this.gl20.glColorMask(red, green, blue, alpha);
        this.check();
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data) {
        ++calls;
        this.gl20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
        this.check();
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Buffer data) {
        ++calls;
        this.gl20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
        this.check();
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        ++calls;
        this.gl20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
        this.check();
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        ++calls;
        this.gl20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
        this.check();
    }

    @Override
    public void glCullFace(int mode) {
        ++calls;
        this.gl20.glCullFace(mode);
        this.check();
    }

    @Override
    public void glDeleteTextures(int n, IntBuffer textures) {
        ++calls;
        this.gl20.glDeleteTextures(n, textures);
        this.check();
    }

    @Override
    public void glDeleteTexture(int texture) {
        ++calls;
        this.gl20.glDeleteTexture(texture);
        this.check();
    }

    @Override
    public void glDepthFunc(int func) {
        ++calls;
        this.gl20.glDepthFunc(func);
        this.check();
    }

    @Override
    public void glDepthMask(boolean flag) {
        ++calls;
        this.gl20.glDepthMask(flag);
        this.check();
    }

    @Override
    public void glDepthRangef(float zNear, float zFar) {
        ++calls;
        this.gl20.glDepthRangef(zNear, zFar);
        this.check();
    }

    @Override
    public void glDisable(int cap) {
        ++calls;
        this.gl20.glDisable(cap);
        this.check();
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        vertexCount.put(count);
        ++drawCalls;
        ++calls;
        this.gl20.glDrawArrays(mode, first, count);
        this.check();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices) {
        vertexCount.put(count);
        ++drawCalls;
        ++calls;
        this.gl20.glDrawElements(mode, count, type, indices);
        this.check();
    }

    @Override
    public void glEnable(int cap) {
        ++calls;
        this.gl20.glEnable(cap);
        this.check();
    }

    @Override
    public void glFinish() {
        ++calls;
        this.gl20.glFinish();
        this.check();
    }

    @Override
    public void glFlush() {
        ++calls;
        this.gl20.glFlush();
        this.check();
    }

    @Override
    public void glFrontFace(int mode) {
        ++calls;
        this.gl20.glFrontFace(mode);
        this.check();
    }

    @Override
    public void glGenTextures(int n, IntBuffer textures) {
        ++calls;
        this.gl20.glGenTextures(n, textures);
        this.check();
    }

    @Override
    public int glGenTexture() {
        ++calls;
        int result = this.gl20.glGenTexture();
        this.check();
        return result;
    }

    @Override
    public int glGetError() {
        ++calls;
        return this.gl20.glGetError();
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetIntegerv(pname, params);
        this.check();
    }

    @Override
    public String glGetString(int name) {
        ++calls;
        String result = this.gl20.glGetString(name);
        this.check();
        return result;
    }

    @Override
    public void glHint(int target, int mode) {
        ++calls;
        this.gl20.glHint(target, mode);
        this.check();
    }

    @Override
    public void glLineWidth(float width) {
        ++calls;
        this.gl20.glLineWidth(width);
        this.check();
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        ++calls;
        this.gl20.glPixelStorei(pname, param);
        this.check();
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        ++calls;
        this.gl20.glPolygonOffset(factor, units);
        this.check();
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, Buffer pixels) {
        ++calls;
        this.gl20.glReadPixels(x, y, width, height, format, type, pixels);
        this.check();
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        ++calls;
        this.gl20.glScissor(x, y, width, height);
        this.check();
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        ++calls;
        this.gl20.glStencilFunc(func, ref, mask);
        this.check();
    }

    @Override
    public void glStencilMask(int mask) {
        ++calls;
        this.gl20.glStencilMask(mask);
        this.check();
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass) {
        ++calls;
        this.gl20.glStencilOp(fail, zfail, zpass);
        this.check();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        ++calls;
        this.gl20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
        this.check();
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        ++calls;
        this.gl20.glTexParameterf(target, pname, param);
        this.check();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        ++calls;
        this.gl20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
        this.check();
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        ++calls;
        this.gl20.glViewport(x, y, width, height);
        this.check();
    }

    @Override
    public void glAttachShader(int program, int shader) {
        ++calls;
        this.gl20.glAttachShader(program, shader);
        this.check();
    }

    @Override
    public void glBindAttribLocation(int program, int index, String name) {
        ++calls;
        this.gl20.glBindAttribLocation(program, index, name);
        this.check();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        ++calls;
        this.gl20.glBindBuffer(target, buffer);
        this.check();
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        ++calls;
        this.gl20.glBindFramebuffer(target, framebuffer);
        this.check();
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        ++calls;
        this.gl20.glBindRenderbuffer(target, renderbuffer);
        this.check();
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        ++calls;
        this.gl20.glBlendColor(red, green, blue, alpha);
        this.check();
    }

    @Override
    public void glBlendEquation(int mode) {
        ++calls;
        this.gl20.glBlendEquation(mode);
        this.check();
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        ++calls;
        this.gl20.glBlendEquationSeparate(modeRGB, modeAlpha);
        this.check();
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        ++calls;
        this.gl20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        this.check();
    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage) {
        ++calls;
        this.gl20.glBufferData(target, size, data, usage);
        this.check();
    }

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data) {
        ++calls;
        this.gl20.glBufferSubData(target, offset, size, data);
        this.check();
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        ++calls;
        int result = this.gl20.glCheckFramebufferStatus(target);
        this.check();
        return result;
    }

    @Override
    public void glCompileShader(int shader) {
        ++calls;
        this.gl20.glCompileShader(shader);
        this.check();
    }

    @Override
    public int glCreateProgram() {
        ++calls;
        int result = this.gl20.glCreateProgram();
        this.check();
        return result;
    }

    @Override
    public int glCreateShader(int type) {
        ++calls;
        int result = this.gl20.glCreateShader(type);
        this.check();
        return result;
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        ++calls;
        this.gl20.glDeleteBuffer(buffer);
        this.check();
    }

    @Override
    public void glDeleteBuffers(int n, IntBuffer buffers) {
        ++calls;
        this.gl20.glDeleteBuffers(n, buffers);
        this.check();
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        ++calls;
        this.gl20.glDeleteFramebuffer(framebuffer);
        this.check();
    }

    @Override
    public void glDeleteFramebuffers(int n, IntBuffer framebuffers) {
        ++calls;
        this.gl20.glDeleteFramebuffers(n, framebuffers);
        this.check();
    }

    @Override
    public void glDeleteProgram(int program) {
        ++calls;
        this.gl20.glDeleteProgram(program);
        this.check();
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        ++calls;
        this.gl20.glDeleteRenderbuffer(renderbuffer);
        this.check();
    }

    @Override
    public void glDeleteRenderbuffers(int n, IntBuffer renderbuffers) {
        ++calls;
        this.gl20.glDeleteRenderbuffers(n, renderbuffers);
        this.check();
    }

    @Override
    public void glDeleteShader(int shader) {
        ++calls;
        this.gl20.glDeleteShader(shader);
        this.check();
    }

    @Override
    public void glDetachShader(int program, int shader) {
        ++calls;
        this.gl20.glDetachShader(program, shader);
        this.check();
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        ++calls;
        this.gl20.glDisableVertexAttribArray(index);
        this.check();
    }

    @Override
    public void glDrawElements(int mode, int count, int type, int indices) {
        vertexCount.put(count);
        ++drawCalls;
        ++calls;
        this.gl20.glDrawElements(mode, count, type, indices);
        this.check();
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        ++calls;
        this.gl20.glEnableVertexAttribArray(index);
        this.check();
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        ++calls;
        this.gl20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        this.check();
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        ++calls;
        this.gl20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        this.check();
    }

    @Override
    public int glGenBuffer() {
        ++calls;
        int result = this.gl20.glGenBuffer();
        this.check();
        return result;
    }

    @Override
    public void glGenBuffers(int n, IntBuffer buffers) {
        ++calls;
        this.gl20.glGenBuffers(n, buffers);
        this.check();
    }

    @Override
    public void glGenerateMipmap(int target) {
        ++calls;
        this.gl20.glGenerateMipmap(target);
        this.check();
    }

    @Override
    public int glGenFramebuffer() {
        ++calls;
        int result = this.gl20.glGenFramebuffer();
        this.check();
        return result;
    }

    @Override
    public void glGenFramebuffers(int n, IntBuffer framebuffers) {
        ++calls;
        this.gl20.glGenFramebuffers(n, framebuffers);
        this.check();
    }

    @Override
    public int glGenRenderbuffer() {
        ++calls;
        int result = this.gl20.glGenRenderbuffer();
        this.check();
        return result;
    }

    @Override
    public void glGenRenderbuffers(int n, IntBuffer renderbuffers) {
        ++calls;
        this.gl20.glGenRenderbuffers(n, renderbuffers);
        this.check();
    }

    @Override
    public String glGetActiveAttrib(int program, int index, IntBuffer size, Buffer type) {
        ++calls;
        String result = this.gl20.glGetActiveAttrib(program, index, size, type);
        this.check();
        return result;
    }

    @Override
    public String glGetActiveUniform(int program, int index, IntBuffer size, Buffer type) {
        ++calls;
        String result = this.gl20.glGetActiveUniform(program, index, size, type);
        this.check();
        return result;
    }

    @Override
    public void glGetAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
        ++calls;
        this.gl20.glGetAttachedShaders(program, maxcount, count, shaders);
        this.check();
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        ++calls;
        int result = this.gl20.glGetAttribLocation(program, name);
        this.check();
        return result;
    }

    @Override
    public void glGetBooleanv(int pname, Buffer params) {
        ++calls;
        this.gl20.glGetBooleanv(pname, params);
        this.check();
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetBufferParameteriv(target, pname, params);
        this.check();
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params) {
        ++calls;
        this.gl20.glGetFloatv(pname, params);
        this.check();
    }

    @Override
    public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
        this.check();
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetProgramiv(program, pname, params);
        this.check();
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        ++calls;
        String result = this.gl20.glGetProgramInfoLog(program);
        this.check();
        return result;
    }

    @Override
    public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetRenderbufferParameteriv(target, pname, params);
        this.check();
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetShaderiv(shader, pname, params);
        this.check();
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        ++calls;
        String result = this.gl20.glGetShaderInfoLog(shader);
        this.check();
        return result;
    }

    @Override
    public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision) {
        ++calls;
        this.gl20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
        this.check();
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        ++calls;
        this.gl20.glGetTexParameterfv(target, pname, params);
        this.check();
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetTexParameteriv(target, pname, params);
        this.check();
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params) {
        ++calls;
        this.gl20.glGetUniformfv(program, location, params);
        this.check();
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params) {
        ++calls;
        this.gl20.glGetUniformiv(program, location, params);
        this.check();
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
        ++calls;
        int result = this.gl20.glGetUniformLocation(program, name);
        this.check();
        return result;
    }

    @Override
    public void glGetVertexAttribfv(int index, int pname, FloatBuffer params) {
        ++calls;
        this.gl20.glGetVertexAttribfv(index, pname, params);
        this.check();
    }

    @Override
    public void glGetVertexAttribiv(int index, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glGetVertexAttribiv(index, pname, params);
        this.check();
    }

    @Override
    public void glGetVertexAttribPointerv(int index, int pname, Buffer pointer) {
        ++calls;
        this.gl20.glGetVertexAttribPointerv(index, pname, pointer);
        this.check();
    }

    @Override
    public boolean glIsBuffer(int buffer) {
        ++calls;
        boolean result = this.gl20.glIsBuffer(buffer);
        this.check();
        return result;
    }

    @Override
    public boolean glIsEnabled(int cap) {
        ++calls;
        boolean result = this.gl20.glIsEnabled(cap);
        this.check();
        return result;
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer) {
        ++calls;
        boolean result = this.gl20.glIsFramebuffer(framebuffer);
        this.check();
        return result;
    }

    @Override
    public boolean glIsProgram(int program) {
        ++calls;
        boolean result = this.gl20.glIsProgram(program);
        this.check();
        return result;
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer) {
        ++calls;
        boolean result = this.gl20.glIsRenderbuffer(renderbuffer);
        this.check();
        return result;
    }

    @Override
    public boolean glIsShader(int shader) {
        ++calls;
        boolean result = this.gl20.glIsShader(shader);
        this.check();
        return result;
    }

    @Override
    public boolean glIsTexture(int texture) {
        ++calls;
        boolean result = this.gl20.glIsTexture(texture);
        this.check();
        return result;
    }

    @Override
    public void glLinkProgram(int program) {
        ++calls;
        this.gl20.glLinkProgram(program);
        this.check();
    }

    @Override
    public void glReleaseShaderCompiler() {
        ++calls;
        this.gl20.glReleaseShaderCompiler();
        this.check();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        ++calls;
        this.gl20.glRenderbufferStorage(target, internalformat, width, height);
        this.check();
    }

    @Override
    public void glSampleCoverage(float value, boolean invert) {
        ++calls;
        this.gl20.glSampleCoverage(value, invert);
        this.check();
    }

    @Override
    public void glShaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
        ++calls;
        this.gl20.glShaderBinary(n, shaders, binaryformat, binary, length);
        this.check();
    }

    @Override
    public void glShaderSource(int shader, String string) {
        ++calls;
        this.gl20.glShaderSource(shader, string);
        this.check();
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        ++calls;
        this.gl20.glStencilFuncSeparate(face, func, ref, mask);
        this.check();
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {
        ++calls;
        this.gl20.glStencilMaskSeparate(face, mask);
        this.check();
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
        ++calls;
        this.gl20.glStencilOpSeparate(face, fail, zfail, zpass);
        this.check();
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params) {
        ++calls;
        this.gl20.glTexParameterfv(target, pname, params);
        this.check();
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        ++calls;
        this.gl20.glTexParameteri(target, pname, param);
        this.check();
    }

    @Override
    public void glTexParameteriv(int target, int pname, IntBuffer params) {
        ++calls;
        this.gl20.glTexParameteriv(target, pname, params);
        this.check();
    }

    @Override
    public void glUniform1f(int location, float x) {
        ++calls;
        this.gl20.glUniform1f(location, x);
        this.check();
    }

    @Override
    public void glUniform1fv(int location, int count, FloatBuffer v) {
        ++calls;
        this.gl20.glUniform1fv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform1fv(int location, int count, float[] v, int offset) {
        ++calls;
        this.gl20.glUniform1fv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform1i(int location, int x) {
        ++calls;
        this.gl20.glUniform1i(location, x);
        this.check();
    }

    @Override
    public void glUniform1iv(int location, int count, IntBuffer v) {
        ++calls;
        this.gl20.glUniform1iv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform1iv(int location, int count, int[] v, int offset) {
        ++calls;
        this.gl20.glUniform1iv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform2f(int location, float x, float y) {
        ++calls;
        this.gl20.glUniform2f(location, x, y);
        this.check();
    }

    @Override
    public void glUniform2fv(int location, int count, FloatBuffer v) {
        ++calls;
        this.gl20.glUniform2fv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform2fv(int location, int count, float[] v, int offset) {
        ++calls;
        this.gl20.glUniform2fv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform2i(int location, int x, int y) {
        ++calls;
        this.gl20.glUniform2i(location, x, y);
        this.check();
    }

    @Override
    public void glUniform2iv(int location, int count, IntBuffer v) {
        ++calls;
        this.gl20.glUniform2iv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform2iv(int location, int count, int[] v, int offset) {
        ++calls;
        this.gl20.glUniform2iv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z) {
        ++calls;
        this.gl20.glUniform3f(location, x, y, z);
        this.check();
    }

    @Override
    public void glUniform3fv(int location, int count, FloatBuffer v) {
        ++calls;
        this.gl20.glUniform3fv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform3fv(int location, int count, float[] v, int offset) {
        ++calls;
        this.gl20.glUniform3fv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z) {
        ++calls;
        this.gl20.glUniform3i(location, x, y, z);
        this.check();
    }

    @Override
    public void glUniform3iv(int location, int count, IntBuffer v) {
        ++calls;
        this.gl20.glUniform3iv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform3iv(int location, int count, int[] v, int offset) {
        ++calls;
        this.gl20.glUniform3iv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {
        ++calls;
        this.gl20.glUniform4f(location, x, y, z, w);
        this.check();
    }

    @Override
    public void glUniform4fv(int location, int count, FloatBuffer v) {
        ++calls;
        this.gl20.glUniform4fv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform4fv(int location, int count, float[] v, int offset) {
        ++calls;
        this.gl20.glUniform4fv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w) {
        ++calls;
        this.gl20.glUniform4i(location, x, y, z, w);
        this.check();
    }

    @Override
    public void glUniform4iv(int location, int count, IntBuffer v) {
        ++calls;
        this.gl20.glUniform4iv(location, count, v);
        this.check();
    }

    @Override
    public void glUniform4iv(int location, int count, int[] v, int offset) {
        ++calls;
        this.gl20.glUniform4iv(location, count, v, offset);
        this.check();
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
        ++calls;
        this.gl20.glUniformMatrix2fv(location, count, transpose, value);
        this.check();
    }

    @Override
    public void glUniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset) {
        ++calls;
        this.gl20.glUniformMatrix2fv(location, count, transpose, value, offset);
        this.check();
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
        ++calls;
        this.gl20.glUniformMatrix3fv(location, count, transpose, value);
        this.check();
    }

    @Override
    public void glUniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset) {
        ++calls;
        this.gl20.glUniformMatrix3fv(location, count, transpose, value, offset);
        this.check();
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
        ++calls;
        this.gl20.glUniformMatrix4fv(location, count, transpose, value);
        this.check();
    }

    @Override
    public void glUniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset) {
        ++calls;
        this.gl20.glUniformMatrix4fv(location, count, transpose, value, offset);
        this.check();
    }

    @Override
    public void glUseProgram(int program) {
        ++shaderSwitches;
        ++calls;
        this.gl20.glUseProgram(program);
        this.check();
    }

    @Override
    public void glValidateProgram(int program) {
        ++calls;
        this.gl20.glValidateProgram(program);
        this.check();
    }

    @Override
    public void glVertexAttrib1f(int indx, float x) {
        ++calls;
        this.gl20.glVertexAttrib1f(indx, x);
        this.check();
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values) {
        ++calls;
        this.gl20.glVertexAttrib1fv(indx, values);
        this.check();
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y) {
        ++calls;
        this.gl20.glVertexAttrib2f(indx, x, y);
        this.check();
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values) {
        ++calls;
        this.gl20.glVertexAttrib2fv(indx, values);
        this.check();
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z) {
        ++calls;
        this.gl20.glVertexAttrib3f(indx, x, y, z);
        this.check();
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values) {
        ++calls;
        this.gl20.glVertexAttrib3fv(indx, values);
        this.check();
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        ++calls;
        this.gl20.glVertexAttrib4f(indx, x, y, z, w);
        this.check();
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values) {
        ++calls;
        this.gl20.glVertexAttrib4fv(indx, values);
        this.check();
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {
        ++calls;
        this.gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
        this.check();
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {
        ++calls;
        this.gl20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
        this.check();
    }
}

