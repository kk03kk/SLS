/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.helpers.File;
import com.megacrit.cardcrawl.helpers.FileSaver;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncSaver {
    private static final Logger logger = LogManager.getLogger(AsyncSaver.class.getName());
    private static Thread saveThread;
    private static final BlockingQueue<File> saveQueue;

    public static void save(String filepath, String data) {
        boolean enableAsyncSave = true;
        if (enableAsyncSave) {
            logger.debug("Enqueue: qsize=" + saveQueue.size() + " file=" + filepath);
            saveQueue.add(new File(filepath, data));
            AsyncSaver.ensureSaveThread();
        } else {
            logger.info("Saving synchronously");
            File saveFile = new File(filepath, data);
            saveFile.save();
        }
    }

    private static void ensureSaveThread() {
        if (saveThread == null) {
            AsyncSaver.startSaveThread();
        } else if (!saveThread.isAlive()) {
            logger.info("Save thread is dead. Starting save thread!");
            AsyncSaver.startSaveThread();
        }
    }

    private static void startSaveThread() {
        saveThread = new Thread(new FileSaver(saveQueue));
        saveThread.setName("FileSaver");
        saveThread.start();
    }

    public static void shutdownSaveThread() {
        if (saveThread != null) {
            saveThread.interrupt();
        }
    }

    static {
        saveQueue = new LinkedBlockingQueue<File>();
    }
}

