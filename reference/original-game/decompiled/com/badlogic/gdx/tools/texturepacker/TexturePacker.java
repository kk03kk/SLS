/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.texturepacker;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.tools.texturepacker.ColorBleedEffect;
import com.badlogic.gdx.tools.texturepacker.GridPacker;
import com.badlogic.gdx.tools.texturepacker.ImageProcessor;
import com.badlogic.gdx.tools.texturepacker.MaxRectsPacker;
import com.badlogic.gdx.tools.texturepacker.TexturePackerFileProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

public class TexturePacker {
    private final Settings settings;
    private final Packer packer;
    private final ImageProcessor imageProcessor;
    private final Array<InputImage> inputImages = new Array();
    private File rootDir;

    public TexturePacker(File rootDir, Settings settings) {
        this.rootDir = rootDir;
        this.settings = settings;
        if (settings.pot) {
            if (settings.maxWidth != MathUtils.nextPowerOfTwo(settings.maxWidth)) {
                throw new RuntimeException("If pot is true, maxWidth must be a power of two: " + settings.maxWidth);
            }
            if (settings.maxHeight != MathUtils.nextPowerOfTwo(settings.maxHeight)) {
                throw new RuntimeException("If pot is true, maxHeight must be a power of two: " + settings.maxHeight);
            }
        }
        this.packer = settings.grid ? new GridPacker(settings) : new MaxRectsPacker(settings);
        this.imageProcessor = new ImageProcessor(rootDir, settings);
    }

    public TexturePacker(Settings settings) {
        this(null, settings);
    }

    public void addImage(File file) {
        InputImage inputImage = new InputImage();
        inputImage.file = file;
        this.inputImages.add(inputImage);
    }

    public void addImage(BufferedImage image, String name) {
        InputImage inputImage = new InputImage();
        inputImage.image = image;
        inputImage.name = name;
        this.inputImages.add(inputImage);
    }

    public void pack(File outputDir, String packFileName) {
        if (packFileName.endsWith(this.settings.atlasExtension)) {
            packFileName = packFileName.substring(0, packFileName.length() - this.settings.atlasExtension.length());
        }
        outputDir.mkdirs();
        int n = this.settings.scale.length;
        for (int i = 0; i < n; ++i) {
            this.imageProcessor.setScale(this.settings.scale[i]);
            for (InputImage inputImage : this.inputImages) {
                if (inputImage.file != null) {
                    this.imageProcessor.addImage(inputImage.file);
                    continue;
                }
                this.imageProcessor.addImage(inputImage.image, inputImage.name);
            }
            Array<Page> pages = this.packer.pack(this.imageProcessor.getImages());
            String scaledPackFileName = this.settings.getScaledPackFileName(packFileName, i);
            this.writeImages(outputDir, scaledPackFileName, pages);
            try {
                this.writePackFile(outputDir, scaledPackFileName, pages);
            }
            catch (IOException ex) {
                throw new RuntimeException("Error writing pack file.", ex);
            }
            this.imageProcessor.clear();
        }
    }

