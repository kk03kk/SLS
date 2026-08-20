/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;

public class SimpleLoggerFactory
implements ILoggerFactory {
    ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<String, Logger>();

    public SimpleLoggerFactory() {
        SimpleLogger.lazyInit();
    }

    public Logger getLogger(String name) {
        Logger simpleLogger = (Logger)this.loggerMap.get(name);
        if (simpleLogger != null) {
            return simpleLogger;
        }
        SimpleLogger newInstance = new SimpleLogger(name);
        Logger oldInstance = this.loggerMap.putIfAbsent(name, newInstance);
        return oldInstance == null ? newInstance : oldInstance;
    }

    void reset() {
        this.loggerMap.clear();
    }
}

