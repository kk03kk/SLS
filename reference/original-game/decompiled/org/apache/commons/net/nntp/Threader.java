/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.net.nntp.ThreadContainer;
import org.apache.commons.net.nntp.Threadable;

public class Threader {
    public Threadable thread(List<? extends Threadable> messages) {
        return this.thread((Iterable<? extends Threadable>)messages);
    }

    /*
     * WARNING - void declaration
     */
    public Threadable thread(Iterable<? extends Threadable> messages) {
        void var4_6;
        if (messages == null) {
            return null;
        }
        HashMap<String, ThreadContainer> idTable = new HashMap<String, ThreadContainer>();
        for (Threadable threadable : messages) {
            if (threadable.isDummy()) continue;
            this.buildContainer(threadable, idTable);
        }
        if (idTable.isEmpty()) {
            return null;
        }
        ThreadContainer root = this.findRootSet(idTable);
        idTable.clear();
        idTable = null;
        this.pruneEmptyContainers(root);
        root.reverseChildren();
        this.gatherSubjects(root);
        if (root.next != null) {
            throw new RuntimeException("root node has a next:" + root);
        }
        ThreadContainer threadContainer = root.child;
        while (var4_6 != null) {
            if (var4_6.threadable == null) {
                var4_6.threadable = var4_6.child.threadable.makeDummy();
            }
            ThreadContainer threadContainer2 = var4_6.next;
        }
        Threadable threadable = root.child == null ? null : root.child.threadable;
        root.flush();
        return threadable;
    }

    private void buildContainer(Threadable threadable, HashMap<String, ThreadContainer> idTable) {
        String[] references;
        String id = threadable.messageThreadId();
        ThreadContainer container = idTable.get(id);
        int bogusIdCount = 0;
        if (container != null) {
            if (container.threadable != null) {
                id = "<Bogus-id:" + ++bogusIdCount + ">";
                container = null;
            } else {
                container.threadable = threadable;
            }
        }
        if (container == null) {
            container = new ThreadContainer();
            container.threadable = threadable;
            idTable.put(id, container);
        }
        ThreadContainer parentRef = null;
        for (String refString : references = threadable.messageThreadReferences()) {
            ThreadContainer ref = idTable.get(refString);
            if (ref == null) {
                ref = new ThreadContainer();
                idTable.put(refString, ref);
            }
            if (parentRef != null && ref.parent == null && parentRef != ref && !ref.findChild(parentRef)) {
                ref.parent = parentRef;
                ref.next = parentRef.child;
                parentRef.child = ref;
            }
            parentRef = ref;
        }
        if (parentRef != null && (parentRef == container || container.findChild(parentRef))) {
            parentRef = null;
        }
        if (container.parent != null) {
            ThreadContainer prev = null;
            ThreadContainer rest = container.parent.child;
            while (rest != null && rest != container) {
                prev = rest;
                rest = rest.next;
            }
            if (rest == null) {
                throw new RuntimeException("Didnt find " + container + " in parent" + container.parent);
            }
            if (prev == null) {
                container.parent.child = container.next;
            } else {
                prev.next = container.next;
            }
            container.next = null;
            container.parent = null;
        }
        if (parentRef != null) {
            container.parent = parentRef;
            container.next = parentRef.child;
            parentRef.child = container;
        }
    }

    private ThreadContainer findRootSet(HashMap<String, ThreadContainer> idTable) {
        ThreadContainer root = new ThreadContainer();
        for (Map.Entry<String, ThreadContainer> entry : idTable.entrySet()) {
            ThreadContainer c = entry.getValue();
            if (c.parent != null) continue;
            if (c.next != null) {
                throw new RuntimeException("c.next is " + c.next.toString());
            }
            c.next = root.child;
            root.child = c;
        }
        return root;
    }

