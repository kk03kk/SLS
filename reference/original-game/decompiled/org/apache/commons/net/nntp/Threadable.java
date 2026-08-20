/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

public interface Threadable {
    public boolean isDummy();

    public String messageThreadId();

    public String[] messageThreadReferences();

    public String simplifiedSubject();

    public boolean subjectIsReply();

    public void setChild(Threadable var1);

    public void setNext(Threadable var1);

    public Threadable makeDummy();
}

