/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net;

import java.io.Serializable;
import java.util.EventListener;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.util.ListenerList;

public class ProtocolCommandSupport
implements Serializable {
    private static final long serialVersionUID = -8017692739988399978L;
    private final Object __source;
    private final ListenerList __listeners = new ListenerList();

    public ProtocolCommandSupport(Object source) {
        this.__source = source;
    }

    public void fireCommandSent(String command, String message) {
        ProtocolCommandEvent event = new ProtocolCommandEvent(this.__source, command, message);
        for (EventListener listener : this.__listeners) {
            ((ProtocolCommandListener)listener).protocolCommandSent(event);
        }
    }

    public void fireReplyReceived(int replyCode, String message) {
        ProtocolCommandEvent event = new ProtocolCommandEvent(this.__source, replyCode, message);
        for (EventListener listener : this.__listeners) {
            ((ProtocolCommandListener)listener).protocolReplyReceived(event);
        }
    }

    public void addProtocolCommandListener(ProtocolCommandListener listener) {
        this.__listeners.addListener(listener);
    }

    public void removeProtocolCommandListener(ProtocolCommandListener listener) {
        this.__listeners.removeListener(listener);
    }

    public int getListenerCount() {
        return this.__listeners.getListenerCount();
    }
}

