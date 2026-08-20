/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.particles;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.reflect.ArrayReflection;

public class ParallelArray {
    Array<Channel> arrays = new Array(false, 2, Channel.class);
    public int capacity;
    public int size;

    public ParallelArray(int capacity) {
        this.capacity = capacity;
        this.size = 0;
    }

    public <T extends Channel> T addChannel(ChannelDescriptor channelDescriptor) {
        return this.addChannel(channelDescriptor, null);
    }

    public <T extends Channel> T addChannel(ChannelDescriptor channelDescriptor, ChannelInitializer<T> initializer) {
        T channel = this.getChannel(channelDescriptor);
        if (channel == null) {
            channel = this.allocateChannel(channelDescriptor);
            if (initializer != null) {
                initializer.init(channel);
            }
            this.arrays.add((Channel)channel);
        }
        return channel;
    }

    private <T extends Channel> T allocateChannel(ChannelDescriptor channelDescriptor) {
        if (channelDescriptor.type == Float.TYPE) {
            return (T)new FloatChannel(channelDescriptor.id, channelDescriptor.count, this.capacity);
        }
        if (channelDescriptor.type == Integer.TYPE) {
            return (T)new IntChannel(channelDescriptor.id, channelDescriptor.count, this.capacity);
        }
        return (T)new ObjectChannel(channelDescriptor.id, channelDescriptor.count, this.capacity, channelDescriptor.type);
    }

    public <T> void removeArray(int id) {
        this.arrays.removeIndex(this.findIndex(id));
    }

    private int findIndex(int id) {
        for (int i = 0; i < this.arrays.size; ++i) {
            Channel array = ((Channel[])this.arrays.items)[i];
            if (array.id != id) continue;
            return i;
        }
        return -1;
    }

    public void addElement(Object ... values) {
        if (this.size == this.capacity) {
            throw new GdxRuntimeException("Capacity reached, cannot add other elements");
        }
        int k = 0;
        for (Channel strideArray : this.arrays) {
            strideArray.add(k, values);
            k += strideArray.strideSize;
        }
        ++this.size;
    }

    public void removeElement(int index) {
        int last = this.size - 1;
        for (Channel strideArray : this.arrays) {
            strideArray.swap(index, last);
        }
        this.size = last;
    }

    public <T extends Channel> T getChannel(ChannelDescriptor descriptor) {
        for (Channel array : this.arrays) {
            if (array.id != descriptor.id) continue;
            return (T)array;
        }
        return null;
    }

    public void clear() {
        this.arrays.clear();
        this.size = 0;
    }

    public void setCapacity(int requiredCapacity) {
        if (this.capacity != requiredCapacity) {
            for (Channel channel : this.arrays) {
                channel.setCapacity(requiredCapacity);
            }
            this.capacity = requiredCapacity;
        }
    }

    public class ObjectChannel<T>
    extends Channel {
        Class<T> componentType;
        public T[] data;

        public ObjectChannel(int id, int strideSize, int size, Class<T> type) {
            super(id, ArrayReflection.newInstance(type, size * strideSize), strideSize);
            this.componentType = type;
            this.data = (Object[])((Channel)this).data;
        }

        @Override
        public void add(int index, Object ... objects) {
            int i = this.strideSize * ParallelArray.this.size;
            int c = i + this.strideSize;
            int k = 0;
            while (i < c) {
                this.data[i] = objects[k];
                ++i;
                ++k;
            }
        }

        @Override
        public void swap(int i, int k) {
            i = this.strideSize * i;
            k = this.strideSize * k;
            int c = i + this.strideSize;
            while (i < c) {
                T t = this.data[i];
                this.data[i] = this.data[k];
                this.data[k] = t;
                ++i;
                ++k;
            }
        }

        @Override
        public void setCapacity(int requiredCapacity) {
            Object[] newData = (Object[])ArrayReflection.newInstance(this.componentType, this.strideSize * requiredCapacity);
            System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newData.length));
            this.data = newData;
            ((Channel)this).data = newData;
        }
    }

    public class IntChannel
    extends Channel {
        public int[] data;

        public IntChannel(int id, int strideSize, int size) {
            super(id, new int[size * strideSize], strideSize);
            this.data = (int[])((Channel)this).data;
        }

        @Override
        public void add(int index, Object ... objects) {
            int i = this.strideSize * ParallelArray.this.size;
            int c = i + this.strideSize;
            int k = 0;
            while (i < c) {
                this.data[i] = (Integer)objects[k];
                ++i;
                ++k;
            }
        }

        @Override
        public void swap(int i, int k) {
            i = this.strideSize * i;
            k = this.strideSize * k;
            int c = i + this.strideSize;
            while (i < c) {
                int t = this.data[i];
                this.data[i] = this.data[k];
                this.data[k] = t;
                ++i;
                ++k;
            }
        }

        @Override
        public void setCapacity(int requiredCapacity) {
            int[] newData = new int[this.strideSize * requiredCapacity];
            System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newData.length));
            this.data = newData;
            ((Channel)this).data = newData;
        }
    }

    public class FloatChannel
    extends Channel {
        public float[] data;

        public FloatChannel(int id, int strideSize, int size) {
            super(id, new float[size * strideSize], strideSize);
            this.data = (float[])((Channel)this).data;
        }

        @Override
        public void add(int index, Object ... objects) {
            int i = this.strideSize * ParallelArray.this.size;
            int c = i + this.strideSize;
            int k = 0;
            while (i < c) {
                this.data[i] = ((Float)objects[k]).floatValue();
                ++i;
                ++k;
            }
        }

        @Override
        public void swap(int i, int k) {
            i = this.strideSize * i;
            k = this.strideSize * k;
            int c = i + this.strideSize;
            while (i < c) {
                float t = this.data[i];
                this.data[i] = this.data[k];
                this.data[k] = t;
                ++i;
                ++k;
            }
        }

        @Override
        public void setCapacity(int requiredCapacity) {
            float[] newData = new float[this.strideSize * requiredCapacity];
            System.arraycopy(this.data, 0, newData, 0, Math.min(this.data.length, newData.length));
            this.data = newData;
            ((Channel)this).data = newData;
        }
    }

    public static interface ChannelInitializer<T extends Channel> {
        public void init(T var1);
    }

    public abstract class Channel {
        public int id;
        public Object data;
        public int strideSize;

        public Channel(int id, Object data, int strideSize) {
            this.id = id;
            this.strideSize = strideSize;
            this.data = data;
        }

        public abstract void add(int var1, Object ... var2);

        public abstract void swap(int var1, int var2);

        protected abstract void setCapacity(int var1);
    }

    public static class ChannelDescriptor {
        public int id;
        public Class<?> type;
        public int count;

        public ChannelDescriptor(int id, Class<?> type, int count) {
            this.id = id;
            this.type = type;
            this.count = count;
        }
    }
}

