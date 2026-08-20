/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.texturepacker;

import com.badlogic.gdx.tools.FileProcessor;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.ObjectMap;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TexturePackerFileProcessor
extends FileProcessor {
    private final TexturePacker.Settings defaultSettings;
    private ObjectMap<File, TexturePacker.Settings> dirToSettings = new ObjectMap();
    private Json json = new Json();
    private String packFileName;
    private File root;
    ArrayList<File> ignoreDirs = new ArrayList();

    public TexturePackerFileProcessor() {
        this(new TexturePacker.Settings(), "pack.atlas");
    }

    public TexturePackerFileProcessor(TexturePacker.Settings defaultSettings, String packFileName) {
        this.defaultSettings = defaultSettings;
        if (packFileName.toLowerCase().endsWith(defaultSettings.atlasExtension.toLowerCase())) {
            packFileName = packFileName.substring(0, packFileName.length() - defaultSettings.atlasExtension.length());
        }
        this.packFileName = packFileName;
        this.setFlattenOutput(true);
        this.addInputSuffix(".png", ".jpg", ".jpeg");
    }

    @Override
    public ArrayList<FileProcessor.Entry> process(File inputFile, File outputRoot) throws Exception {
        this.root = inputFile;
        final ArrayList settingsFiles = new ArrayList();
        FileProcessor settingsProcessor = new FileProcessor(){

            @Override
            protected void processFile(FileProcessor.Entry inputFile) throws Exception {
                settingsFiles.add(inputFile.inputFile);
            }
        };
        settingsProcessor.addInputRegex("pack\\.json");
        settingsProcessor.process(inputFile, null);
        Collections.sort(settingsFiles, new Comparator<File>(){

            @Override
            public int compare(File file1, File file2) {
                return file1.toString().length() - file2.toString().length();
            }
        });
        for (File settingsFile : settingsFiles) {
            TexturePacker.Settings settings = null;
            File parent = settingsFile.getParentFile();
            while (!parent.equals(this.root)) {
                settings = this.dirToSettings.get(parent = parent.getParentFile());
                if (settings == null) continue;
                settings = new TexturePacker.Settings(settings);
                break;
            }
            if (settings == null) {
                settings = new TexturePacker.Settings(this.defaultSettings);
            }
            this.merge(settings, settingsFile);
            this.dirToSettings.put(settingsFile.getParentFile(), settings);
        }
        return super.process(inputFile, outputRoot);
    }

    private void merge(TexturePacker.Settings settings, File settingsFile) {
        try {
            this.json.readFields(settings, new JsonReader().parse(new FileReader(settingsFile)));
        }
        catch (Exception ex) {
            throw new GdxRuntimeException("Error reading settings file: " + settingsFile, ex);
        }
    }

    @Override
    public ArrayList<FileProcessor.Entry> process(File[] files, File outputRoot) throws Exception {
        if (outputRoot.exists()) {
            File settingsFile = new File(this.root, "pack.json");
            TexturePacker.Settings rootSettings = this.defaultSettings;
            if (settingsFile.exists()) {
                rootSettings = new TexturePacker.Settings(rootSettings);
                this.merge(rootSettings, settingsFile);
            }
            String atlasExtension = rootSettings.atlasExtension == null ? "" : rootSettings.atlasExtension;
            atlasExtension = Pattern.quote(atlasExtension);
            int n = rootSettings.scale.length;
            for (int i = 0; i < n; ++i) {
                FileProcessor deleteProcessor = new FileProcessor(){

                    @Override
                    protected void processFile(FileProcessor.Entry inputFile) throws Exception {
                        inputFile.inputFile.delete();
                    }
                };
                deleteProcessor.setRecursive(false);
                String scaledPackFileName = rootSettings.getScaledPackFileName(this.packFileName, i);
                File packFile = new File(scaledPackFileName);
                String prefix = packFile.getName();
                int dotIndex = prefix.lastIndexOf(46);
                if (dotIndex != -1) {
                    prefix = prefix.substring(0, dotIndex);
                }
                deleteProcessor.addInputRegex("(?i)" + prefix + "\\d*\\.(png|jpg|jpeg)");
                deleteProcessor.addInputRegex("(?i)" + prefix + atlasExtension);
                String dir = packFile.getParent();
                if (dir == null) {
                    deleteProcessor.process(outputRoot, null);
                    continue;
                }
                if (!new File(outputRoot + "/" + dir).exists()) continue;
                deleteProcessor.process(outputRoot + "/" + dir, null);
            }
        }
        return super.process(files, outputRoot);
    }

    @Override
    protected void processDir(FileProcessor.Entry inputDir, ArrayList<FileProcessor.Entry> files) throws Exception {
        if (this.ignoreDirs.contains(inputDir.inputFile)) {
            return;
        }
        TexturePacker.Settings settings = null;
        for (File parent = inputDir.inputFile; (settings = this.dirToSettings.get(parent)) == null && parent != null && !parent.equals(this.root); parent = parent.getParentFile()) {
        }
        if (settings == null) {
            settings = this.defaultSettings;
        }
        if (settings.ignore) {
            return;
        }
        if (settings.combineSubdirectories) {
            files = new FileProcessor(this){

                @Override
                protected void processDir(FileProcessor.Entry entryDir, ArrayList<FileProcessor.Entry> files) {
                    TexturePackerFileProcessor.this.ignoreDirs.add(entryDir.inputFile);
                }

                @Override
                protected void processFile(FileProcessor.Entry entry) {
                    this.addProcessedFile(entry);
                }
            }.process(inputDir.inputFile, null);
        }
        if (files.isEmpty()) {
            return;
        }
        Collections.sort(files, new Comparator<FileProcessor.Entry>(){
            final Pattern digitSuffix = Pattern.compile("(.*?)(\\d+)$");

            @Override
            public int compare(FileProcessor.Entry entry1, FileProcessor.Entry entry2) {
                int compare;
                String full2;
                String full1 = entry1.inputFile.getName();
                int dotIndex = full1.lastIndexOf(46);
                if (dotIndex != -1) {
                    full1 = full1.substring(0, dotIndex);
                }
                if ((dotIndex = (full2 = entry2.inputFile.getName()).lastIndexOf(46)) != -1) {
                    full2 = full2.substring(0, dotIndex);
                }
                String name1 = full1;
                String name2 = full2;
                int num1 = 0;
                int num2 = 0;
                Matcher matcher = this.digitSuffix.matcher(full1);
                if (matcher.matches()) {
                    try {
                        num1 = Integer.parseInt(matcher.group(2));
                        name1 = matcher.group(1);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if ((matcher = this.digitSuffix.matcher(full2)).matches()) {
                    try {
                        num2 = Integer.parseInt(matcher.group(2));
                        name2 = matcher.group(1);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if ((compare = name1.compareTo(name2)) != 0 || num1 == num2) {
                    return compare;
                }
                return num1 - num2;
            }
        });
        if (!settings.silent) {
            System.out.println(inputDir.inputFile.getName());
        }
        TexturePacker packer = new TexturePacker(this.root, settings);
        for (FileProcessor.Entry file : files) {
            packer.addImage(file.inputFile);
        }
        packer.pack(inputDir.outputDir, this.packFileName);
    }
}

