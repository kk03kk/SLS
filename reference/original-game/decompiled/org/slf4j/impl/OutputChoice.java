/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.impl;

import java.io.PrintStream;

class OutputChoice {
    final OutputChoiceType outputChoiceType;
    final PrintStream targetPrintStream;

    OutputChoice(OutputChoiceType outputChoiceType) {
        if (outputChoiceType == OutputChoiceType.FILE) {
            throw new IllegalArgumentException();
        }
        this.outputChoiceType = outputChoiceType;
        this.targetPrintStream = outputChoiceType == OutputChoiceType.CACHED_SYS_OUT ? System.out : (outputChoiceType == OutputChoiceType.CACHED_SYS_ERR ? System.err : null);
    }

    OutputChoice(PrintStream printStream) {
        this.outputChoiceType = OutputChoiceType.FILE;
        this.targetPrintStream = printStream;
    }

    PrintStream getTargetPrintStream() {
        switch (this.outputChoiceType) {
            case SYS_OUT: {
                return System.out;
            }
            case SYS_ERR: {
                return System.err;
            }
            case CACHED_SYS_ERR: 
            case CACHED_SYS_OUT: 
            case FILE: {
                return this.targetPrintStream;
            }
        }
        throw new IllegalArgumentException();
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    static enum OutputChoiceType {
        SYS_OUT,
        CACHED_SYS_OUT,
        SYS_ERR,
        CACHED_SYS_ERR,
        FILE;

    }
}

