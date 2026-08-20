/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.lookup;

import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

public final class ConfigurationStrSubstitutor
extends StrSubstitutor {
    public ConfigurationStrSubstitutor() {
    }

    public ConfigurationStrSubstitutor(Map<String, String> valueMap) {
        super(valueMap);
    }

    public ConfigurationStrSubstitutor(Properties properties) {
        super(properties);
    }

    public ConfigurationStrSubstitutor(StrLookup lookup) {
        super(lookup);
    }

    public ConfigurationStrSubstitutor(StrSubstitutor other) {
        super(other);
    }

    @Override
    boolean isRecursiveEvaluationAllowed() {
        return true;
    }

    @Override
    void setRecursiveEvaluationAllowed(boolean recursiveEvaluationAllowed) {
        throw new UnsupportedOperationException("recursiveEvaluationAllowed cannot be modified within ConfigurationStrSubstitutor");
    }

    @Override
    public String toString() {
        return "ConfigurationStrSubstitutor{" + super.toString() + "}";
    }
}

