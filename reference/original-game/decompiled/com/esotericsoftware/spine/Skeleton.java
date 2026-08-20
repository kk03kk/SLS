/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.IkConstraint;
import com.esotericsoftware.spine.IkConstraintData;
import com.esotericsoftware.spine.PathConstraint;
import com.esotericsoftware.spine.PathConstraintData;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.SlotData;
import com.esotericsoftware.spine.TransformConstraint;
import com.esotericsoftware.spine.TransformConstraintData;
import com.esotericsoftware.spine.Updatable;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public class Skeleton {
    final SkeletonData data;
    final Array<Bone> bones;
    final Array<Slot> slots;
    Array<Slot> drawOrder;
    final Array<IkConstraint> ikConstraints;
    final Array<IkConstraint> ikConstraintsSorted;
    final Array<TransformConstraint> transformConstraints;
    final Array<PathConstraint> pathConstraints;
    final Array<Updatable> updateCache = new Array();
    Skin skin;
    final Color color;
    float time;
    boolean flipX;
    boolean flipY;
    float x;
    float y;

    public Skeleton(SkeletonData data) {
        Bone bone;
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null.");
        }
        this.data = data;
        this.bones = new Array(data.bones.size);
        for (BoneData boneData : data.bones) {
            if (boneData.parent == null) {
                bone = new Bone(boneData, this, null);
            } else {
                Bone parent = this.bones.get(boneData.parent.index);
                bone = new Bone(boneData, this, parent);
                parent.children.add(bone);
            }
            this.bones.add(bone);
        }
        this.slots = new Array(data.slots.size);
        this.drawOrder = new Array(data.slots.size);
        for (SlotData slotData : data.slots) {
            bone = this.bones.get(slotData.boneData.index);
            Slot slot = new Slot(slotData, bone);
            this.slots.add(slot);
            this.drawOrder.add(slot);
        }
        this.ikConstraints = new Array(data.ikConstraints.size);
        this.ikConstraintsSorted = new Array(this.ikConstraints.size);
        for (IkConstraintData ikConstraintData : data.ikConstraints) {
            this.ikConstraints.add(new IkConstraint(ikConstraintData, this));
        }
        this.transformConstraints = new Array(data.transformConstraints.size);
        for (TransformConstraintData transformConstraintData : data.transformConstraints) {
            this.transformConstraints.add(new TransformConstraint(transformConstraintData, this));
        }
        this.pathConstraints = new Array(data.pathConstraints.size);
        for (PathConstraintData pathConstraintData : data.pathConstraints) {
            this.pathConstraints.add(new PathConstraint(pathConstraintData, this));
        }
        this.color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        this.updateCache();
    }

    public Skeleton(Skeleton skeleton) {
        if (skeleton == null) {
            throw new IllegalArgumentException("skeleton cannot be null.");
        }
        this.data = skeleton.data;
        this.bones = new Array(skeleton.bones.size);
        for (Bone bone : skeleton.bones) {
            Bone copy;
            if (bone.parent == null) {
                copy = new Bone(bone, this, null);
            } else {
                Bone parent = this.bones.get(bone.parent.data.index);
                copy = new Bone(bone, this, parent);
                parent.children.add(copy);
            }
            this.bones.add(copy);
        }
        this.slots = new Array(skeleton.slots.size);
        for (Slot slot : skeleton.slots) {
            Bone bone = this.bones.get(slot.bone.data.index);
            this.slots.add(new Slot(slot, bone));
        }
        this.drawOrder = new Array(this.slots.size);
        for (Slot slot : skeleton.drawOrder) {
            this.drawOrder.add(this.slots.get(slot.data.index));
        }
        this.ikConstraints = new Array(skeleton.ikConstraints.size);
        this.ikConstraintsSorted = new Array(this.ikConstraints.size);
        for (IkConstraint ikConstraint : skeleton.ikConstraints) {
            this.ikConstraints.add(new IkConstraint(ikConstraint, this));
        }
        this.transformConstraints = new Array(skeleton.transformConstraints.size);
        for (TransformConstraint transformConstraint : skeleton.transformConstraints) {
            this.transformConstraints.add(new TransformConstraint(transformConstraint, this));
        }
        this.pathConstraints = new Array(skeleton.pathConstraints.size);
        for (PathConstraint pathConstraint : skeleton.pathConstraints) {
            this.pathConstraints.add(new PathConstraint(pathConstraint, this));
        }
        this.skin = skeleton.skin;
        this.color = new Color(skeleton.color);
        this.time = skeleton.time;
        this.flipX = skeleton.flipX;
        this.flipY = skeleton.flipY;
        this.updateCache();
    }

    public void updateCache() {
        int i;
        int i2;
        Array<Updatable> updateCache = this.updateCache;
        updateCache.clear();
        Array<Bone> bones = this.bones;
        int n = bones.size;
        for (int i3 = 0; i3 < n; ++i3) {
            bones.get((int)i3).sorted = false;
        }
        Array<IkConstraint> ikConstraints = this.ikConstraintsSorted;
        ikConstraints.clear();
        ikConstraints.addAll(this.ikConstraints);
        int ikCount = ikConstraints.size;
        int n2 = ikCount;
        for (i2 = 0; i2 < n2; ++i2) {
            IkConstraint ik = ikConstraints.get(i2);
            Bone bone = ik.bones.first().parent;
            int level = 0;
            while (bone != null) {
                bone = bone.parent;
                ++level;
            }
            ik.level = level;
        }
        for (i2 = 1; i2 < ikCount; ++i2) {
            int ii;
            IkConstraint ik = ikConstraints.get(i2);
            int level = ik.level;
            for (ii = i2 - 1; ii >= 0; --ii) {
                IkConstraint other = ikConstraints.get(ii);
                if (other.level < level) break;
                ikConstraints.set(ii + 1, other);
            }
            ikConstraints.set(ii + 1, ik);
        }
        int n3 = ikConstraints.size;
        for (i2 = 0; i2 < n3; ++i2) {
            IkConstraint constraint = ikConstraints.get(i2);
            Bone target = constraint.target;
            this.sortBone(target);
            Array<Bone> constrained = constraint.bones;
            Bone parent = constrained.first();
            this.sortBone(parent);
            updateCache.add(constraint);
            this.sortReset(parent.children);
            constrained.peek().sorted = true;
        }
        Array<PathConstraint> pathConstraints = this.pathConstraints;
        n2 = pathConstraints.size;
        for (int i4 = 0; i4 < n2; ++i4) {
            int ii;
            PathConstraint constraint = pathConstraints.get(i4);
            Slot slot = constraint.target;
            int slotIndex = slot.getData().index;
            Bone slotBone = slot.bone;
            if (this.skin != null) {
                this.sortPathConstraintAttachment(this.skin, slotIndex, slotBone);
            }
            if (this.data.defaultSkin != null && this.data.defaultSkin != this.skin) {
                this.sortPathConstraintAttachment(this.data.defaultSkin, slotIndex, slotBone);
            }
            int nn = this.data.skins.size;
            for (int ii2 = 0; ii2 < nn; ++ii2) {
                this.sortPathConstraintAttachment(this.data.skins.get(ii2), slotIndex, slotBone);
            }
            Attachment attachment = slot.attachment;
            if (attachment instanceof PathAttachment) {
                this.sortPathConstraintAttachment(attachment, slotBone);
            }
            Array<Bone> constrained = constraint.bones;
            int boneCount = constrained.size;
            for (ii = 0; ii < boneCount; ++ii) {
                this.sortBone(constrained.get(ii));
            }
            updateCache.add(constraint);
            for (ii = 0; ii < boneCount; ++ii) {
                this.sortReset(constrained.get((int)ii).children);
            }
            for (ii = 0; ii < boneCount; ++ii) {
                constrained.get((int)ii).sorted = true;
            }
        }
        Array<TransformConstraint> transformConstraints = this.transformConstraints;
        int n4 = transformConstraints.size;
        for (i = 0; i < n4; ++i) {
            int ii;
            TransformConstraint constraint = transformConstraints.get(i);
            this.sortBone(constraint.target);
            Array<Bone> constrained = constraint.bones;
            int boneCount = constrained.size;
            for (ii = 0; ii < boneCount; ++ii) {
                this.sortBone(constrained.get(ii));
            }
            updateCache.add(constraint);
            for (ii = 0; ii < boneCount; ++ii) {
                this.sortReset(constrained.get((int)ii).children);
            }
            for (ii = 0; ii < boneCount; ++ii) {
                constrained.get((int)ii).sorted = true;
            }
        }
        n4 = bones.size;
        for (i = 0; i < n4; ++i) {
            this.sortBone(bones.get(i));
        }
    }

    private void sortPathConstraintAttachment(Skin skin, int slotIndex, Bone slotBone) {
        for (ObjectMap.Entry entry : skin.attachments.entries()) {
            if (((Skin.Key)entry.key).slotIndex != slotIndex) continue;
            this.sortPathConstraintAttachment((Attachment)entry.value, slotBone);
        }
    }

    private void sortPathConstraintAttachment(Attachment attachment, Bone slotBone) {
        if (!(attachment instanceof PathAttachment)) {
            return;
        }
        int[] pathBones = ((PathAttachment)attachment).getBones();
        if (pathBones == null) {
            this.sortBone(slotBone);
        } else {
            Array<Bone> bones = this.bones;
            for (int boneIndex : pathBones) {
                this.sortBone(bones.get(boneIndex));
            }
        }
    }

    private void sortBone(Bone bone) {
        if (bone.sorted) {
            return;
        }
        Bone parent = bone.parent;
        if (parent != null) {
            this.sortBone(parent);
        }
        bone.sorted = true;
        this.updateCache.add(bone);
    }

    private void sortReset(Array<Bone> bones) {
        int n = bones.size;
        for (int i = 0; i < n; ++i) {
            Bone bone = bones.get(i);
            if (bone.sorted) {
                this.sortReset(bone.children);
            }
            bone.sorted = false;
        }
    }

    public void updateWorldTransform() {
        Array<Updatable> updateCache = this.updateCache;
        int n = updateCache.size;
        for (int i = 0; i < n; ++i) {
            updateCache.get(i).update();
        }
    }

    public void setToSetupPose() {
        this.setBonesToSetupPose();
        this.setSlotsToSetupPose();
    }

    public void setBonesToSetupPose() {
        Array<Bone> bones = this.bones;
        int n = bones.size;
        for (int i = 0; i < n; ++i) {
            bones.get(i).setToSetupPose();
        }
        Array<IkConstraint> ikConstraints = this.ikConstraints;
        int n2 = ikConstraints.size;
        for (int i = 0; i < n2; ++i) {
            IkConstraint constraint = ikConstraints.get(i);
            constraint.bendDirection = constraint.data.bendDirection;
            constraint.mix = constraint.data.mix;
        }
        Array<TransformConstraint> transformConstraints = this.transformConstraints;
        int n3 = transformConstraints.size;
        for (int i = 0; i < n3; ++i) {
            TransformConstraint constraint = transformConstraints.get(i);
            TransformConstraintData data = constraint.data;
            constraint.rotateMix = data.rotateMix;
            constraint.translateMix = data.translateMix;
            constraint.scaleMix = data.scaleMix;
            constraint.shearMix = data.shearMix;
        }
        Array<PathConstraint> pathConstraints = this.pathConstraints;
        int n4 = pathConstraints.size;
        for (int i = 0; i < n4; ++i) {
            PathConstraint constraint = pathConstraints.get(i);
            PathConstraintData data = constraint.data;
            constraint.position = data.position;
            constraint.spacing = data.spacing;
            constraint.rotateMix = data.rotateMix;
            constraint.translateMix = data.translateMix;
        }
    }

    public void setSlotsToSetupPose() {
        Array<Slot> slots = this.slots;
        System.arraycopy(slots.items, 0, this.drawOrder.items, 0, slots.size);
        int n = slots.size;
        for (int i = 0; i < n; ++i) {
            slots.get(i).setToSetupPose();
        }
    }

    public SkeletonData getData() {
        return this.data;
    }

    public Array<Bone> getBones() {
        return this.bones;
    }

    public Array<Updatable> getUpdateCache() {
        return this.updateCache;
    }

    public Bone getRootBone() {
        if (this.bones.size == 0) {
            return null;
        }
        return this.bones.first();
    }

    public Bone findBone(String boneName) {
        if (boneName == null) {
            throw new IllegalArgumentException("boneName cannot be null.");
        }
        Array<Bone> bones = this.bones;
        int n = bones.size;
        for (int i = 0; i < n; ++i) {
            Bone bone = bones.get(i);
            if (!bone.data.name.equals(boneName)) continue;
            return bone;
        }
        return null;
    }

    public int findBoneIndex(String boneName) {
        if (boneName == null) {
            throw new IllegalArgumentException("boneName cannot be null.");
        }
        Array<Bone> bones = this.bones;
        int n = bones.size;
        for (int i = 0; i < n; ++i) {
            if (!bones.get((int)i).data.name.equals(boneName)) continue;
            return i;
        }
        return -1;
    }

    public Array<Slot> getSlots() {
        return this.slots;
    }

    public Slot findSlot(String slotName) {
        if (slotName == null) {
            throw new IllegalArgumentException("slotName cannot be null.");
        }
        Array<Slot> slots = this.slots;
        int n = slots.size;
        for (int i = 0; i < n; ++i) {
            Slot slot = slots.get(i);
            if (!slot.data.name.equals(slotName)) continue;
            return slot;
        }
        return null;
    }

    public int findSlotIndex(String slotName) {
        if (slotName == null) {
            throw new IllegalArgumentException("slotName cannot be null.");
        }
        Array<Slot> slots = this.slots;
        int n = slots.size;
        for (int i = 0; i < n; ++i) {
            if (!slots.get((int)i).data.name.equals(slotName)) continue;
            return i;
        }
        return -1;
    }

    public Array<Slot> getDrawOrder() {
        return this.drawOrder;
    }

    public void setDrawOrder(Array<Slot> drawOrder) {
        if (drawOrder == null) {
            throw new IllegalArgumentException("drawOrder cannot be null.");
        }
        this.drawOrder = drawOrder;
    }

    public Skin getSkin() {
        return this.skin;
    }

    public void setSkin(String skinName) {
        Skin skin = this.data.findSkin(skinName);
        if (skin == null) {
            throw new IllegalArgumentException("Skin not found: " + skinName);
        }
        this.setSkin(skin);
    }

    public void setSkin(Skin newSkin) {
        if (newSkin != null) {
            if (this.skin != null) {
                newSkin.attachAll(this, this.skin);
            } else {
                Array<Slot> slots = this.slots;
                int n = slots.size;
                for (int i = 0; i < n; ++i) {
                    Attachment attachment;
                    Slot slot = slots.get(i);
                    String name = slot.data.attachmentName;
                    if (name == null || (attachment = newSkin.getAttachment(i, name)) == null) continue;
                    slot.setAttachment(attachment);
                }
            }
        }
        this.skin = newSkin;
    }

    public Attachment getAttachment(String slotName, String attachmentName) {
        return this.getAttachment(this.data.findSlotIndex(slotName), attachmentName);
    }

    public Attachment getAttachment(int slotIndex, String attachmentName) {
        Attachment attachment;
        if (attachmentName == null) {
            throw new IllegalArgumentException("attachmentName cannot be null.");
        }
        if (this.skin != null && (attachment = this.skin.getAttachment(slotIndex, attachmentName)) != null) {
            return attachment;
        }
        if (this.data.defaultSkin != null) {
            return this.data.defaultSkin.getAttachment(slotIndex, attachmentName);
        }
        return null;
    }

    public void setAttachment(String slotName, String attachmentName) {
        if (slotName == null) {
            throw new IllegalArgumentException("slotName cannot be null.");
        }
        Array<Slot> slots = this.slots;
        int n = slots.size;
        for (int i = 0; i < n; ++i) {
            Slot slot = slots.get(i);
            if (!slot.data.name.equals(slotName)) continue;
            Attachment attachment = null;
            if (attachmentName != null && (attachment = this.getAttachment(i, attachmentName)) == null) {
                throw new IllegalArgumentException("Attachment not found: " + attachmentName + ", for slot: " + slotName);
            }
            slot.setAttachment(attachment);
            return;
        }
        throw new IllegalArgumentException("Slot not found: " + slotName);
    }

    public Array<IkConstraint> getIkConstraints() {
        return this.ikConstraints;
    }

    public IkConstraint findIkConstraint(String constraintName) {
        if (constraintName == null) {
            throw new IllegalArgumentException("constraintName cannot be null.");
        }
        Array<IkConstraint> ikConstraints = this.ikConstraints;
        int n = ikConstraints.size;
        for (int i = 0; i < n; ++i) {
            IkConstraint ikConstraint = ikConstraints.get(i);
            if (!ikConstraint.data.name.equals(constraintName)) continue;
            return ikConstraint;
        }
        return null;
    }

    public Array<TransformConstraint> getTransformConstraints() {
        return this.transformConstraints;
    }

    public TransformConstraint findTransformConstraint(String constraintName) {
        if (constraintName == null) {
            throw new IllegalArgumentException("constraintName cannot be null.");
        }
        Array<TransformConstraint> transformConstraints = this.transformConstraints;
        int n = transformConstraints.size;
        for (int i = 0; i < n; ++i) {
            TransformConstraint constraint = transformConstraints.get(i);
            if (!constraint.data.name.equals(constraintName)) continue;
            return constraint;
        }
        return null;
    }

    public Array<PathConstraint> getPathConstraints() {
        return this.pathConstraints;
    }

    public PathConstraint findPathConstraint(String constraintName) {
        if (constraintName == null) {
            throw new IllegalArgumentException("constraintName cannot be null.");
        }
        Array<PathConstraint> pathConstraints = this.pathConstraints;
        int n = pathConstraints.size;
        for (int i = 0; i < n; ++i) {
            PathConstraint constraint = pathConstraints.get(i);
            if (!constraint.data.name.equals(constraintName)) continue;
            return constraint;
        }
        return null;
    }

    public void getBounds(Vector2 offset, Vector2 size) {
        if (offset == null) {
            throw new IllegalArgumentException("offset cannot be null.");
        }
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null.");
        }
        Array<Slot> drawOrder = this.drawOrder;
        float minX = 2.1474836E9f;
        float minY = 2.1474836E9f;
        float maxX = -2.1474836E9f;
        float maxY = -2.1474836E9f;
        int n = drawOrder.size;
        for (int i = 0; i < n; ++i) {
            Slot slot = drawOrder.get(i);
            float[] vertices = null;
            Attachment attachment = slot.attachment;
            if (attachment instanceof RegionAttachment) {
                vertices = ((RegionAttachment)attachment).updateWorldVertices(slot, false);
            } else if (attachment instanceof MeshAttachment) {
                vertices = ((MeshAttachment)attachment).updateWorldVertices(slot, true);
            }
            if (vertices == null) continue;
            int nn = vertices.length;
            for (int ii = 0; ii < nn; ii += 5) {
                float x = vertices[ii];
                float y = vertices[ii + 1];
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        offset.set(minX, minY);
        size.set(maxX - minX, maxY - minY);
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null.");
        }
        this.color.set(color);
    }

    public boolean getFlipX() {
        return this.flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean getFlipY() {
        return this.flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }

    public void setFlip(boolean flipX, boolean flipY) {
        this.flipX = flipX;
        this.flipY = flipY;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getTime() {
        return this.time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public void update(float delta) {
        this.time += delta;
    }

    public String toString() {
        return this.data.name != null ? this.data.name : super.toString();
    }
}

