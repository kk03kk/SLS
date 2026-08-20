/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamPublishedFileID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUGCCallback;
import com.codedisaster.steamworks.SteamUGCDetails;
import com.codedisaster.steamworks.SteamUGCQuery;

class SteamUGCCallbackAdapter
extends SteamCallbackAdapter<SteamUGCCallback> {
    SteamUGCCallbackAdapter(SteamUGCCallback callback) {
        super(callback);
    }

    void onUGCQueryCompleted(long handle, int numResultsReturned, int totalMatchingResults, boolean isCachedData, int result) {
        ((SteamUGCCallback)this.callback).onUGCQueryCompleted(new SteamUGCQuery(handle), numResultsReturned, totalMatchingResults, isCachedData, SteamResult.byValue(result));
    }

    void onSubscribeItem(long publishedFileID, int result) {
        ((SteamUGCCallback)this.callback).onSubscribeItem(new SteamPublishedFileID(publishedFileID), SteamResult.byValue(result));
    }

    void onUnsubscribeItem(long publishedFileID, int result) {
        ((SteamUGCCallback)this.callback).onUnsubscribeItem(new SteamPublishedFileID(publishedFileID), SteamResult.byValue(result));
    }

    void onRequestUGCDetails(long publishedFileID, int result, String title, String description, long fileHandle, long previewFileHandle, String fileName, boolean cachedData, int votesUp, int votesDown, long ownerID, int timeCreated, int timeUpdated) {
        SteamUGCDetails details = new SteamUGCDetails();
        details.publishedFileID = publishedFileID;
        details.result = result;
        details.title = title;
        details.description = description;
        details.fileHandle = fileHandle;
        details.previewFileHandle = previewFileHandle;
        details.fileName = fileName;
        details.votesUp = votesUp;
        details.votesDown = votesDown;
        details.ownerID = ownerID;
        details.timeCreated = timeCreated;
        details.timeUpdated = timeUpdated;
        ((SteamUGCCallback)this.callback).onRequestUGCDetails(details, SteamResult.byValue(result));
    }

    void onCreateItem(long publishedFileID, boolean needsToAcceptWLA, int result) {
        ((SteamUGCCallback)this.callback).onCreateItem(new SteamPublishedFileID(publishedFileID), needsToAcceptWLA, SteamResult.byValue(result));
    }

    void onSubmitItemUpdate(long publishedFileID, boolean needsToAcceptWLA, int result) {
        ((SteamUGCCallback)this.callback).onSubmitItemUpdate(new SteamPublishedFileID(publishedFileID), needsToAcceptWLA, SteamResult.byValue(result));
    }

    void onDownloadItemResult(int appID, long publishedFileID, int result) {
        ((SteamUGCCallback)this.callback).onDownloadItemResult(appID, new SteamPublishedFileID(publishedFileID), SteamResult.byValue(result));
    }

    void onUserFavoriteItemsListChanged(long publishedFileID, boolean wasAddRequest, int result) {
        ((SteamUGCCallback)this.callback).onUserFavoriteItemsListChanged(new SteamPublishedFileID(publishedFileID), wasAddRequest, SteamResult.byValue(result));
    }

    void onSetUserItemVote(long publishedFileID, boolean voteUp, int result) {
        ((SteamUGCCallback)this.callback).onSetUserItemVote(new SteamPublishedFileID(publishedFileID), voteUp, SteamResult.byValue(result));
    }

    void onGetUserItemVote(long publishedFileID, boolean votedUp, boolean votedDown, boolean voteSkipped, int result) {
        ((SteamUGCCallback)this.callback).onGetUserItemVote(new SteamPublishedFileID(publishedFileID), votedUp, votedDown, voteSkipped, SteamResult.byValue(result));
    }

    void onStartPlaytimeTracking(int result) {
        ((SteamUGCCallback)this.callback).onStartPlaytimeTracking(SteamResult.byValue(result));
    }

    void onStopPlaytimeTracking(int result) {
        ((SteamUGCCallback)this.callback).onStopPlaytimeTracking(SteamResult.byValue(result));
    }

    void onStopPlaytimeTrackingForAllItems(int result) {
        ((SteamUGCCallback)this.callback).onStopPlaytimeTrackingForAllItems(SteamResult.byValue(result));
    }

    void onDeleteItem(long publishedFileID, int result) {
        ((SteamUGCCallback)this.callback).onDeleteItem(new SteamPublishedFileID(publishedFileID), SteamResult.byValue(result));
    }
}

