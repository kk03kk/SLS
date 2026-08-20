/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

public class ParserInitializationException
extends RuntimeException {
    private static final long serialVersionUID = 5563335279583210658L;

    public ParserInitializationException(String message) {
        super(message);
    }

    public ParserInitializationException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    @Deprecated
    public Throwable getRootCause() {
        return super.getCause();
    }
}

