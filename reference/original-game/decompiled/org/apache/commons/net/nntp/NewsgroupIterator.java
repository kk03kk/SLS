/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import java.util.Iterator;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;

class NewsgroupIterator
implements Iterator<NewsgroupInfo>,
Iterable<NewsgroupInfo> {
    private final Iterator<String> stringIterator;

    public NewsgroupIterator(Iterable<String> iterableString) {
        this.stringIterator = iterableString.iterator();
    }

    @Override
    public boolean hasNext() {
        return this.stringIterator.hasNext();
    }

    @Override
    public NewsgroupInfo next() {
        String line = this.stringIterator.next();
        return NNTPClient.__parseNewsgroupListEntry(line);
    }

    @Override
    public void remove() {
        this.stringIterator.remove();
    }

    @Override
    public Iterator<NewsgroupInfo> iterator() {
        return this;
    }
}

