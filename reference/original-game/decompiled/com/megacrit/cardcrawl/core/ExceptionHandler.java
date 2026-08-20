/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import org.apache.logging.log4j.Logger;

public class ExceptionHandler {
    public static void handleException(Exception e, Logger logger) {
        logger.error("Exception caught", (Throwable)e);
    }
}

