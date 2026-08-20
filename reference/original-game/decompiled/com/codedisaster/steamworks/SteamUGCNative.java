/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamUGC;
import com.codedisaster.steamworks.SteamUGCCallbackAdapter;
import com.codedisaster.steamworks.SteamUGCDetails;

final class SteamUGCNative {
    SteamUGCNative() {
    }

    static native long createCallback(SteamUGCCallbackAdapter var0);

    static native long createQueryUserUGCRequest(int var0, int var1, int var2, int var3, int var4, int var5, int var6);

    static native long createQueryAllUGCRequest(int var0, int var1, int var2, int var3, int var4);

    static native long createQueryUGCDetailsRequest(long[] var0, int var1);

    static native long sendQueryUGCRequest(long var0, long var2);

    static native boolean getQueryUGCResult(long var0, int var2, SteamUGCDetails var3);

    static native String getQueryUGCPreviewURL(long var0, int var2);

    static native String getQueryUGCMetadata(long var0, int var2);

    static native long getQueryUGCStatistic(long var0, int var2, int var3);

    static native int getQueryUGCNumAdditionalPreviews(long var0, int var2);

    static native boolean getQueryUGCAdditionalPreview(long var0, int var2, int var3, SteamUGC.ItemAdditionalPreview var4);

    static native int getQueryUGCNumKeyValueTags(long var0, int var2);

    static native boolean getQueryUGCKeyValueTag(long var0, int var2, int var3, String[] var4);

    static native boolean releaseQueryUserUGCRequest(long var0);

    static native boolean addRequiredTag(long var0, String var2);

    static native boolean addExcludedTag(long var0, String var2);

    static native boolean setReturnOnlyIDs(long var0, boolean var2);

    static native boolean setReturnKeyValueTags(long var0, boolean var2);

    static native boolean setReturnLongDescription(long var0, boolean var2);

    static native boolean setReturnMetadata(long var0, boolean var2);

    static native boolean setReturnChildren(long var0, boolean var2);

    static native boolean setReturnAdditionalPreviews(long var0, boolean var2);

    static native boolean setReturnTotalOnly(long var0, boolean var2);

    static native boolean setReturnPlaytimeStats(long var0, int var2);

    static native boolean setLanguage(long var0, String var2);

    static native boolean setAllowCachedResponse(long var0, int var2);

    static native boolean setCloudFileNameFilter(long var0, String var2);

    static native boolean setMatchAnyTag(long var0, boolean var2);

    static native boolean setSearchText(long var0, String var2);

    static native boolean setRankedByTrendDays(long var0, int var2);

    static native boolean addRequiredKeyValueTag(long var0, String var2, String var3);

    static native long requestUGCDetails(long var0, long var2, int var4);

    static native long createItem(long var0, int var2, int var3);

    static native long startItemUpdate(int var0, long var1);

    static native boolean setItemTitle(long var0, String var2);

    static native boolean setItemDescription(long var0, String var2);

    static native boolean setItemUpdateLanguage(long var0, String var2);

    static native boolean setItemMetadata(long var0, String var2);

    static native boolean setItemVisibility(long var0, int var2);

    static native boolean setItemTags(long var0, String[] var2, int var3);

    static native boolean setItemContent(long var0, String var2);

    static native boolean setItemPreview(long var0, String var2);

    static native boolean removeItemKeyValueTags(long var0, String var2);

    static native boolean addItemKeyValueTag(long var0, String var2, String var3);

    static native long submitItemUpdate(long var0, long var2, String var4);

    static native int getItemUpdateProgress(long var0, long[] var2);

    static native long setUserItemVote(long var0, long var2, boolean var4);

    static native long getUserItemVote(long var0, long var2);

    static native long addItemToFavorites(long var0, int var2, long var3);

    static native long removeItemFromFavorites(long var0, int var2, long var3);

    static native long subscribeItem(long var0, long var2);

    static native long unsubscribeItem(long var0, long var2);

    static native int getNumSubscribedItems();

    static native int getSubscribedItems(long[] var0, int var1);

    static native int getItemState(long var0);

    static native boolean getItemInstallInfo(long var0, SteamUGC.ItemInstallInfo var2);

    static native boolean getItemDownloadInfo(long var0, long[] var2);

    static native long deleteItem(long var0, long var2);

    static native boolean downloadItem(long var0, boolean var2);

    static native boolean initWorkshopForGameServer(int var0, String var1);

    static native void suspendDownloads(boolean var0);

    static native long startPlaytimeTracking(long var0, long[] var2, int var3);

    static native long stopPlaytimeTracking(long var0, long[] var2, int var3);

    static native long stopPlaytimeTrackingForAllItems(long var0);
}

