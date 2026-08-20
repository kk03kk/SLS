/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.assets.loaders.resolvers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

public class ResolutionFileResolver
implements FileHandleResolver {
    protected final FileHandleResolver baseResolver;
    protected final Resolution[] descriptors;

    public ResolutionFileResolver(FileHandleResolver baseResolver, Resolution ... descriptors) {
        if (descriptors.length == 0) {
            throw new IllegalArgumentException("At least one Resolution needs to be supplied.");
        }
        this.baseResolver = baseResolver;
        this.descriptors = descriptors;
    }

    @Override
    public FileHandle resolve(String fileName) {
        Resolution bestResolution = ResolutionFileResolver.choose(this.descriptors);
        FileHandle originalHandle = new FileHandle(fileName);
        FileHandle handle = this.baseResolver.resolve(this.resolve(originalHandle, bestResolution.folder));
        if (!handle.exists()) {
            handle = this.baseResolver.resolve(fileName);
        }
        return handle;
    }

    protected String resolve(FileHandle originalHandle, String suffix) {
        String parentString = "";
        FileHandle parent = originalHandle.parent();
        if (parent != null && !parent.name().equals("")) {
            parentString = parent + "/";
        }
        return parentString + suffix + "/" + originalHandle.name();
    }

    public static Resolution choose(Resolution ... descriptors) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        Resolution best = descriptors[0];
        if (w < h) {
            int n = descriptors.length;
            for (int i = 0; i < n; ++i) {
                Resolution other = descriptors[i];
                if (w < other.portraitWidth || other.portraitWidth < best.portraitWidth || h < other.portraitHeight || other.portraitHeight < best.portraitHeight) continue;
                best = descriptors[i];
            }
        } else {
            int n = descriptors.length;
            for (int i = 0; i < n; ++i) {
                Resolution other = descriptors[i];
                if (w < other.portraitHeight || other.portraitHeight < best.portraitHeight || h < other.portraitWidth || other.portraitWidth < best.portraitWidth) continue;
                best = descriptors[i];
            }
        }
        return best;
    }

    public static class Resolution {
        public final int portraitWidth;
        public final int portraitHeight;
        public final String folder;

        public Resolution(int portraitWidth, int portraitHeight, String folder) {
            this.portraitWidth = portraitWidth;
            this.portraitHeight = portraitHeight;
            this.folder = folder;
        }
    }
}

