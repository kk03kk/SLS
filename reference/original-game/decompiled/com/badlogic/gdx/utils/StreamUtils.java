/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.BufferUtils;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class StreamUtils {
    public static final int DEFAULT_BUFFER_SIZE = 4096;
    public static final byte[] EMPTY_BYTES = new byte[0];

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        StreamUtils.copyStream(input, output, new byte[4096]);
    }

    public static void copyStream(InputStream input, OutputStream output, int bufferSize) throws IOException {
        StreamUtils.copyStream(input, output, new byte[bufferSize]);
    }

    public static void copyStream(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static void copyStream(InputStream input, ByteBuffer output) throws IOException {
        StreamUtils.copyStream(input, output, new byte[4096]);
    }

    public static void copyStream(InputStream input, ByteBuffer output, int bufferSize) throws IOException {
        StreamUtils.copyStream(input, output, new byte[bufferSize]);
    }

    public static int copyStream(InputStream input, ByteBuffer output, byte[] buffer) throws IOException {
        int bytesRead;
        int startPosition = output.position();
        int total = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            BufferUtils.copy(buffer, 0, (Buffer)output, bytesRead);
            output.position(startPosition + (total += bytesRead));
        }
        output.position(startPosition);
        return total;
    }

    public static byte[] copyStreamToByteArray(InputStream input) throws IOException {
        return StreamUtils.copyStreamToByteArray(input, input.available());
    }

    public static byte[] copyStreamToByteArray(InputStream input, int estimatedSize) throws IOException {
        OptimizedByteArrayOutputStream baos = new OptimizedByteArrayOutputStream(Math.max(0, estimatedSize));
        StreamUtils.copyStream(input, baos);
        return ((ByteArrayOutputStream)baos).toByteArray();
    }

    public static String copyStreamToString(InputStream input) throws IOException {
        return StreamUtils.copyStreamToString(input, input.available(), null);
    }

    public static String copyStreamToString(InputStream input, int estimatedSize) throws IOException {
        return StreamUtils.copyStreamToString(input, estimatedSize, null);
    }

    public static String copyStreamToString(InputStream input, int estimatedSize, String charset) throws IOException {
        int charsRead;
        InputStreamReader reader = charset == null ? new InputStreamReader(input) : new InputStreamReader(input, charset);
        StringWriter writer = new StringWriter(Math.max(0, estimatedSize));
        char[] buffer = new char[4096];
        while ((charsRead = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, charsRead);
        }
        return writer.toString();
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static class OptimizedByteArrayOutputStream
    extends ByteArrayOutputStream {
        public OptimizedByteArrayOutputStream(int initialSize) {
            super(initialSize);
        }

        @Override
        public synchronized byte[] toByteArray() {
            if (this.count == this.buf.length) {
                return this.buf;
            }
            return super.toByteArray();
        }

        public byte[] getBuffer() {
            return this.buf;
        }
    }
}

