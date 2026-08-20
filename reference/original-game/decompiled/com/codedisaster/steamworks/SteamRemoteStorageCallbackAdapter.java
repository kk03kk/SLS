/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAPICall;
import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamPublishedFileID;
import com.codedisaster.steamworks.SteamRemoteStorageCallback;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUGCHandle;

class SteamRemoteStorageCallbackAdapter
extends SteamCallbackAdapter<SteamRemoteStorageCallback> {
    SteamRemoteStorageCallbackAdapter(SteamRemoteStorageCallback callback) {
        super(callback);
    }

    void onFileShareResult(long fileHandle, String fileName, int result) {
        ((SteamRemoteStorageCallback)this.callback).onFileShareResult(new SteamUGCHandle(fileHandle), fileName, SteamResult.byValue(result));
    }

    void onDownloadUGCResult(long fileHandle, int result) {
        ((SteamRemoteStorageCallback)this.callback).onDownloadUGCResult(new SteamUGCHandle(fileHandle), SteamResult.byValue(result));
    }

    void onPublishFileResult(long publishedFileID, boolean needsToAcceptWLA, int result) {
        ((SteamRemoteStorageCallback)this.callback).onPublishFileResult(new SteamPublishedFileID(publishedFileID), needsToAcceptWLA, SteamResult.byValue(result));
    }

    void onUpdatePublishedFileResult(long publishedFileID, boolean needsToAcceptWLA, int result) {
        ((SteamRemoteStorageCallback)this.callback).onUpdatePublishedFileResult(new SteamPublishedFileID(publishedFileID), needsToAcceptWLA, SteamResult.byValue(result));
    }

    void onPublishedFileSubscribed(long publishedFileID, int appID) {
        ((SteamRemoteStorageCallback)this.callback).onPublishedFileSubscribed(new SteamPublishedFileID(publishedFileID), appID);
    }

    void onPublishedFileUnsubscribed(long publishedFileID, int appID) {
        ((SteamRemoteStorageCallback)this.callback).onPublishedFileUnsubscribed(new SteamPublishedFileID(publishedFileID), appID);
    }

    void onPublishedFileDeleted(long publishedFileID, int appID) {
        ((SteamRemoteStorageCallback)this.callback).onPublishedFileDeleted(new SteamPublishedFileID(publishedFileID), appID);
    }

    void onFileWriteAsyncComplete(int result) {
        ((SteamRemoteStorageCallback)this.callback).onFileWriteAsyncComplete(SteamResult.byValue(result));
    }

    void onFileReadAsyncComplete(long fileReadAsync, int result, int offset, int read) {
        ((SteamRemoteStorageCallback)this.callback).onFileReadAsyncComplete(new SteamAPICall(fileReadAsync), SteamResult.byValue(result), offset, read);
    }
}

