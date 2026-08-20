/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.helpers.FileWriteValidationError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class File {
    private static final Logger logger = LogManager.getLogger(File.class.getName());
    private String filepath;
    private byte[] data;

    public File(String filepath, String data) {
        this.filepath = filepath;
        this.data = data.getBytes(StandardCharsets.UTF_8);
    }

    public String getFilepath() {
        return this.filepath;
    }

    public void save() {
        boolean success;
        int MAX_RETRIES = 5;
        String localStoragePath = Gdx.files.getLocalStoragePath();
        Path destination = FileSystems.getDefault().getPath(localStoragePath + this.filepath, new String[0]);
        Path backup = FileSystems.getDefault().getPath(localStoragePath + this.filepath + ".backUp", new String[0]);
        Path parent = destination.getParent();
        logger.debug("Attempting to save file=" + destination);
        if (Files.exists(parent, new LinkOption[0])) {
            if (Files.exists(destination, new LinkOption[0])) {
                File.copyAndValidate(destination, backup, 5);
                File.deleteFile(destination);
            }
        } else {
            try {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            catch (IOException e) {
                logger.info("Failed to create directory", (Throwable)e);
            }
        }
        if (success = File.writeAndValidate(destination, this.data, 5)) {
            logger.debug("Successfully saved file=" + destination.toString());
        }
    }

    private static void copyAndValidate(Path source, Path target, int retry) {
        byte[] sourceData = new byte[]{};
        try {
            sourceData = Files.readAllBytes(source);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            if (retry <= 0) {
                logger.info("Failed to copy " + source.toString() + " to " + target.toString() + ", but the retry expired", (Throwable)e);
                return;
            }
            logger.info("Failed to copy file=" + source.toString(), (Throwable)e);
            File.sleep(300);
            File.copyAndValidate(source, target, retry - 1);
        }
        Exception err = File.validateWrite(target, sourceData);
        if (err != null) {
            if (retry <= 0) {
                logger.info("Failed to copy " + source.toString() + " to " + target.toString() + ", but the retry expired", (Throwable)err);
                return;
            }
            logger.info("Failed to copy file=" + source.toString(), (Throwable)err);
            File.sleep(300);
            File.copyAndValidate(source, target, retry - 1);
        }
    }

    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e) {
            logger.info(e);
        }
    }

    private static void deleteFile(Path filepath) {
        try {
            Files.delete(filepath);
        }
        catch (IOException e) {
            logger.info("Failed to delete", (Throwable)e);
        }
    }

    private static Exception validateWrite(Path filepath, byte[] inMemoryBytes) {
        byte[] writtenBytes;
        try {
            writtenBytes = Files.readAllBytes(filepath);
        }
        catch (IOException e) {
            return e;
        }
        boolean valid = Arrays.equals(writtenBytes, inMemoryBytes);
        if (!valid) {
            return new FileWriteValidationError("Not valid: written=" + Arrays.toString(writtenBytes) + " vs inMemory=" + Arrays.toString(inMemoryBytes));
        }
        return null;
    }

    static boolean writeAndValidate(Path filepath, byte[] data, int retry) {
        try {
            Files.write(filepath, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        }
        catch (Exception ex) {
            if (retry <= 0) {
                logger.info("Failed to write file " + filepath.toString() + ", but the retry expired.", (Throwable)ex);
                return false;
            }
            logger.info("Failed to validate source=" + filepath.toString() + ", retrying...", (Throwable)ex);
            File.sleep(300);
            return File.writeAndValidate(filepath, data, retry - 1);
        }
        Exception err = File.validateWrite(filepath, data);
        if (err != null) {
            if (retry <= 0) {
                logger.info("Failed to write file " + filepath.toString() + ", but the retry expired.", (Throwable)err);
                return false;
            }
            logger.info("Failed to validate source=" + filepath.toString() + ", retrying...", (Throwable)err);
            File.sleep(300);
            return File.writeAndValidate(filepath, data, retry - 1);
        }
        return true;
    }
}