    private void pruneEmptyContainers(ThreadContainer parent) {
        ThreadContainer prev = null;
        ThreadContainer container = parent.child;
        ThreadContainer next = container.next;
        while (container != null) {
            if (container.threadable == null && container.child == null) {
                if (prev == null) {
                    parent.child = container.next;
                } else {
                    prev.next = container.next;
                }
                container = prev;
            } else if (container.threadable == null && container.child != null && (container.parent != null || container.child.next == null)) {
                ThreadContainer kids = container.child;
                if (prev == null) {
                    parent.child = kids;
                } else {
                    prev.next = kids;
                }
                ThreadContainer tail = kids;
                while (tail.next != null) {
                    tail.parent = container.parent;
                    tail = tail.next;
                }
                tail.parent = container.parent;
                tail.next = container.next;
                next = kids;
                container = prev;
            } else if (container.child != null) {
                this.pruneEmptyContainers(container);
            }
            prev = container;
            container = next;
            next = container == null ? null : container.next;
        }
    }

    private void gatherSubjects(ThreadContainer root) {
        int count = 0;
        ThreadContainer c = root.child;
        while (c != null) {
            ++count;
            c = c.next;
        }
        HashMap<String, ThreadContainer> subjectTable = new HashMap<String, ThreadContainer>((int)((double)count * 1.2), 0.9f);
        count = 0;
        ThreadContainer c2 = root.child;
        while (c2 != null) {
            ThreadContainer old;
            String subj;
            Threadable threadable = c2.threadable;
            if (threadable == null) {
                threadable = c2.child.threadable;
            }
            if ((subj = threadable.simplifiedSubject()) != null && subj.length() != 0 && ((old = (ThreadContainer)subjectTable.get(subj)) == null || c2.threadable == null && old.threadable != null || old.threadable != null && old.threadable.subjectIsReply() && c2.threadable != null && !c2.threadable.subjectIsReply())) {
                subjectTable.put(subj, c2);
                ++count;
            }
            c2 = c2.next;
        }
        if (count == 0) {
            return;
        }
        ThreadContainer prev = null;
        ThreadContainer c3 = root.child;
        ThreadContainer rest = c3.next;
        while (c3 != null) {
            ThreadContainer old;
            String subj;
            Threadable threadable = c3.threadable;
            if (threadable == null) {
                threadable = c3.child.threadable;
            }
            if ((subj = threadable.simplifiedSubject()) != null && subj.length() != 0 && (old = (ThreadContainer)subjectTable.get(subj)) != c3) {
                if (prev == null) {
                    root.child = c3.next;
                } else {
                    prev.next = c3.next;
                }
                c3.next = null;
                if (old.threadable == null && c3.threadable == null) {
                    ThreadContainer tail = old.child;
                    while (tail != null && tail.next != null) {
                        tail = tail.next;
                    }
                    if (tail != null) {
                        tail.next = c3.child;
                    }
                    tail = c3.child;
                    while (tail != null) {
                        tail.parent = old;
                        tail = tail.next;
                    }
                    c3.child = null;
                } else if (old.threadable == null || c3.threadable != null && c3.threadable.subjectIsReply() && !old.threadable.subjectIsReply()) {
                    c3.parent = old;
                    c3.next = old.child;
                    old.child = c3;
                } else {
                    ThreadContainer newc = new ThreadContainer();
                    newc.threadable = old.threadable;
                    ThreadContainer tail = newc.child = old.child;
                    while (tail != null) {
                        tail.parent = newc;
                        tail = tail.next;
                    }
                    old.threadable = null;
                    old.child = null;
                    c3.parent = old;
                    newc.parent = old;
                    old.child = c3;
                    c3.next = newc;
                }
                c3 = prev;
            }
            prev = c3;
            c3 = rest;
            rest = rest == null ? null : rest.next;
        }
        subjectTable.clear();
        subjectTable = null;
    }

    @Deprecated
    public Threadable thread(Threadable[] messages) {
        if (messages == null) {
            return null;
        }
        return this.thread(Arrays.asList(messages));
    }
}

