/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.SimpleLoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder
implements LoggerFactoryBinder {
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    public static String REQUESTED_API_VERSION = "1.6.99";
    private static final String loggerFactoryClassStr = SimpleLoggerFactory.class.getName();
    private final ILoggerFactory loggerFactory = new SimpleLoggerFactory();

    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    private StaticLoggerBinder() {
    }

    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}

