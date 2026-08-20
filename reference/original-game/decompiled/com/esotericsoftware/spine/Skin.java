/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import java.util.Iterator;

public class Skin {
    private static final Key lookup = new Key();
    final String name;
    final ObjectMap<Key, Attachment> attachments = new ObjectMap();
    final Pool<Key> keyPool = new Pool(64){

        protected Object newObject() {
            return new Key();
        }
    };

    public Skin(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        this.name = name;
    }

    public void addAttachment(int slotIndex, String name, Attachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("attachment cannot be null.");
        }
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0.");
        }
        Key key = this.keyPool.obtain();
        key.set(slotIndex, name);
        this.attachments.put(key, attachment);
    }

    public Attachment getAttachment(int slotIndex, String name) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0.");
        }
        lookup.set(slotIndex, name);
        return this.attachments.get(lookup);
    }

    public void findNamesForSlot(int slotIndex, Array<String> names) {
        if (names == null) {
            throw new IllegalArgumentException("names cannot be null.");
        }
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0.");
        }
        for (Key key : this.attachments.keys()) {
            if (key.slotIndex != slotIndex) continue;
            names.add(key.name);
        }
    }

    public void findAttachmentsForSlot(int slotIndex, Array<Attachment> attachments) {
        if (attachments == null) {
            throw new IllegalArgumentException("attachments cannot be null.");
        }
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0.");
        }
        for (ObjectMap.Entry entry : this.attachments.entries()) {
            if (((Key)entry.key).slotIndex != slotIndex) continue;
            attachments.add((Attachment)entry.value);
        }
    }

    public void clear() {
        for (Key key : this.attachments.keys()) {
            this.keyPool.free(key);
        }
        this.attachments.clear();
    }

    public String getName() {
        return this.name;
    }

    public Iterator<Attachment> attachments() {
        return this.attachments.values().iterator();
    }

    public String toString() {
        return this.name;
    }

    void attachAll(Skeleton skeleton, Skin oldSkin) {
        for (ObjectMap.Entry entry : oldSkin.attachments.entries()) {
            Attachment attachment;
            int slotIndex = ((Key)entry.key).slotIndex;
            Slot slot = skeleton.slots.get(slotIndex);
            if (slot.attachment != entry.value || (attachment = this.getAttachment(slotIndex, ((Key)entry.key).name)) == null) continue;
            slot.setAttachment(attachment);
        }
    }

    static class Key {
        int slotIndex;
        String name;
        int hashCode;

        Key() {
        }

        public void set(int slotIndex, String name) {
            if (name == null) {
                throw new IllegalArgumentException("name cannot be null.");
            }
            this.slotIndex = slotIndex;
            this.name = name;
            this.hashCode = 31 * (31 + name.hashCode()) + slotIndex;
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            Key other = (Key)object;
            if (this.slotIndex != other.slotIndex) {
                return false;
            }
            return this.name.equals(other.name);
        }

        public String toString() {
            return this.slotIndex + ":" + this.name;
        }
    }
}

