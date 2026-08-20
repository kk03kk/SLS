/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.CopyStreamListener;

public final class Util {
    public static final int DEFAULT_COPY_BUFFER_SIZE = 1024;

    private Util() {
    }

    public static final long copyStream(InputStream source, OutputStream dest, int bufferSize, long streamSize, CopyStreamListener listener, boolean flush) throws CopyStreamException {
        long total = 0L;
        byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 1024];
        try {
            int numBytes;
            while ((numBytes = source.read(buffer)) != -1) {
                if (numBytes == 0) {
                    int singleByte = source.read();
                    if (singleByte >= 0) {
                        dest.write(singleByte);
                        if (flush) {
                            dest.flush();
                        }
                        ++total;
                        if (listener == null) continue;
                        listener.bytesTransferred(total, 1, streamSize);
                        continue;
                    }
                    break;
                }
                dest.write(buffer, 0, numBytes);
                if (flush) {
                    dest.flush();
                }
                total += (long)numBytes;
                if (listener == null) continue;
                listener.bytesTransferred(total, numBytes, streamSize);
            }
        }
        catch (IOException e) {
            throw new CopyStreamException("IOException caught while copying.", total, e);
        }
        return total;
    }

    public static final long copyStream(InputStream source, OutputStream dest, int bufferSize, long streamSize, CopyStreamListener listener) throws CopyStreamException {
        return Util.copyStream(source, dest, bufferSize, streamSize, listener, true);
    }

    public static final long copyStream(InputStream source, OutputStream dest, int bufferSize) throws CopyStreamException {
        return Util.copyStream(source, dest, bufferSize, -1L, null);
    }

    public static final long copyStream(InputStream source, OutputStream dest) throws CopyStreamException {
        return Util.copyStream(source, dest, 1024);
    }

    public static final long copyReader(Reader source, Writer dest, int bufferSize, long streamSize, CopyStreamListener listener) throws CopyStreamException {
        long total = 0L;
        char[] buffer = new char[bufferSize > 0 ? bufferSize : 1024];
        try {
            int numChars;
            while ((numChars = source.read(buffer)) != -1) {
                if (numChars == 0) {
                    int singleChar = source.read();
                    if (singleChar >= 0) {
                        dest.write(singleChar);
                        dest.flush();
                        ++total;
                        if (listener == null) continue;
                        listener.bytesTransferred(total, 1, streamSize);
                        continue;
                    }
                    break;
                }
                dest.write(buffer, 0, numChars);
                dest.flush();
                total += (long)numChars;
                if (listener == null) continue;
                listener.bytesTransferred(total, numChars, streamSize);
            }
        }
        catch (IOException e) {
            throw new CopyStreamException("IOException caught while copying.", total, e);
        }
        return total;
    }

    public static final long copyReader(Reader source, Writer dest, int bufferSize) throws CopyStreamException {
        return Util.copyReader(source, dest, bufferSize, -1L, null);
    }

    public static final long copyReader(Reader source, Writer dest) throws CopyStreamException {
        return Util.copyReader(source, dest, 1024);
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }
}

