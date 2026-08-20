/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;

public interface LoggerContext {
    public Object getExternalContext();

    default public Object getObject(String key) {
        return null;
    }

    default public Object putObject(String key, Object value) {
        return null;
    }

    default public Object putObjectIfAbsent(String key, Object value) {
        return null;
    }

    default public Object removeObject(String key) {
        return null;
    }

    default public boolean removeObject(String key, Object value) {
        return false;
    }

    public ExtendedLogger getLogger(String var1);

    default public ExtendedLogger getLogger(Class<?> cls) {
        String canonicalName = cls.getCanonicalName();
        return this.getLogger(canonicalName != null ? canonicalName : cls.getName());
    }

    public ExtendedLogger getLogger(String var1, MessageFactory var2);

    default public ExtendedLogger getLogger(Class<?> cls, MessageFactory messageFactory) {
        String canonicalName = cls.getCanonicalName();
        return this.getLogger(canonicalName != null ? canonicalName : cls.getName(), messageFactory);
    }

    public boolean hasLogger(String var1);

    public boolean hasLogger(String var1, MessageFactory var2);

    public boolean hasLogger(String var1, Class<? extends MessageFactory> var2);
}

