/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.core.config.plugins.convert;

public interface TypeConverter<T> {
    public T convert(String var1) throws Exception;
}

