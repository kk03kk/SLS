/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamRemoteStorageCallbackAdapter;
import java.nio.ByteBuffer;

final class SteamRemoteStorageNative {
    SteamRemoteStorageNative() {
    }

    static native long createCallback(SteamRemoteStorageCallbackAdapter var0);

    static native boolean fileWrite(String var0, ByteBuffer var1, int var2, int var3);

    static native int fileRead(String var0, ByteBuffer var1, int var2, int var3);

    static native long fileWriteAsync(long var0, String var2, ByteBuffer var3, int var4, int var5);

    static native long fileReadAsync(long var0, String var2, int var3, int var4);

    static native boolean fileReadAsyncComplete(long var0, ByteBuffer var2, long var3, int var5);

    static native boolean fileForget(String var0);

    static native boolean fileDelete(String var0);

    static native long fileShare(long var0, String var2);

    static native boolean setSyncPlatforms(String var0, int var1);

    static native long fileWriteStreamOpen(String var0);

    static native boolean fileWriteStreamWriteChunk(long var0, ByteBuffer var2, int var3, int var4);

    static native boolean fileWriteStreamClose(long var0);

    static native boolean fileWriteStreamCancel(long var0);

    static native boolean fileExists(String var0);

    static native boolean filePersisted(String var0);

    static native int getFileSize(String var0);

    static native long getFileTimestamp(String var0);

    static native int getSyncPlatforms(String var0);

    static native int getFileCount();

    static native String getFileNameAndSize(int var0, int[] var1);

    static native boolean getQuota(long[] var0, long[] var1);

    static native boolean isCloudEnabledForAccount();

    static native boolean isCloudEnabledForApp();

    static native void setCloudEnabledForApp(boolean var0);

    static native long ugcDownload(long var0, long var2, int var4);

    static native boolean getUGCDownloadProgress(long var0, int[] var2, int[] var3);

    static native int ugcRead(long var0, ByteBuffer var2, int var3, int var4, int var5, int var6);

    static native int getCachedUGCCount();

    static native long getCachedUGCHandle(int var0);

    static native long publishWorkshopFile(long var0, String var2, String var3, int var4, String var5, String var6, int var7, String[] var8, int var9, int var10);

    static native long createPublishedFileUpdateRequest(long var0);

    static native boolean updatePublishedFileFile(long var0, String var2);

    static native boolean updatePublishedFilePreviewFile(long var0, String var2);

    static native boolean updatePublishedFileTitle(long var0, String var2);

    static native boolean updatePublishedFileDescription(long var0, String var2);

    static native boolean updatePublishedFileVisibility(long var0, int var2);

    static native boolean updatePublishedFileTags(long var0, String[] var2, int var3);

    static native long commitPublishedFileUpdate(long var0, long var2);
}

