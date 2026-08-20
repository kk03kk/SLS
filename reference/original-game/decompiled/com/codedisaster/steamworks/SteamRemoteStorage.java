/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAPICall;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamInterface;
import com.codedisaster.steamworks.SteamPublishedFileID;
import com.codedisaster.steamworks.SteamPublishedFileUpdateHandle;
import com.codedisaster.steamworks.SteamRemoteStorageCallback;
import com.codedisaster.steamworks.SteamRemoteStorageCallbackAdapter;
import com.codedisaster.steamworks.SteamRemoteStorageNative;
import com.codedisaster.steamworks.SteamUGCFileWriteStreamHandle;
import com.codedisaster.steamworks.SteamUGCHandle;
import java.nio.ByteBuffer;

public class SteamRemoteStorage
extends SteamInterface {
    public SteamRemoteStorage(SteamRemoteStorageCallback callback) {
        super(SteamRemoteStorageNative.createCallback(new SteamRemoteStorageCallbackAdapter(callback)));
    }

    public boolean fileWrite(String file, ByteBuffer data) throws SteamException {
        this.checkBuffer(data);
        return SteamRemoteStorageNative.fileWrite(file, data, data.position(), data.remaining());
    }

    public int fileRead(String file, ByteBuffer buffer) throws SteamException {
        this.checkBuffer(buffer);
        return SteamRemoteStorageNative.fileRead(file, buffer, buffer.position(), buffer.remaining());
    }

    public SteamAPICall fileWriteAsync(String file, ByteBuffer data) throws SteamException {
        this.checkBuffer(data);
        return new SteamAPICall(SteamRemoteStorageNative.fileWriteAsync(this.callback, file, data, data.position(), data.remaining()));
    }

    public SteamAPICall fileReadAsync(String file, int offset, int toRead) {
        return new SteamAPICall(SteamRemoteStorageNative.fileReadAsync(this.callback, file, offset, toRead));
    }

    public boolean fileReadAsyncComplete(SteamAPICall readCall, ByteBuffer buffer, int toRead) {
        return SteamRemoteStorageNative.fileReadAsyncComplete(readCall.handle, buffer, buffer.position(), toRead);
    }

    public boolean fileForget(String file) {
        return SteamRemoteStorageNative.fileForget(file);
    }

    public boolean fileDelete(String file) {
        return SteamRemoteStorageNative.fileDelete(file);
    }

    public SteamAPICall fileShare(String file) {
        return new SteamAPICall(SteamRemoteStorageNative.fileShare(this.callback, file));
    }

    public boolean setSyncPlatforms(String file, RemoteStoragePlatform remoteStoragePlatform) {
        return SteamRemoteStorageNative.setSyncPlatforms(file, remoteStoragePlatform.mask);
    }

    public SteamUGCFileWriteStreamHandle fileWriteStreamOpen(String name) {
        return new SteamUGCFileWriteStreamHandle(SteamRemoteStorageNative.fileWriteStreamOpen(name));
    }

    public boolean fileWriteStreamWriteChunk(SteamUGCFileWriteStreamHandle stream, ByteBuffer data) {
        return SteamRemoteStorageNative.fileWriteStreamWriteChunk(stream.handle, data, data.position(), data.remaining());
    }

    public boolean fileWriteStreamClose(SteamUGCFileWriteStreamHandle stream) {
        return SteamRemoteStorageNative.fileWriteStreamClose(stream.handle);
    }

    public boolean fileWriteStreamCancel(SteamUGCFileWriteStreamHandle stream) {
        return SteamRemoteStorageNative.fileWriteStreamCancel(stream.handle);
    }

    public boolean fileExists(String file) {
        return SteamRemoteStorageNative.fileExists(file);
    }

    public boolean filePersisted(String file) {
        return SteamRemoteStorageNative.filePersisted(file);
    }

    public int getFileSize(String file) {
        return SteamRemoteStorageNative.getFileSize(file);
    }

    public long getFileTimestamp(String file) {
        return SteamRemoteStorageNative.getFileTimestamp(file);
    }

    public RemoteStoragePlatform[] getSyncPlatforms(String file) {
        int mask = SteamRemoteStorageNative.getSyncPlatforms(file);
        return RemoteStoragePlatform.byMask(mask);
    }

    public int getFileCount() {
        return SteamRemoteStorageNative.getFileCount();
    }

    public String getFileNameAndSize(int index, int[] sizes) {
        return SteamRemoteStorageNative.getFileNameAndSize(index, sizes);
    }

    public boolean getQuota(long[] totalBytes, long[] availableBytes) {
        return SteamRemoteStorageNative.getQuota(totalBytes, availableBytes);
    }

    public boolean isCloudEnabledForAccount() {
        return SteamRemoteStorageNative.isCloudEnabledForAccount();
    }

    public boolean isCloudEnabledForApp() {
        return SteamRemoteStorageNative.isCloudEnabledForApp();
    }

    public void setCloudEnabledForApp(boolean enabled) {
        SteamRemoteStorageNative.setCloudEnabledForApp(enabled);
    }

    public SteamAPICall ugcDownload(SteamUGCHandle fileHandle, int priority) {
        return new SteamAPICall(SteamRemoteStorageNative.ugcDownload(this.callback, fileHandle.handle, priority));
    }

    public boolean getUGCDownloadProgress(SteamUGCHandle fileHandle, int[] bytesDownloaded, int[] bytesExpected) {
        return SteamRemoteStorageNative.getUGCDownloadProgress(fileHandle.handle, bytesDownloaded, bytesExpected);
    }

    public int ugcRead(SteamUGCHandle fileHandle, ByteBuffer buffer, int dataToRead, int offset, UGCReadAction action) {
        return SteamRemoteStorageNative.ugcRead(fileHandle.handle, buffer, buffer.position(), dataToRead, offset, action.ordinal());
    }

    public int getCachedUGCCount() {
        return SteamRemoteStorageNative.getCachedUGCCount();
    }

    public SteamUGCHandle getCachedUGCHandle(int cachedContent) {
        return new SteamUGCHandle(SteamRemoteStorageNative.getCachedUGCHandle(cachedContent));
    }

    public SteamAPICall publishWorkshopFile(String file, String previewFile, int consumerAppID, String title, String description, PublishedFileVisibility visibility, String[] tags, WorkshopFileType workshopFileType) {
        return new SteamAPICall(SteamRemoteStorageNative.publishWorkshopFile(this.callback, file, previewFile, consumerAppID, title, description, visibility.ordinal(), tags, tags != null ? tags.length : 0, workshopFileType.ordinal()));
    }

    public SteamPublishedFileUpdateHandle createPublishedFileUpdateRequest(SteamPublishedFileID publishedFileID) {
        return new SteamPublishedFileUpdateHandle(SteamRemoteStorageNative.createPublishedFileUpdateRequest(publishedFileID.handle));
    }

    public boolean updatePublishedFileFile(SteamPublishedFileUpdateHandle updateHandle, String file) {
        return SteamRemoteStorageNative.updatePublishedFileFile(updateHandle.handle, file);
    }

    public boolean updatePublishedFilePreviewFile(SteamPublishedFileUpdateHandle updateHandle, String previewFile) {
        return SteamRemoteStorageNative.updatePublishedFilePreviewFile(updateHandle.handle, previewFile);
    }

    public boolean updatePublishedFileTitle(SteamPublishedFileUpdateHandle updateHandle, String title) {
        return SteamRemoteStorageNative.updatePublishedFileTitle(updateHandle.handle, title);
    }

    public boolean updatePublishedFileDescription(SteamPublishedFileUpdateHandle updateHandle, String description) {
        return SteamRemoteStorageNative.updatePublishedFileDescription(updateHandle.handle, description);
    }

    public boolean updatePublishedFileVisibility(SteamPublishedFileUpdateHandle updateHandle, PublishedFileVisibility visibility) {
        return SteamRemoteStorageNative.updatePublishedFileVisibility(updateHandle.handle, visibility.ordinal());
    }

    public boolean updatePublishedFileTags(SteamPublishedFileUpdateHandle updateHandle, String[] tags) {
        return SteamRemoteStorageNative.updatePublishedFileTags(updateHandle.handle, tags, tags != null ? tags.length : 0);
    }

    public SteamAPICall commitPublishedFileUpdate(SteamPublishedFileUpdateHandle updateHandle) {
        return new SteamAPICall(SteamRemoteStorageNative.commitPublishedFileUpdate(this.callback, updateHandle.handle));
    }

    public static enum WorkshopFileType {
        Community,
        Microtransaction,
        Collection,
        Art,
        Video,
        Screenshot,
        Game,
        Software,
        Concept,
        WebGuide,
        IntegratedGuide,
        Merch,
        ControllerBinding,
        SteamworksAccessInvite,
        SteamVideo,
        GameManagedItem;

        private static final WorkshopFileType[] values;

        static WorkshopFileType byOrdinal(int ordinal) {
            return values[ordinal];
        }

        static {
            values = WorkshopFileType.values();
        }
    }

    public static enum PublishedFileVisibility {
        Public,
        FriendsOnly,
        Private;

    }

    public static enum UGCReadAction {
        ContinueReadingUntilFinished,
        ContinueReading,
        Close;

    }

    public static enum RemoteStoragePlatform {
        None(0),
        Windows(1),
        OSX(2),
        PS3(4),
        Linux(8),
        Reserved2(16),
        Android(32),
        IOS(64),
        All(-1);

        private final int mask;
        private static final RemoteStoragePlatform[] values;

        private RemoteStoragePlatform(int mask) {
            this.mask = mask;
        }

        static RemoteStoragePlatform[] byMask(int mask) {
            int bits = Integer.bitCount(mask);
            RemoteStoragePlatform[] result = new RemoteStoragePlatform[bits];
            int idx = 0;
            for (RemoteStoragePlatform value : values) {
                if ((value.mask & mask) == 0) continue;
                result[idx++] = value;
            }
            return result;
        }

        static {
            values = RemoteStoragePlatform.values();
        }
    }
}

