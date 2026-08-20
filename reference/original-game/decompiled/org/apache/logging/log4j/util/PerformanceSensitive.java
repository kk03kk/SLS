/*
 * Decompiled with CFR 0.152.
 */
package org.apache.logging.log4j.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value=RetentionPolicy.CLASS)
public @interface PerformanceSensitive {
    public String[] value() default {""};
}

