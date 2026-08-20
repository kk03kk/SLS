/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.ktx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ETC1;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

public class KTXProcessor {
    static final byte[] HEADER_MAGIC = new byte[]{-85, 75, 84, 88, 32, 49, 49, -69, 13, 10, 26, 10};
    private static final int DISPOSE_DONT = 0;
    private static final int DISPOSE_PACK = 1;
    private static final int DISPOSE_FACE = 2;
    private static final int DISPOSE_LEVEL = 4;

    public static void convert(String input, String output, boolean genMipmaps, boolean packETC1, boolean genAlphaAtlas) throws Exception {
        Array<String> opts = new Array<String>(String.class);
        opts.add(input);
        opts.add(output);
        if (genMipmaps) {
            opts.add("-mipmaps");
        }
        if (packETC1 && !genAlphaAtlas) {
            opts.add("-etc1");
        }
        if (packETC1 && genAlphaAtlas) {
            opts.add("-etc1a");
        }
        KTXProcessor.main((String[])opts.toArray());
    }

    public static void convert(String inPx, String inNx, String inPy, String inNy, String inPz, String inNz, String output, boolean genMipmaps, boolean packETC1, boolean genAlphaAtlas) throws Exception {
        Array<String> opts = new Array<String>(String.class);
        opts.add(inPx);
        opts.add(inNx);
        opts.add(inPy);
        opts.add(inNy);
        opts.add(inPz);
        opts.add(inNz);
        opts.add(output);
        if (genMipmaps) {
            opts.add("-mipmaps");
        }
        if (packETC1 && !genAlphaAtlas) {
            opts.add("-etc1");
        }
        if (packETC1 && genAlphaAtlas) {
            opts.add("-etc1a");
        }
        KTXProcessor.main((String[])opts.toArray());
    }

    public static void main(String[] args) {
        new HeadlessApplication(new KTXProcessorListener(args));
    }

    private static class Image {
        public ETC1.ETC1Data etcData;
        public Pixmap pixmap;

        public int getSize() {
            if (this.etcData != null) {
                return this.etcData.compressedData.limit() - this.etcData.dataOffset;
            }
            throw new GdxRuntimeException("Unsupported output format, try adding '-etc1' as argument");
        }

        public byte[] getBytes() {
            if (this.etcData != null) {
                byte[] result = new byte[this.getSize()];
                this.etcData.compressedData.position(this.etcData.dataOffset);
                this.etcData.compressedData.get(result);
                return result;
            }
            throw new GdxRuntimeException("Unsupported output format, try adding '-etc1' as argument");
        }
    }