    private void writeImages(File outputDir, String scaledPackFileName, Array<Page> pages) {
        File packFileNoExt = new File(outputDir, scaledPackFileName);
        File packDir = packFileNoExt.getParentFile();
        String imageName = packFileNoExt.getName();
        int fileIndex = 0;
        for (Page page : pages) {
            File outputFile;
            int width = page.width;
            int height = page.height;
            int paddingX = this.settings.paddingX;
            int paddingY = this.settings.paddingY;
            if (this.settings.duplicatePadding) {
                paddingX /= 2;
                paddingY /= 2;
            }
            width -= this.settings.paddingX;
            height -= this.settings.paddingY;
            if (this.settings.edgePadding) {
                page.x = paddingX;
                page.y = paddingY;
                width += paddingX * 2;
                height += paddingY * 2;
            }
            if (this.settings.pot) {
                width = MathUtils.nextPowerOfTwo(width);
                height = MathUtils.nextPowerOfTwo(height);
            }
            width = Math.max(this.settings.minWidth, width);
            height = Math.max(this.settings.minHeight, height);
            page.imageWidth = width;
            page.imageHeight = height;
            while ((outputFile = new File(packDir, imageName + (fileIndex++ == 0 ? "" : Integer.valueOf(fileIndex)) + "." + this.settings.outputFormat)).exists()) {
            }
            new FileHandle(outputFile).parent().mkdirs();
            page.imageName = outputFile.getName();
            BufferedImage canvas = new BufferedImage(width, height, this.getBufferedImageType(this.settings.format));
            Graphics2D g = (Graphics2D)canvas.getGraphics();
            if (!this.settings.silent) {
                System.out.println("Writing " + canvas.getWidth() + "x" + canvas.getHeight() + ": " + outputFile);
            }
            for (Rect rect : page.outputRects) {
                BufferedImage image = rect.getImage(this.imageProcessor);
                int iw = image.getWidth();
                int ih = image.getHeight();
                int rectX = page.x + rect.x;
                int rectY = page.y + page.height - rect.y - rect.height;
                if (this.settings.duplicatePadding) {
                    int j;
                    int i;
                    int amountX = this.settings.paddingX / 2;
                    int amountY = this.settings.paddingY / 2;
                    if (rect.rotated) {
                        for (i = 1; i <= amountX; ++i) {
                            for (j = 1; j <= amountY; ++j) {
                                TexturePacker.plot(canvas, rectX - j, rectY + iw - 1 + i, image.getRGB(0, 0));
                                TexturePacker.plot(canvas, rectX + ih - 1 + j, rectY + iw - 1 + i, image.getRGB(0, ih - 1));
                                TexturePacker.plot(canvas, rectX - j, rectY - i, image.getRGB(iw - 1, 0));
                                TexturePacker.plot(canvas, rectX + ih - 1 + j, rectY - i, image.getRGB(iw - 1, ih - 1));
                            }
                        }
                        for (i = 1; i <= amountY; ++i) {
                            for (j = 0; j < iw; ++j) {
                                TexturePacker.plot(canvas, rectX - i, rectY + iw - 1 - j, image.getRGB(j, 0));
                                TexturePacker.plot(canvas, rectX + ih - 1 + i, rectY + iw - 1 - j, image.getRGB(j, ih - 1));
                            }
                        }
                        for (i = 1; i <= amountX; ++i) {
                            for (j = 0; j < ih; ++j) {
                                TexturePacker.plot(canvas, rectX + j, rectY - i, image.getRGB(iw - 1, j));
                                TexturePacker.plot(canvas, rectX + j, rectY + iw - 1 + i, image.getRGB(0, j));
                            }
                        }
                    } else {
                        for (i = 1; i <= amountX; ++i) {
                            for (j = 1; j <= amountY; ++j) {
                                TexturePacker.plot(canvas, rectX - i, rectY - j, image.getRGB(0, 0));
                                TexturePacker.plot(canvas, rectX - i, rectY + ih - 1 + j, image.getRGB(0, ih - 1));
                                TexturePacker.plot(canvas, rectX + iw - 1 + i, rectY - j, image.getRGB(iw - 1, 0));
                                TexturePacker.plot(canvas, rectX + iw - 1 + i, rectY + ih - 1 + j, image.getRGB(iw - 1, ih - 1));
                            }
                        }
                        for (i = 1; i <= amountY; ++i) {
                            TexturePacker.copy(image, 0, 0, iw, 1, canvas, rectX, rectY - i, rect.rotated);
                            TexturePacker.copy(image, 0, ih - 1, iw, 1, canvas, rectX, rectY + ih - 1 + i, rect.rotated);
                        }
                        for (i = 1; i <= amountX; ++i) {
                            TexturePacker.copy(image, 0, 0, 1, ih, canvas, rectX - i, rectY, rect.rotated);
                            TexturePacker.copy(image, iw - 1, 0, 1, ih, canvas, rectX + iw - 1 + i, rectY, rect.rotated);
                        }
                    }
                }
                TexturePacker.copy(image, 0, 0, iw, ih, canvas, rectX, rectY, rect.rotated);
                if (!this.settings.debug) continue;
                g.setColor(Color.magenta);
                g.drawRect(rectX, rectY, rect.width - this.settings.paddingX - 1, rect.height - this.settings.paddingY - 1);
            }
            if (this.settings.bleed && !this.settings.premultiplyAlpha && !this.settings.outputFormat.equalsIgnoreCase("jpg") && !this.settings.outputFormat.equalsIgnoreCase("jpeg")) {
                canvas = new ColorBleedEffect().processImage(canvas, 2);
                g = (Graphics2D)canvas.getGraphics();
            }
            if (this.settings.debug) {
                g.setColor(Color.magenta);
                g.drawRect(0, 0, width - 1, height - 1);
            }
            ImageInputStream ios = null;
            try {
                if (this.settings.outputFormat.equalsIgnoreCase("jpg") || this.settings.outputFormat.equalsIgnoreCase("jpeg")) {
                    BufferedImage newImage = new BufferedImage(canvas.getWidth(), canvas.getHeight(), 5);
                    newImage.getGraphics().drawImage(canvas, 0, 0, null);
                    canvas = newImage;
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                    ImageWriter writer = writers.next();
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(2);
                    param.setCompressionQuality(this.settings.jpegQuality);
                    ios = ImageIO.createImageOutputStream(outputFile);
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(canvas, null, null), param);
                    continue;
                }
                if (this.settings.premultiplyAlpha) {
                    canvas.getColorModel().coerceData(canvas.getRaster(), true);
                }
                ImageIO.write((RenderedImage)canvas, "png", outputFile);
            }
            catch (IOException ex) {
                throw new RuntimeException("Error writing file: " + outputFile, ex);
            }
            finally {
                if (ios == null) continue;
                try {
                    ios.close();
                }
                catch (Exception exception) {}
            }
        }
    }

    private static void plot(BufferedImage dst, int x, int y, int argb) {
        if (0 <= x && x < dst.getWidth() && 0 <= y && y < dst.getHeight()) {
            dst.setRGB(x, y, argb);
        }
    }

    private static void copy(BufferedImage src, int x, int y, int w, int h, BufferedImage dst, int dx, int dy, boolean rotated) {
        if (rotated) {
            for (int i = 0; i < w; ++i) {
                for (int j = 0; j < h; ++j) {
                    TexturePacker.plot(dst, dx + j, dy + w - i - 1, src.getRGB(x + i, y + j));
                }
            }
        } else {
            for (int i = 0; i < w; ++i) {
                for (int j = 0; j < h; ++j) {
                    TexturePacker.plot(dst, dx + i, dy + j, src.getRGB(x + i, y + j));
                }
            }
        }
    }

    private void writePackFile(File outputDir, String scaledPackFileName, Array<Page> pages) throws IOException {
        File packFile = new File(outputDir, scaledPackFileName + this.settings.atlasExtension);
        File packDir = packFile.getParentFile();
        packDir.mkdirs();
        if (packFile.exists()) {
            TextureAtlas.TextureAtlasData textureAtlasData = new TextureAtlas.TextureAtlasData(new FileHandle(packFile), new FileHandle(packFile), false);
            for (Page page : pages) {
                for (Rect rect : page.outputRects) {
                    String rectName = Rect.getAtlasName(rect.name, this.settings.flattenPaths);
                    for (TextureAtlas.TextureAtlasData.Region region : textureAtlasData.getRegions()) {
                        if (!region.name.equals(rectName)) continue;
                        throw new GdxRuntimeException("A region with the name \"" + rectName + "\" has already been packed: " + rect.name);
                    }
                }
            }
        }
        OutputStreamWriter writer = new OutputStreamWriter((OutputStream)new FileOutputStream(packFile, true), "UTF-8");
        for (Page page : pages) {
            writer.write("\n" + page.imageName + "\n");
            writer.write("size: " + page.imageWidth + "," + page.imageHeight + "\n");
            writer.write("format: " + (Object)((Object)this.settings.format) + "\n");
            writer.write("filter: " + (Object)((Object)this.settings.filterMin) + "," + (Object)((Object)this.settings.filterMag) + "\n");
            writer.write("repeat: " + this.getRepeatValue() + "\n");
            page.outputRects.sort();
            for (Rect rect : page.outputRects) {
                this.writeRect(writer, page, rect, rect.name);
                Array<Object> aliases = new Array<Object>(rect.aliases.toArray());
                aliases.sort();
                for (Alias alias : aliases) {
                    Rect aliasRect = new Rect();
                    aliasRect.set(rect);
                    alias.apply(aliasRect);
                    this.writeRect(writer, page, aliasRect, alias.name);
                }
            }
        }
        ((Writer)writer).close();
    }

    private void writeRect(Writer writer, Page page, Rect rect, String name) throws IOException {
        writer.write(Rect.getAtlasName(name, this.settings.flattenPaths) + "\n");
        writer.write("  rotate: " + rect.rotated + "\n");
        writer.write("  xy: " + (page.x + rect.x) + ", " + (page.y + page.height - rect.height - rect.y) + "\n");
        writer.write("  size: " + rect.regionWidth + ", " + rect.regionHeight + "\n");
        if (rect.splits != null) {
            writer.write("  split: " + rect.splits[0] + ", " + rect.splits[1] + ", " + rect.splits[2] + ", " + rect.splits[3] + "\n");
        }
        if (rect.pads != null) {
            if (rect.splits == null) {
                writer.write("  split: 0, 0, 0, 0\n");
            }
            writer.write("  pad: " + rect.pads[0] + ", " + rect.pads[1] + ", " + rect.pads[2] + ", " + rect.pads[3] + "\n");
        }
        writer.write("  orig: " + rect.originalWidth + ", " + rect.originalHeight + "\n");
        writer.write("  offset: " + rect.offsetX + ", " + (rect.originalHeight - rect.regionHeight - rect.offsetY) + "\n");
        writer.write("  index: " + rect.index + "\n");
    }

    private String getRepeatValue() {
        if (this.settings.wrapX == Texture.TextureWrap.Repeat && this.settings.wrapY == Texture.TextureWrap.Repeat) {
            return "xy";
        }
        if (this.settings.wrapX == Texture.TextureWrap.Repeat && this.settings.wrapY == Texture.TextureWrap.ClampToEdge) {
            return "x";
        }
        if (this.settings.wrapX == Texture.TextureWrap.ClampToEdge && this.settings.wrapY == Texture.TextureWrap.Repeat) {
            return "y";
        }
        return "none";
    }

    private int getBufferedImageType(Pixmap.Format format) {
        switch (this.settings.format) {
            case RGBA8888: 
            case RGBA4444: {
                return 2;
            }
            case RGB565: 
            case RGB888: {
                return 1;
            }
            case Alpha: {
                return 10;
            }
        }
        throw new RuntimeException("Unsupported format: " + (Object)((Object)this.settings.format));
    }

    public static void process(String input, String output, String packFileName) {
        TexturePacker.process(new Settings(), input, output, packFileName);
    }

    public static void process(Settings settings, String input, String output, String packFileName) {
        try {
            TexturePackerFileProcessor processor = new TexturePackerFileProcessor(settings, packFileName);
            processor.setComparator(new Comparator<File>(){

                @Override
                public int compare(File file1, File file2) {
                    return file1.getName().compareTo(file2.getName());
                }
            });
            processor.process(new File(input), new File(output));
        }
        catch (Exception ex) {
            throw new RuntimeException("Error packing images.", ex);
        }
    }

    public static boolean isModified(String input, String output, String packFileName, Settings settings) {
        String packFullFileName = output;
        if (!packFullFileName.endsWith("/")) {
            packFullFileName = packFullFileName + "/";
        }
        packFullFileName = packFullFileName + packFileName;
        File outputFile = new File(packFullFileName = packFullFileName + settings.atlasExtension);
        if (!outputFile.exists()) {
            return true;
        }
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath());
        }
        return TexturePacker.isModified(inputFile, outputFile.lastModified());
    }

    private static boolean isModified(File file, long lastModified) {
        if (file.lastModified() > lastModified) {
            return true;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!TexturePacker.isModified(child, lastModified)) continue;
                return true;
            }
        }
        return false;
    }

    public static boolean processIfModified(String input, String output, String packFileName) {
        Settings settings = new Settings();
        if (TexturePacker.isModified(input, output, packFileName, settings)) {
            TexturePacker.process(settings, input, output, packFileName);
            return true;
        }
        return false;
    }

    public static boolean processIfModified(Settings settings, String input, String output, String packFileName) {
        if (TexturePacker.isModified(input, output, packFileName, settings)) {
            TexturePacker.process(settings, input, output, packFileName);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Settings settings = null;
        String input = null;
        String output = null;
        String packFileName = "pack.atlas";
        switch (args.length) {
            case 4: {
                settings = new Json().fromJson(Settings.class, new FileReader(args[3]));
            }
            case 3: {
                packFileName = args[2];
            }
            case 2: {
                output = args[1];
            }
            case 1: {
                input = args[0];
                break;
            }
            default: {
                System.out.println("Usage: inputDir [outputDir] [packFileName] [settingsFileName]");
                System.exit(0);
            }
        }
        if (output == null) {
            File inputFile = new File(input);
            output = new File(inputFile.getParentFile(), inputFile.getName() + "-packed").getAbsolutePath();
        }
        if (settings == null) {
            settings = new Settings();
        }
        TexturePacker.process(settings, input, output, packFileName);
    }

    static final class InputImage {
        File file;
        String name;
        BufferedImage image;

        InputImage() {
        }
    }

    public static interface Packer {
        public Array<Page> pack(Array<Rect> var1);
    }

    public static class Settings {
        public boolean pot = true;
        public int paddingX = 2;
        public int paddingY = 2;
        public boolean edgePadding = true;
        public boolean duplicatePadding = false;
        public boolean rotation;
        public int minWidth = 16;
        public int minHeight = 16;
        public int maxWidth = 1024;
        public int maxHeight = 1024;
        public boolean square = false;
        public boolean stripWhitespaceX;
        public boolean stripWhitespaceY;
        public int alphaThreshold;
        public Texture.TextureFilter filterMin = Texture.TextureFilter.Nearest;
        public Texture.TextureFilter filterMag = Texture.TextureFilter.Nearest;
        public Texture.TextureWrap wrapX = Texture.TextureWrap.ClampToEdge;
        public Texture.TextureWrap wrapY = Texture.TextureWrap.ClampToEdge;
        public Pixmap.Format format = Pixmap.Format.RGBA8888;
        public boolean alias = true;
        public String outputFormat = "png";
        public float jpegQuality = 0.9f;
        public boolean ignoreBlankImages = true;
        public boolean fast;
        public boolean debug;
        public boolean silent;
        public boolean combineSubdirectories;
        public boolean ignore;
        public boolean flattenPaths;
        public boolean premultiplyAlpha;
        public boolean useIndexes = true;
        public boolean bleed = true;
        public boolean limitMemory = true;
        public boolean grid;
        public float[] scale = new float[]{1.0f};
        public String[] scaleSuffix = new String[]{""};
        public String atlasExtension = ".atlas";

        public Settings() {
        }

        public Settings(Settings settings) {
            this.set(settings);
        }

        public void set(Settings settings) {
            this.fast = settings.fast;
            this.rotation = settings.rotation;
            this.pot = settings.pot;
            this.minWidth = settings.minWidth;
            this.minHeight = settings.minHeight;
            this.maxWidth = settings.maxWidth;
            this.maxHeight = settings.maxHeight;
            this.paddingX = settings.paddingX;
            this.paddingY = settings.paddingY;
            this.edgePadding = settings.edgePadding;
            this.duplicatePadding = settings.duplicatePadding;
            this.alphaThreshold = settings.alphaThreshold;
            this.ignoreBlankImages = settings.ignoreBlankImages;
            this.stripWhitespaceX = settings.stripWhitespaceX;
            this.stripWhitespaceY = settings.stripWhitespaceY;
            this.alias = settings.alias;
            this.format = settings.format;
            this.jpegQuality = settings.jpegQuality;
            this.outputFormat = settings.outputFormat;
            this.filterMin = settings.filterMin;
            this.filterMag = settings.filterMag;
            this.wrapX = settings.wrapX;
            this.wrapY = settings.wrapY;
            this.debug = settings.debug;
            this.silent = settings.silent;
            this.combineSubdirectories = settings.combineSubdirectories;
            this.ignore = settings.ignore;
            this.flattenPaths = settings.flattenPaths;
            this.premultiplyAlpha = settings.premultiplyAlpha;
            this.square = settings.square;
            this.useIndexes = settings.useIndexes;
            this.bleed = settings.bleed;
            this.limitMemory = settings.limitMemory;
            this.grid = settings.grid;
            this.scale = settings.scale;
            this.scaleSuffix = settings.scaleSuffix;
            this.atlasExtension = settings.atlasExtension;
        }

        public String getScaledPackFileName(String packFileName, int scaleIndex) {
            if (this.scaleSuffix[scaleIndex].length() > 0) {
                packFileName = packFileName + this.scaleSuffix[scaleIndex];
            } else {
                float scaleValue = this.scale[scaleIndex];
                if (this.scale.length != 1) {
                    packFileName = (scaleValue == (float)((int)scaleValue) ? Integer.toString((int)scaleValue) : Float.toString(scaleValue)) + "/" + packFileName;
                }
            }
            return packFileName;
        }
    }

    public static class Rect
    implements Comparable<Rect> {
        public String name;
        public int offsetX;
        public int offsetY;
        public int regionWidth;
        public int regionHeight;
        public int originalWidth;
        public int originalHeight;
        public int x;
        public int y;
        public int width;
        public int height;
        public int index;
        public boolean rotated;
        public Set<Alias> aliases = new HashSet<Alias>();
        public int[] splits;
        public int[] pads;
        public boolean canRotate = true;
        private boolean isPatch;
        private BufferedImage image;
        private File file;
        int score1;
        int score2;

        Rect(BufferedImage source, int left, int top, int newWidth, int newHeight, boolean isPatch) {
            this.image = new BufferedImage(source.getColorModel(), source.getRaster().createWritableChild(left, top, newWidth, newHeight, 0, 0, null), source.getColorModel().isAlphaPremultiplied(), null);
            this.offsetX = left;
            this.offsetY = top;
            this.regionWidth = newWidth;
            this.regionHeight = newHeight;
            this.originalWidth = source.getWidth();
            this.originalHeight = source.getHeight();
            this.width = newWidth;
            this.height = newHeight;
            this.isPatch = isPatch;
        }

        public void unloadImage(File file) {
            this.file = file;
            this.image = null;
        }

        public BufferedImage getImage(ImageProcessor imageProcessor) {
            BufferedImage image;
            if (this.image != null) {
                return this.image;
            }
            try {
                image = ImageIO.read(this.file);
            }
            catch (IOException ex) {
                throw new RuntimeException("Error reading image: " + this.file, ex);
            }
            if (image == null) {
                throw new RuntimeException("Unable to read image: " + this.file);
            }
            String name = this.name;
            if (this.isPatch) {
                name = name + ".9";
            }
            return imageProcessor.processImage(image, name).getImage(null);
        }

        Rect() {
        }

        Rect(Rect rect) {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
        }

        void set(Rect rect) {
            this.name = rect.name;
            this.image = rect.image;
            this.offsetX = rect.offsetX;
            this.offsetY = rect.offsetY;
            this.regionWidth = rect.regionWidth;
            this.regionHeight = rect.regionHeight;
            this.originalWidth = rect.originalWidth;
            this.originalHeight = rect.originalHeight;
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
            this.index = rect.index;
            this.rotated = rect.rotated;
            this.aliases = rect.aliases;
            this.splits = rect.splits;
            this.pads = rect.pads;
            this.canRotate = rect.canRotate;
            this.score1 = rect.score1;
            this.score2 = rect.score2;
            this.file = rect.file;
            this.isPatch = rect.isPatch;
        }

        @Override
        public int compareTo(Rect o) {
            return this.name.compareTo(o.name);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            Rect other = (Rect)obj;
            return !(this.name == null ? other.name != null : !this.name.equals(other.name));
        }

        public String toString() {
            return this.name + "[" + this.x + "," + this.y + " " + this.width + "x" + this.height + "]";
        }

        public static String getAtlasName(String name, boolean flattenPaths) {
            return flattenPaths ? new FileHandle(name).name() : name;
        }
    }

    public static class Alias
    implements Comparable<Alias> {
        public String name;
        public int index;
        public int[] splits;
        public int[] pads;
        public int offsetX;
        public int offsetY;
        public int originalWidth;
        public int originalHeight;

        public Alias(Rect rect) {
            this.name = rect.name;
            this.index = rect.index;
            this.splits = rect.splits;
            this.pads = rect.pads;
            this.offsetX = rect.offsetX;
            this.offsetY = rect.offsetY;
            this.originalWidth = rect.originalWidth;
            this.originalHeight = rect.originalHeight;
        }

        public void apply(Rect rect) {
            rect.name = this.name;
            rect.index = this.index;
            rect.splits = this.splits;
            rect.pads = this.pads;
            rect.offsetX = this.offsetX;
            rect.offsetY = this.offsetY;
            rect.originalWidth = this.originalWidth;
            rect.originalHeight = this.originalHeight;
        }

        @Override
        public int compareTo(Alias o) {
            return this.name.compareTo(o.name);
        }
    }

    public static class Page {
        public String imageName;
        public Array<Rect> outputRects;
        public Array<Rect> remainingRects;
        public float occupancy;
        public int x;
        public int y;
        public int width;
        public int height;
        public int imageWidth;
        public int imageHeight;
    }
}

