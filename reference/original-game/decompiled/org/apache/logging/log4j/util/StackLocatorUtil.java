/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.util;

import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Predicate;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StackLocator;

public final class StackLocatorUtil {
    private static StackLocator stackLocator = null;
    private static volatile boolean errorLogged;

    private StackLocatorUtil() {
    }

    @PerformanceSensitive
    public static Class<?> getCallerClass(int depth) {
        return stackLocator.getCallerClass(depth + 1);
    }

    public static StackTraceElement getStackTraceElement(int depth) {
        return stackLocator.getStackTraceElement(depth + 1);
    }

    @PerformanceSensitive
    public static Class<?> getCallerClass(String fqcn) {
        return StackLocatorUtil.getCallerClass(fqcn, "");
    }

    @PerformanceSensitive
    public static Class<?> getCallerClass(String fqcn, String pkg) {
        return stackLocator.getCallerClass(fqcn, pkg);
    }

    @PerformanceSensitive
    public static Class<?> getCallerClass(Class<?> sentinelClass, Predicate<Class<?>> callerPredicate) {
        return stackLocator.getCallerClass(sentinelClass, callerPredicate);
    }

    @PerformanceSensitive
    public static Class<?> getCallerClass(Class<?> anchor) {
        return stackLocator.getCallerClass(anchor);
    }

    @PerformanceSensitive
    public static Stack<Class<?>> getCurrentStackTrace() {
        return stackLocator.getCurrentStackTrace();
    }

    public static StackTraceElement calcLocation(String fqcnOfLogger) {
        try {
            return stackLocator.calcLocation(fqcnOfLogger);
        }
        catch (NoSuchElementException ex) {
            if (!errorLogged) {
                errorLogged = true;
                StatusLogger.getLogger().warn("Unable to locate stack trace element for {}", (Object)fqcnOfLogger, (Object)ex);
            }
            return null;
        }
    }

    static {
        stackLocator = StackLocator.getInstance();
    }
}

