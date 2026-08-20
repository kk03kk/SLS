/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import java.io.IOException;

public final class NNTPConnectionClosedException
extends IOException {
    private static final long serialVersionUID = 1029785635891040770L;

    public NNTPConnectionClosedException() {
    }

    public NNTPConnectionClosedException(String message) {
        super(message);
    }
}

