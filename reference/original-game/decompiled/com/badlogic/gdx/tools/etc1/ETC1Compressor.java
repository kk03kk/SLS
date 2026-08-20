/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.etc1;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ETC1;
import com.badlogic.gdx.tools.FileProcessor;
import com.badlogic.gdx.utils.GdxNativesLoader;
import java.io.File;
import java.util.ArrayList;

public class ETC1Compressor {
    public static void process(String inputDirectory, String outputDirectory, boolean recursive, boolean flatten) throws Exception {
        GdxNativesLoader.load();
        ETC1FileProcessor processor = new ETC1FileProcessor();
        processor.setRecursive(recursive);
        processor.setFlattenOutput(flatten);
        processor.process(new File(inputDirectory), new File(outputDirectory));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("ETC1Compressor <input-dir> <output-dir>");
            System.exit(-1);
        }
        ETC1Compressor.process(args[0], args[1], true, false);
    }

    static class ETC1FileProcessor
    extends FileProcessor {
        ETC1FileProcessor() {
            this.addInputSuffix(".png");
            this.addInputSuffix(".jpg");
            this.addInputSuffix(".jpeg");
            this.addInputSuffix(".bmp");
            this.setOutputSuffix(".etc1");
        }

        @Override
        protected void processFile(FileProcessor.Entry entry) throws Exception {
            System.out.println("Processing " + entry.inputFile);
            Pixmap pixmap = new Pixmap(new FileHandle(entry.inputFile));
            if (pixmap.getFormat() != Pixmap.Format.RGB888 && pixmap.getFormat() != Pixmap.Format.RGB565) {
                System.out.println("Converting from " + (Object)((Object)pixmap.getFormat()) + " to RGB888!");
                Pixmap tmp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGB888);
                tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
                pixmap.dispose();
                pixmap = tmp;
            }
            ETC1.encodeImagePKM(pixmap).write(new FileHandle(entry.outputFile));
            pixmap.dispose();
        }

        @Override
        protected void processDir(FileProcessor.Entry entryDir, ArrayList<FileProcessor.Entry> value) throws Exception {
            if (!entryDir.outputDir.exists() && !entryDir.outputDir.mkdirs()) {
                throw new Exception("Couldn't create output directory '" + entryDir.outputDir + "'");
            }
        }
    }
}

