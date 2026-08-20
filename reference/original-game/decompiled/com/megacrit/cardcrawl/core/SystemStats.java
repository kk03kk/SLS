/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import java.io.File;
import java.text.NumberFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemStats {
    private static final Logger logger = LogManager.getLogger(SystemStats.class.getName());
    private static final String diskRoot = "/";

    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getUnallocatedMemory() {
        return SystemStats.getMaxMemory() - SystemStats.getAllocatedMemory();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static long getUsedMemory() {
        return SystemStats.getAllocatedMemory() - SystemStats.getFreeMemory();
    }

    public static long getTotalFreeMemory() {
        return SystemStats.getFreeMemory() + (SystemStats.getMaxMemory() - SystemStats.getAllocatedMemory());
    }

    private static long toMb(long sizeInBytes) {
        return sizeInBytes / 1024L / 1024L;
    }

    private static String prettyBytes(long sizeInBytes, NumberFormat format) {
        return format.format(SystemStats.toMb(sizeInBytes));
    }

    public static void logMemoryStats() {
        StringBuilder sb = new StringBuilder();
        NumberFormat format = NumberFormat.getInstance();
        sb.append("Free Memory: ").append(SystemStats.prettyBytes(SystemStats.getFreeMemory(), format)).append("Mb\n");
        sb.append("Max Memory: ").append(SystemStats.prettyBytes(SystemStats.getMaxMemory(), format)).append("Mb\n");
        sb.append("Allocated Memory: ").append(SystemStats.prettyBytes(SystemStats.getAllocatedMemory(), format)).append("Mb\n");
        sb.append("Unallocated Memory: ").append(SystemStats.prettyBytes(SystemStats.getUnallocatedMemory(), format)).append("Mb\n");
        sb.append("Total Free Memory: ").append(SystemStats.prettyBytes(SystemStats.getTotalFreeMemory(), format)).append("Mb\n");
        sb.append("Used Memory: ").append(SystemStats.prettyBytes(SystemStats.getUsedMemory(), format)).append("Mb\n");
        logger.info("MEMORY STATS:\n" + sb.toString());
    }

    public static long getDiskSpaceTotal() {
        return new File(diskRoot).getTotalSpace();
    }

    public static long getDiskSpaceFree() {
        return new File(diskRoot).getFreeSpace();
    }

    public static long getDiskSpaceUsable() {
        return new File(diskRoot).getUsableSpace();
    }

    public static void logDiskStats() {
        StringBuilder sb = new StringBuilder();
        NumberFormat format = NumberFormat.getInstance();
        sb.append("Total Space: ").append(SystemStats.prettyBytes(SystemStats.getDiskSpaceTotal(), format)).append("Mb\n");
        sb.append("Usable Space: ").append(SystemStats.prettyBytes(SystemStats.getDiskSpaceUsable(), format)).append("Mb\n");
        sb.append("Free Space: ").append(SystemStats.prettyBytes(SystemStats.getDiskSpaceFree(), format)).append("Mb\n");
        logger.info("DISK STATS:\n" + sb.toString());
    }
}