    public static class KTXProcessorListener
    extends ApplicationAdapter {
        String[] args;

        KTXProcessorListener(String[] args) {
            this.args = args;
        }

        @Override
        public void create() {
            int glBaseInternalFormat;
            int glInternalFormat;
            int glTypeSize;
            int glType;
            int glFormat;
            boolean isCubemap = this.args.length == 7 || this.args.length == 8 || this.args.length == 9;
            boolean isTexture = this.args.length == 2 || this.args.length == 3 || this.args.length == 4;
            boolean isPackETC1 = false;
            boolean isAlphaAtlas = false;
            boolean isGenMipMaps = false;
            if (!isCubemap && !isTexture) {
                System.out.println("usage : KTXProcessor input_file output_file [-etc1|-etc1a] [-mipmaps]");
                System.out.println("  input_file  is the texture file to include in the output KTX or ZKTX file.");
                System.out.println("              for cube map, just provide 6 input files corresponding to the faces in the following order : X+, X-, Y+, Y-, Z+, Z-");
                System.out.println("  output_file is the path to the output file, its type is based on the extension which must be either KTX or ZKTX");
                System.out.println();
                System.out.println("  options:");
                System.out.println("    -etc1    input file will be packed using ETC1 compression, dropping the alpha channel");
                System.out.println("    -etc1a   input file will be packed using ETC1 compression, doubling the height and placing the alpha channel in the bottom half");
                System.out.println("    -mipmaps input file will be processed to generate mipmaps");
                System.out.println();
                System.out.println("  examples:");
                System.out.println("    KTXProcessor in.png out.ktx                                        Create a KTX file with the provided 2D texture");
                System.out.println("    KTXProcessor in.png out.zktx                                       Create a Zipped KTX file with the provided 2D texture");
                System.out.println("    KTXProcessor in.png out.zktx -mipmaps                              Create a Zipped KTX file with the provided 2D texture, generating all mipmap levels");
                System.out.println("    KTXProcessor px.ktx nx.ktx py.ktx ny.ktx pz.ktx nz.ktx out.zktx    Create a Zipped KTX file with the provided cubemap textures");
                System.out.println("    KTXProcessor in.ktx out.zktx                                       Convert a KTX file to a Zipped KTX file");
                System.exit(-1);
            }
            LwjglNativesLoader.load();
            for (int i = 0; i < this.args.length; ++i) {
                System.out.println(i + " = " + this.args[i]);
                if (isTexture && i < 2 || isCubemap && i < 7) continue;
                if ("-etc1".equals(this.args[i])) {
                    isPackETC1 = true;
                }
                if ("-etc1a".equals(this.args[i])) {
                    isPackETC1 = true;
                    isAlphaAtlas = true;
                }
                if (!"-mipmaps".equals(this.args[i])) continue;
                isGenMipMaps = true;
            }
            File output = new File(this.args[isCubemap ? 6 : 1]);
            int ktxDispose = 0;
            KTXTextureData ktx = null;
            FileHandle file = new FileHandle(this.args[0]);
            if (file.name().toLowerCase().endsWith(".ktx") || file.name().toLowerCase().endsWith(".zktx")) {
                ktx = new KTXTextureData(file, false);
                if (ktx.getNumberOfFaces() == 6) {
                    isCubemap = true;
                }
                ktxDispose = 1;
            }
            int nFaces = isCubemap ? 6 : 1;
            Image[][] images = new Image[nFaces][];
            Pixmap.setBlending(Pixmap.Blending.None);
            Pixmap.setFilter(Pixmap.Filter.BiLinear);
            int texWidth = -1;
            int texHeight = -1;
            int texFormat = -1;
            int nLevels = 0;
            for (int face = 0; face < nFaces; ++face) {
                ETC1.ETC1Data etc1 = null;
                Pixmap facePixmap = null;
                int ktxFace = 0;
                if (ktx != null && ktx.getNumberOfFaces() == 6) {
                    nLevels = ktx.getNumberOfMipMapLevels();
                    ktxFace = face;
                } else {
                    file = new FileHandle(this.args[face]);
                    System.out.println("Processing : " + file + " for face #" + face);
                    if (file.name().toLowerCase().endsWith(".ktx") || file.name().toLowerCase().endsWith(".zktx")) {
                        if (ktx == null || ktx.getNumberOfFaces() != 6) {
                            ktxDispose = 2;
                            ktx = new KTXTextureData(file, false);
                            ktx.prepare();
                        }
                        nLevels = ktx.getNumberOfMipMapLevels();
                        texWidth = ktx.getWidth();
                        texHeight = ktx.getHeight();
                    } else if (file.name().toLowerCase().endsWith(".etc1")) {
                        etc1 = new ETC1.ETC1Data(file);
                        nLevels = 1;
                        texWidth = etc1.width;
                        texHeight = etc1.height;
                    } else {
                        facePixmap = new Pixmap(file);
                        nLevels = 1;
                        texWidth = facePixmap.getWidth();
                        texHeight = facePixmap.getHeight();
                    }
                    if (isGenMipMaps) {
                        if (!MathUtils.isPowerOfTwo(texWidth) || !MathUtils.isPowerOfTwo(texHeight)) {
                            throw new GdxRuntimeException("Invalid input : mipmap generation is only available for power of two textures : " + file);
                        }
                        nLevels = Math.max(32 - Integer.numberOfLeadingZeros(texWidth), 32 - Integer.numberOfLeadingZeros(texHeight));
                    }
                }
                images[face] = new Image[nLevels];
                for (int level = 0; level < nLevels; ++level) {
                    ByteBuffer ktxData;
                    int levelWidth = Math.max(1, texWidth >> level);
                    int levelHeight = Math.max(1, texHeight >> level);
                    Pixmap levelPixmap = null;
                    ETC1.ETC1Data levelETCData = null;
                    if (ktx != null && (ktxData = ktx.getData(level, ktxFace)) != null && ktx.getGlInternalFormat() == ETC1.ETC1_RGB8_OES) {
                        levelETCData = new ETC1.ETC1Data(levelWidth, levelHeight, ktxData, 0);
                    }
                    if (ktx != null && levelETCData == null && facePixmap == null && (ktxData = ktx.getData(0, ktxFace)) != null && ktx.getGlInternalFormat() == ETC1.ETC1_RGB8_OES) {
                        facePixmap = ETC1.decodeImage(new ETC1.ETC1Data(levelWidth, levelHeight, ktxData, 0), Pixmap.Format.RGB888);
                    }
                    if (level == 0 && etc1 != null) {
                        levelETCData = etc1;
                    }
                    if (levelETCData == null && etc1 != null && facePixmap == null) {
                        facePixmap = ETC1.decodeImage(etc1, Pixmap.Format.RGB888);
                    }
                    if (levelETCData == null) {
                        levelPixmap = new Pixmap(levelWidth, levelHeight, facePixmap.getFormat());
                        levelPixmap.drawPixmap(facePixmap, 0, 0, facePixmap.getWidth(), facePixmap.getHeight(), 0, 0, levelPixmap.getWidth(), levelPixmap.getHeight());
                    }
                    if (levelETCData == null && levelPixmap == null) {
                        throw new GdxRuntimeException("Failed to load data for face " + face + " / mipmap level " + level);
                    }
                    if (isAlphaAtlas) {
                        if (levelPixmap == null) {
                            levelPixmap = ETC1.decodeImage(levelETCData, Pixmap.Format.RGB888);
                        }
                        int w = levelPixmap.getWidth();
                        int h = levelPixmap.getHeight();
                        Pixmap pm = new Pixmap(w, h * 2, levelPixmap.getFormat());
                        pm.drawPixmap(levelPixmap, 0, 0);
                        for (int y = 0; y < h; ++y) {
                            for (int x = 0; x < w; ++x) {
                                int alpha = levelPixmap.getPixel(x, y) & 0xFF;
                                pm.drawPixel(x, y + h, alpha << 24 | alpha << 16 | alpha << 8 | 0xFF);
                            }
                        }
                        levelPixmap.dispose();
                        levelPixmap = pm;
                        levelETCData = null;
                    }
                    if (levelETCData == null && isPackETC1) {
                        if (levelPixmap.getFormat() != Pixmap.Format.RGB888 && levelPixmap.getFormat() != Pixmap.Format.RGB565) {
                            if (!isAlphaAtlas) {
                                System.out.println("Converting from " + (Object)((Object)levelPixmap.getFormat()) + " to RGB888 for ETC1 compression");
                            }
                            Pixmap tmp = new Pixmap(levelPixmap.getWidth(), levelPixmap.getHeight(), Pixmap.Format.RGB888);
                            tmp.drawPixmap(levelPixmap, 0, 0, 0, 0, levelPixmap.getWidth(), levelPixmap.getHeight());
                            levelPixmap.dispose();
                            levelPixmap = tmp;
                        }
                        levelETCData = ETC1.encodeImagePKM(levelPixmap);
                        levelPixmap.dispose();
                        levelPixmap = null;
                    }
                    images[face][level] = new Image();
                    images[face][level].etcData = levelETCData;
                    images[face][level].pixmap = levelPixmap;
                    if (levelPixmap == null) continue;
                    levelPixmap.dispose();
                    facePixmap = null;
                }
                if (facePixmap != null) {
                    facePixmap.dispose();
                    facePixmap = null;
                }
                if (etc1 != null) {
                    etc1.dispose();
                    etc1 = null;
                }
                if (ktx == null || ktxDispose != 2) continue;
                ktx.disposePreparedData();
                ktx = null;
            }
            if (ktx != null) {
                ktx.disposePreparedData();
                ktx = null;
            }
            if (isPackETC1) {
                glFormat = 0;
                glType = 0;
                glTypeSize = 1;
                glInternalFormat = ETC1.ETC1_RGB8_OES;
                glBaseInternalFormat = 6407;
            } else if (images[0][0].pixmap != null) {
                glType = images[0][0].pixmap.getGLType();
                glTypeSize = 1;
                glFormat = images[0][0].pixmap.getGLFormat();
                glInternalFormat = images[0][0].pixmap.getGLInternalFormat();
                glBaseInternalFormat = glFormat;
            } else {
                throw new GdxRuntimeException("Unsupported output format");
            }
            int totalSize = 64;
            for (int level = 0; level < nLevels; ++level) {
                System.out.println("Level: " + level);
                int faceLodSize = images[0][level].getSize();
                int faceLodSizeRounded = faceLodSize + 3 & 0xFFFFFFFC;
                totalSize += 4;
                totalSize += nFaces * faceLodSizeRounded;
            }
            try {
                DataOutputStream out;
                if (output.getName().toLowerCase().endsWith(".zktx")) {
                    out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(output)));
                    out.writeInt(totalSize);
                } else {
                    out = new DataOutputStream(new FileOutputStream(output));
                }
                out.write(HEADER_MAGIC);
                out.writeInt(67305985);
                out.writeInt(glType);
                out.writeInt(glTypeSize);
                out.writeInt(glFormat);
                out.writeInt(glInternalFormat);
                out.writeInt(glBaseInternalFormat);
                out.writeInt(texWidth);
                out.writeInt(isAlphaAtlas ? 2 * texHeight : texHeight);
                out.writeInt(0);
                out.writeInt(0);
                out.writeInt(nFaces);
                out.writeInt(nLevels);
                out.writeInt(0);
                for (int level = 0; level < nLevels; ++level) {
                    int faceLodSize = images[0][level].getSize();
                    int faceLodSizeRounded = faceLodSize + 3 & 0xFFFFFFFC;
                    out.writeInt(faceLodSize);
                    for (int face = 0; face < nFaces; ++face) {
                        byte[] bytes = images[face][level].getBytes();
                        out.write(bytes);
                        for (int j = bytes.length; j < faceLodSizeRounded; ++j) {
                            out.write(0);
                        }
                    }
                }
                out.close();
            }
            catch (Exception e) {
                Gdx.app.error("KTXProcessor", "Error writing to file: " + output.getName(), e);
            }
        }
    }
}

