/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.megacrit.cardcrawl.helpers.File;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSaver
implements Runnable {
    private static final Logger logger = LogManager.getLogger(FileSaver.class.getName());
    private final BlockingQueue<File> queue;

    public FileSaver(BlockingQueue<File> q) {
        this.queue = q;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.consume(this.queue.take());
            }
            catch (InterruptedException e) {
                logger.info("Save thread interrupted!");
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Save thread will die now.");
    }

    private void consume(File file) {
        logger.debug("Dequeue: qsize=" + this.queue.size() + " file=" + file.getFilepath());
        file.save();
    }
}

