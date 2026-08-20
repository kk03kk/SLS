/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class LwjglClipboard
implements com.badlogic.gdx.utils.Clipboard,
ClipboardOwner {
    @Override
    public String getContents() {
        String result = "";
        try {
            boolean hasTransferableText;
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            boolean bl = hasTransferableText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    result = (String)contents.getTransferData(DataFlavor.stringFlavor);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return result;
    }

    @Override
    public void setContents(String content) {
        try {
            StringSelection stringSelection = new StringSelection(content);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Override
    public void lostOwnership(Clipboard arg0, Transferable arg1) {
    }
}

