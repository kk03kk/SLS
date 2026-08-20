/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ETC1 {
    public static int PKM_HEADER_SIZE = 16;
    public static int ETC1_RGB8_OES = 36196;

    private static int getPixelSize(Pixmap.Format format) {
        if (format == Pixmap.Format.RGB565) {
            return 2;
        }
        if (format == Pixmap.Format.RGB888) {
            return 3;
        }
        throw new GdxRuntimeException("Can only handle RGB565 or RGB888 images");
    }

    public static ETC1Data encodeImage(Pixmap pixmap) {
        int pixelSize = ETC1.getPixelSize(pixmap.getFormat());
        ByteBuffer compressedData = ETC1.encodeImage(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize);
        BufferUtils.newUnsafeByteBuffer(compressedData);
        return new ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 0);
    }

    public static ETC1Data encodeImagePKM(Pixmap pixmap) {
        int pixelSize = ETC1.getPixelSize(pixmap.getFormat());
        ByteBuffer compressedData = ETC1.encodeImagePKM(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize);
        BufferUtils.newUnsafeByteBuffer(compressedData);
        return new ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 16);
    }

    public static Pixmap decodeImage(ETC1Data etc1Data, Pixmap.Format format) {
        int dataOffset = 0;
        int width = 0;
        int height = 0;
        if (etc1Data.hasPKMHeader()) {
            dataOffset = 16;
            width = ETC1.getWidthPKM(etc1Data.compressedData, 0);
            height = ETC1.getHeightPKM(etc1Data.compressedData, 0);
        } else {
            dataOffset = 0;
            width = etc1Data.width;
            height = etc1Data.height;
        }
        int pixelSize = ETC1.getPixelSize(format);
        Pixmap pixmap = new Pixmap(width, height, format);
        ETC1.decodeImage(etc1Data.compressedData, dataOffset, pixmap.getPixels(), 0, width, height, pixelSize);
        return pixmap;
    }

    public static native int getCompressedDataSize(int var0, int var1);

    public static native void formatHeader(ByteBuffer var0, int var1, int var2, int var3);

    static native int getWidthPKM(ByteBuffer var0, int var1);

    static native int getHeightPKM(ByteBuffer var0, int var1);

    static native boolean isValidPKM(ByteBuffer var0, int var1);

    private static native void decodeImage(ByteBuffer var0, int var1, ByteBuffer var2, int var3, int var4, int var5, int var6);

    private static native ByteBuffer encodeImage(ByteBuffer var0, int var1, int var2, int var3, int var4);

    private static native ByteBuffer encodeImagePKM(ByteBuffer var0, int var1, int var2, int var3, int var4);

    public static final class ETC1Data
    implements Disposable {
        public final int width;
        public final int height;
        public final ByteBuffer compressedData;
        public final int dataOffset;

        public ETC1Data(int width, int height, ByteBuffer compressedData, int dataOffset) {
            this.width = width;
            this.height = height;
            this.compressedData = compressedData;
            this.dataOffset = dataOffset;
            this.checkNPOT();
        }

        public ETC1Data(FileHandle pkmFile) {
            byte[] buffer = new byte[10240];
            DataInputStream in = null;
            try {
                in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(pkmFile.read())));
                int fileSize = in.readInt();
                this.compressedData = BufferUtils.newUnsafeByteBuffer(fileSize);
                int readBytes = 0;
                while ((readBytes = in.read(buffer)) != -1) {
                    this.compressedData.put(buffer, 0, readBytes);
                }
                this.compressedData.position(0);
                this.compressedData.limit(this.compressedData.capacity());
            }
            catch (Exception e) {
                try {
                    throw new GdxRuntimeException("Couldn't load pkm file '" + pkmFile + "'", e);
                }
                catch (Throwable throwable) {
                    StreamUtils.closeQuietly(in);
                    throw throwable;
                }
            }
            StreamUtils.closeQuietly(in);
            this.width = ETC1.getWidthPKM(this.compressedData, 0);
            this.height = ETC1.getHeightPKM(this.compressedData, 0);
            this.dataOffset = PKM_HEADER_SIZE;
            this.compressedData.position(this.dataOffset);
            this.checkNPOT();
        }

        private void checkNPOT() {
            if (!MathUtils.isPowerOfTwo(this.width) || !MathUtils.isPowerOfTwo(this.height)) {
                System.out.println("ETC1Data warning: non-power-of-two ETC1 textures may crash the driver of PowerVR GPUs");
            }
        }

        public boolean hasPKMHeader() {
            return this.dataOffset == 16;
        }

        public void write(FileHandle file) {
            DataOutputStream write = null;
            byte[] buffer = new byte[10240];
            this.compressedData.position(0);
            this.compressedData.limit(this.compressedData.capacity());
            try {
                int bytesToWrite;
                write = new DataOutputStream(new GZIPOutputStream(file.write(false)));
                write.writeInt(this.compressedData.capacity());
                for (int writtenBytes = 0; writtenBytes != this.compressedData.capacity(); writtenBytes += bytesToWrite) {
                    bytesToWrite = Math.min(this.compressedData.remaining(), buffer.length);
                    this.compressedData.get(buffer, 0, bytesToWrite);
                    write.write(buffer, 0, bytesToWrite);
                }
            }
            catch (Exception e) {
                try {
                    throw new GdxRuntimeException("Couldn't write PKM file to '" + file + "'", e);
                }
                catch (Throwable throwable) {
                    StreamUtils.closeQuietly(write);
                    throw throwable;
                }
            }
            StreamUtils.closeQuietly(write);
            this.compressedData.position(this.dataOffset);
            this.compressedData.limit(this.compressedData.capacity());
        }

        @Override
        public void dispose() {
            BufferUtils.disposeUnsafeByteBuffer(this.compressedData);
        }

        public String toString() {
            if (this.hasPKMHeader()) {
                return (ETC1.isValidPKM(this.compressedData, 0) ? "valid" : "invalid") + " pkm [" + ETC1.getWidthPKM(this.compressedData, 0) + "x" + ETC1.getHeightPKM(this.compressedData, 0) + "], compressed: " + (this.compressedData.capacity() - PKM_HEADER_SIZE);
            }
            return "raw [" + this.width + "x" + this.height + "], compressed: " + (this.compressedData.capacity() - PKM_HEADER_SIZE);
        }
    }
}

