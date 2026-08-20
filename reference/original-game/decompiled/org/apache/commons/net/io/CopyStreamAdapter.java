/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.util.EventListener;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.util.ListenerList;

public class CopyStreamAdapter
implements CopyStreamListener {
    private final ListenerList internalListeners = new ListenerList();

    @Override
    public void bytesTransferred(CopyStreamEvent event) {
        for (EventListener listener : this.internalListeners) {
            ((CopyStreamListener)listener).bytesTransferred(event);
        }
    }

    @Override
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
        for (EventListener listener : this.internalListeners) {
            ((CopyStreamListener)listener).bytesTransferred(totalBytesTransferred, bytesTransferred, streamSize);
        }
    }

    public void addCopyStreamListener(CopyStreamListener listener) {
        this.internalListeners.addListener(listener);
    }

    public void removeCopyStreamListener(CopyStreamListener listener) {
        this.internalListeners.removeListener(listener);
    }
}

