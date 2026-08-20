/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.lookup;

import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

public final class RuntimeStrSubstitutor
extends StrSubstitutor {
    public RuntimeStrSubstitutor() {
    }

    public RuntimeStrSubstitutor(Map<String, String> valueMap) {
        super(valueMap);
    }

    public RuntimeStrSubstitutor(Properties properties) {
        super(properties);
    }

    public RuntimeStrSubstitutor(StrLookup lookup) {
        super(lookup);
    }

    public RuntimeStrSubstitutor(StrSubstitutor other) {
        super(other);
    }

    @Override
    boolean isRecursiveEvaluationAllowed() {
        return false;
    }

    @Override
    void setRecursiveEvaluationAllowed(boolean recursiveEvaluationAllowed) {
        throw new UnsupportedOperationException("recursiveEvaluationAllowed cannot be modified within RuntimeStrSubstitutor");
    }

    @Override
    public String toString() {
        return "RuntimeStrSubstitutor{" + super.toString() + "}";
    }
}

