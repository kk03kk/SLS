/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.async;

import java.util.concurrent.BlockingQueue;

public interface BlockingQueueFactory<E> {
    public static final String ELEMENT_TYPE = "BlockingQueueFactory";

    public BlockingQueue<E> create(int var1);
}

