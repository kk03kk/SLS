/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.exceptions;

public class SaveFileLoadError
extends Exception {
    private static final long serialVersionUID = 1L;

    public SaveFileLoadError() {
    }

    public SaveFileLoadError(String message) {
        super(message);
    }

    public SaveFileLoadError(String message, Throwable cause) {
        super(message, cause);
    }

    public SaveFileLoadError(Throwable cause) {
        super(cause);
    }
}

