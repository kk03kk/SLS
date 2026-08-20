/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;

public class TextureAtlas
implements Disposable {
    static final String[] tuple = new String[4];
    private final ObjectSet<Texture> textures = new ObjectSet(4);
    private final Array<AtlasRegion> regions = new Array();
    static final Comparator<TextureAtlasData.Region> indexComparator = new Comparator<TextureAtlasData.Region>(){

        @Override
        public int compare(TextureAtlasData.Region region1, TextureAtlasData.Region region2) {
            int i2;
            int i1 = region1.index;
            if (i1 == -1) {
                i1 = Integer.MAX_VALUE;
            }
            if ((i2 = region2.index) == -1) {
                i2 = Integer.MAX_VALUE;
            }
            return i1 - i2;
        }
    };

    public TextureAtlas() {
    }

    public TextureAtlas(String internalPackFile) {
        this(Gdx.files.internal(internalPackFile));
    }

    public TextureAtlas(FileHandle packFile) {
        this(packFile, packFile.parent());
    }

    public TextureAtlas(FileHandle packFile, boolean flip) {
        this(packFile, packFile.parent(), flip);
    }

    public TextureAtlas(FileHandle packFile, FileHandle imagesDir) {
        this(packFile, imagesDir, false);
    }

    public TextureAtlas(FileHandle packFile, FileHandle imagesDir, boolean flip) {
        this(new TextureAtlasData(packFile, imagesDir, flip));
    }

    public TextureAtlas(TextureAtlasData data) {
        if (data != null) {
            this.load(data);
        }
    }

    private void load(TextureAtlasData data) {
        ObjectMap<TextureAtlasData.Page, Texture> pageToTexture = new ObjectMap<TextureAtlasData.Page, Texture>();
        for (TextureAtlasData.Page page : data.pages) {
            Texture texture = null;
            if (page.texture == null) {
                texture = new Texture(page.textureFile, page.format, page.useMipMaps);
                texture.setFilter(page.minFilter, page.magFilter);
                texture.setWrap(page.uWrap, page.vWrap);
            } else {
                texture = page.texture;
                texture.setFilter(page.minFilter, page.magFilter);
                texture.setWrap(page.uWrap, page.vWrap);
            }
            this.textures.add(texture);
            pageToTexture.put(page, texture);
        }
        for (TextureAtlasData.Region region : data.regions) {
            int width = region.width;
            int height = region.height;
            AtlasRegion atlasRegion = new AtlasRegion((Texture)pageToTexture.get(region.page), region.left, region.top, region.rotate ? height : width, region.rotate ? width : height);
            atlasRegion.index = region.index;
            atlasRegion.name = region.name;
            atlasRegion.offsetX = region.offsetX;
            atlasRegion.offsetY = region.offsetY;
            atlasRegion.originalHeight = region.originalHeight;
            atlasRegion.originalWidth = region.originalWidth;
            atlasRegion.rotate = region.rotate;
            atlasRegion.splits = region.splits;
            atlasRegion.pads = region.pads;
            if (region.flip) {
                atlasRegion.flip(false, true);
            }
            this.regions.add(atlasRegion);
        }
    }

    public AtlasRegion addRegion(String name, Texture texture, int x, int y, int width, int height) {
        this.textures.add(texture);
        AtlasRegion region = new AtlasRegion(texture, x, y, width, height);
        region.name = name;
        region.originalWidth = width;
        region.originalHeight = height;
        region.index = -1;
        this.regions.add(region);
        return region;
    }

    public AtlasRegion addRegion(String name, TextureRegion textureRegion) {
        return this.addRegion(name, textureRegion.texture, textureRegion.getRegionX(), textureRegion.getRegionY(), textureRegion.getRegionWidth(), textureRegion.getRegionHeight());
    }

    public Array<AtlasRegion> getRegions() {
        return this.regions;
    }

    public AtlasRegion findRegion(String name) {
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            if (!this.regions.get((int)i).name.equals(name)) continue;
            return this.regions.get(i);
        }
        return null;
    }

    public AtlasRegion findRegion(String name, int index) {
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            AtlasRegion region = this.regions.get(i);
            if (!region.name.equals(name) || region.index != index) continue;
            return region;
        }
        return null;
    }

    public Array<AtlasRegion> findRegions(String name) {
        Array<AtlasRegion> matched = new Array<AtlasRegion>();
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            AtlasRegion region = this.regions.get(i);
            if (!region.name.equals(name)) continue;
            matched.add(new AtlasRegion(region));
        }
        return matched;
    }

    public Array<Sprite> createSprites() {
        Array<Sprite> sprites = new Array<Sprite>(this.regions.size);
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            sprites.add(this.newSprite(this.regions.get(i)));
        }
        return sprites;
    }

    public Sprite createSprite(String name) {
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            if (!this.regions.get((int)i).name.equals(name)) continue;
            return this.newSprite(this.regions.get(i));
        }
        return null;
    }

    public Sprite createSprite(String name, int index) {
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            AtlasRegion region = this.regions.get(i);
            if (!region.name.equals(name) || region.index != index) continue;
            return this.newSprite(this.regions.get(i));
        }
        return null;
    }

    public Array<Sprite> createSprites(String name) {
        Array<Sprite> matched = new Array<Sprite>();
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            AtlasRegion region = this.regions.get(i);
            if (!region.name.equals(name)) continue;
            matched.add(this.newSprite(region));
        }
        return matched;
    }

    private Sprite newSprite(AtlasRegion region) {
        if (region.packedWidth == region.originalWidth && region.packedHeight == region.originalHeight) {
            if (region.rotate) {
                Sprite sprite = new Sprite(region);
                sprite.setBounds(0.0f, 0.0f, region.getRegionHeight(), region.getRegionWidth());
                sprite.rotate90(true);
                return sprite;
            }
            return new Sprite(region);
        }
        return new AtlasSprite(region);
    }

    public NinePatch createPatch(String name) {
        int n = this.regions.size;
        for (int i = 0; i < n; ++i) {
            AtlasRegion region = this.regions.get(i);
            if (!region.name.equals(name)) continue;
            int[] splits = region.splits;
            if (splits == null) {
                throw new IllegalArgumentException("Region does not have ninepatch splits: " + name);
            }
            NinePatch patch = new NinePatch((TextureRegion)region, splits[0], splits[1], splits[2], splits[3]);
            if (region.pads != null) {
                patch.setPadding(region.pads[0], region.pads[1], region.pads[2], region.pads[3]);
            }
            return patch;
        }
        return null;
    }

    public ObjectSet<Texture> getTextures() {
        return this.textures;
    }

    @Override
    public void dispose() {
        for (Texture texture : this.textures) {
            texture.dispose();
        }
        this.textures.clear();
    }

    static String readValue(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        int colon = line.indexOf(58);
        if (colon == -1) {
            throw new GdxRuntimeException("Invalid line: " + line);
        }
        return line.substring(colon + 1).trim();
    }

    static int readTuple(BufferedReader reader) throws IOException {
        int comma;
        String line = reader.readLine();
        int colon = line.indexOf(58);
        if (colon == -1) {
            throw new GdxRuntimeException("Invalid line: " + line);
        }
        int i = 0;
        int lastMatch = colon + 1;
        for (i = 0; i < 3 && (comma = line.indexOf(44, lastMatch)) != -1; ++i) {
            TextureAtlas.tuple[i] = line.substring(lastMatch, comma).trim();
            lastMatch = comma + 1;
        }
        TextureAtlas.tuple[i] = line.substring(lastMatch).trim();
        return i + 1;
    }

    public static class AtlasSprite
    extends Sprite {
        final AtlasRegion region;
        float originalOffsetX;
        float originalOffsetY;

        public AtlasSprite(AtlasRegion region) {
            this.region = new AtlasRegion(region);
            this.originalOffsetX = region.offsetX;
            this.originalOffsetY = region.offsetY;
            this.setRegion(region);
            this.setOrigin((float)region.originalWidth / 2.0f, (float)region.originalHeight / 2.0f);
            int width = region.getRegionWidth();
            int height = region.getRegionHeight();
            if (region.rotate) {
                super.rotate90(true);
                super.setBounds(region.offsetX, region.offsetY, height, width);
            } else {
                super.setBounds(region.offsetX, region.offsetY, width, height);
            }
            this.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public AtlasSprite(AtlasSprite sprite) {
            this.region = sprite.region;
            this.originalOffsetX = sprite.originalOffsetX;
            this.originalOffsetY = sprite.originalOffsetY;
            this.set(sprite);
        }

        @Override
        public void setPosition(float x, float y) {
            super.setPosition(x + this.region.offsetX, y + this.region.offsetY);
        }

        @Override
        public void setX(float x) {
            super.setX(x + this.region.offsetX);
        }

        @Override
        public void setY(float y) {
            super.setY(y + this.region.offsetY);
        }

        @Override
        public void setBounds(float x, float y, float width, float height) {
            float widthRatio = width / (float)this.region.originalWidth;
            float heightRatio = height / (float)this.region.originalHeight;
            this.region.offsetX = this.originalOffsetX * widthRatio;
            this.region.offsetY = this.originalOffsetY * heightRatio;
            int packedWidth = this.region.rotate ? this.region.packedHeight : this.region.packedWidth;
            int packedHeight = this.region.rotate ? this.region.packedWidth : this.region.packedHeight;
            super.setBounds(x + this.region.offsetX, y + this.region.offsetY, (float)packedWidth * widthRatio, (float)packedHeight * heightRatio);
        }

        @Override
        public void setSize(float width, float height) {
            this.setBounds(this.getX(), this.getY(), width, height);
        }

        @Override
        public void setOrigin(float originX, float originY) {
            super.setOrigin(originX - this.region.offsetX, originY - this.region.offsetY);
        }

        @Override
        public void setOriginCenter() {
            super.setOrigin(this.width / 2.0f - this.region.offsetX, this.height / 2.0f - this.region.offsetY);
        }

        @Override
        public void flip(boolean x, boolean y) {
            if (this.region.rotate) {
                super.flip(y, x);
            } else {
                super.flip(x, y);
            }
            float oldOriginX = this.getOriginX();
            float oldOriginY = this.getOriginY();
            float oldOffsetX = this.region.offsetX;
            float oldOffsetY = this.region.offsetY;
            float widthRatio = this.getWidthRatio();
            float heightRatio = this.getHeightRatio();
            this.region.offsetX = this.originalOffsetX;
            this.region.offsetY = this.originalOffsetY;
            this.region.flip(x, y);
            this.originalOffsetX = this.region.offsetX;
            this.originalOffsetY = this.region.offsetY;
            this.region.offsetX *= widthRatio;
            this.region.offsetY *= heightRatio;
            this.translate(this.region.offsetX - oldOffsetX, this.region.offsetY - oldOffsetY);
            this.setOrigin(oldOriginX, oldOriginY);
        }

        @Override
        public void rotate90(boolean clockwise) {
            super.rotate90(clockwise);
            float oldOriginX = this.getOriginX();
            float oldOriginY = this.getOriginY();
            float oldOffsetX = this.region.offsetX;
            float oldOffsetY = this.region.offsetY;
            float widthRatio = this.getWidthRatio();
            float heightRatio = this.getHeightRatio();
            if (clockwise) {
                this.region.offsetX = oldOffsetY;
                this.region.offsetY = (float)this.region.originalHeight * heightRatio - oldOffsetX - (float)this.region.packedWidth * widthRatio;
            } else {
                this.region.offsetX = (float)this.region.originalWidth * widthRatio - oldOffsetY - (float)this.region.packedHeight * heightRatio;
                this.region.offsetY = oldOffsetX;
            }
            this.translate(this.region.offsetX - oldOffsetX, this.region.offsetY - oldOffsetY);
            this.setOrigin(oldOriginX, oldOriginY);
        }

        @Override
        public float getX() {
            return super.getX() - this.region.offsetX;
        }

        @Override
        public float getY() {
            return super.getY() - this.region.offsetY;
        }

        @Override
        public float getOriginX() {
            return super.getOriginX() + this.region.offsetX;
        }

        @Override
        public float getOriginY() {
            return super.getOriginY() + this.region.offsetY;
        }

        @Override
        public float getWidth() {
            return super.getWidth() / this.region.getRotatedPackedWidth() * (float)this.region.originalWidth;
        }

        @Override
        public float getHeight() {
            return super.getHeight() / this.region.getRotatedPackedHeight() * (float)this.region.originalHeight;
        }

        public float getWidthRatio() {
            return super.getWidth() / this.region.getRotatedPackedWidth();
        }

        public float getHeightRatio() {
            return super.getHeight() / this.region.getRotatedPackedHeight();
        }

        public AtlasRegion getAtlasRegion() {
            return this.region;
        }

        public String toString() {
            return this.region.toString();
        }
    }

    public static class AtlasRegion
    extends TextureRegion {
        public int index;
        public String name;
        public float offsetX;
        public float offsetY;
        public int packedWidth;
        public int packedHeight;
        public int originalWidth;
        public int originalHeight;
        public boolean rotate;
        public int[] splits;
        public int[] pads;

        public AtlasRegion(Texture texture, int x, int y, int width, int height) {
            super(texture, x, y, width, height);
            this.originalWidth = width;
            this.originalHeight = height;
            this.packedWidth = width;
            this.packedHeight = height;
        }

        public AtlasRegion(AtlasRegion region) {
            this.setRegion(region);
            this.index = region.index;
            this.name = region.name;
            this.offsetX = region.offsetX;
            this.offsetY = region.offsetY;
            this.packedWidth = region.packedWidth;
            this.packedHeight = region.packedHeight;
            this.originalWidth = region.originalWidth;
            this.originalHeight = region.originalHeight;
            this.rotate = region.rotate;
            this.splits = region.splits;
        }

        @Override
        public void flip(boolean x, boolean y) {
            super.flip(x, y);
            if (x) {
                this.offsetX = (float)this.originalWidth - this.offsetX - this.getRotatedPackedWidth();
            }
            if (y) {
                this.offsetY = (float)this.originalHeight - this.offsetY - this.getRotatedPackedHeight();
            }
        }

        public float getRotatedPackedWidth() {
            return this.rotate ? (float)this.packedHeight : (float)this.packedWidth;
        }

        public float getRotatedPackedHeight() {
            return this.rotate ? (float)this.packedWidth : (float)this.packedHeight;
        }

        public String toString() {
            return this.name;
        }
    }

    public static class TextureAtlasData {
        final Array<Page> pages = new Array();
        final Array<Region> regions = new Array();

        public TextureAtlasData(FileHandle packFile, FileHandle imagesDir, boolean flip) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(packFile.read()), 64);
            try {
                String line;
                Page pageImage = null;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0) {
                        pageImage = null;
                        continue;
                    }
                    if (pageImage == null) {
                        FileHandle file = imagesDir.child(line);
                        float width = 0.0f;
                        float height = 0.0f;
                        if (TextureAtlas.readTuple(reader) == 2) {
                            width = Integer.parseInt(tuple[0]);
                            height = Integer.parseInt(tuple[1]);
                            TextureAtlas.readTuple(reader);
                        }
                        Pixmap.Format format = Pixmap.Format.valueOf(tuple[0]);
                        TextureAtlas.readTuple(reader);
                        Texture.TextureFilter min = Texture.TextureFilter.valueOf(tuple[0]);
                        Texture.TextureFilter max = Texture.TextureFilter.valueOf(tuple[1]);
                        String direction = TextureAtlas.readValue(reader);
                        Texture.TextureWrap repeatX = Texture.TextureWrap.ClampToEdge;
                        Texture.TextureWrap repeatY = Texture.TextureWrap.ClampToEdge;
                        if (direction.equals("x")) {
                            repeatX = Texture.TextureWrap.Repeat;
                        } else if (direction.equals("y")) {
                            repeatY = Texture.TextureWrap.Repeat;
                        } else if (direction.equals("xy")) {
                            repeatX = Texture.TextureWrap.Repeat;
                            repeatY = Texture.TextureWrap.Repeat;
                        }
                        pageImage = new Page(file, width, height, min.isMipMap(), format, min, max, repeatX, repeatY);
                        this.pages.add(pageImage);
                        continue;
                    }
                    boolean rotate = Boolean.valueOf(TextureAtlas.readValue(reader));
                    TextureAtlas.readTuple(reader);
                    int left = Integer.parseInt(tuple[0]);
                    int top = Integer.parseInt(tuple[1]);
                    TextureAtlas.readTuple(reader);
                    int width = Integer.parseInt(tuple[0]);
                    int height = Integer.parseInt(tuple[1]);
                    Region region = new Region();
                    region.page = pageImage;
                    region.left = left;
                    region.top = top;
                    region.width = width;
                    region.height = height;
                    region.name = line;
                    region.rotate = rotate;
                    if (TextureAtlas.readTuple(reader) == 4) {
                        region.splits = new int[]{Integer.parseInt(tuple[0]), Integer.parseInt(tuple[1]), Integer.parseInt(tuple[2]), Integer.parseInt(tuple[3])};
                        if (TextureAtlas.readTuple(reader) == 4) {
                            region.pads = new int[]{Integer.parseInt(tuple[0]), Integer.parseInt(tuple[1]), Integer.parseInt(tuple[2]), Integer.parseInt(tuple[3])};
                            TextureAtlas.readTuple(reader);
                        }
                    }
                    region.originalWidth = Integer.parseInt(tuple[0]);
                    region.originalHeight = Integer.parseInt(tuple[1]);
                    TextureAtlas.readTuple(reader);
                    region.offsetX = Integer.parseInt(tuple[0]);
                    region.offsetY = Integer.parseInt(tuple[1]);
                    region.index = Integer.parseInt(TextureAtlas.readValue(reader));
                    if (flip) {
                        region.flip = true;
                    }
                    this.regions.add(region);
                }
            }
            catch (Exception ex) {
                throw new GdxRuntimeException("Error reading pack file: " + packFile, ex);
            }
            finally {
                StreamUtils.closeQuietly(reader);
            }
            this.regions.sort(indexComparator);
        }

        public Array<Page> getPages() {
            return this.pages;
        }

        public Array<Region> getRegions() {
            return this.regions;
        }

        public static class Region {
            public Page page;
            public int index;
            public String name;
            public float offsetX;
            public float offsetY;
            public int originalWidth;
            public int originalHeight;
            public boolean rotate;
            public int left;
            public int top;
            public int width;
            public int height;
            public boolean flip;
            public int[] splits;
            public int[] pads;
        }

        public static class Page {
            public final FileHandle textureFile;
            public Texture texture;
            public final float width;
            public final float height;
            public final boolean useMipMaps;
            public final Pixmap.Format format;
            public final Texture.TextureFilter minFilter;
            public final Texture.TextureFilter magFilter;
            public final Texture.TextureWrap uWrap;
            public final Texture.TextureWrap vWrap;

            public Page(FileHandle handle, float width, float height, boolean useMipMaps, Pixmap.Format format, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, Texture.TextureWrap uWrap, Texture.TextureWrap vWrap) {
                this.width = width;
                this.height = height;
                this.textureFile = handle;
                this.useMipMaps = useMipMaps;
                this.format = format;
                this.minFilter = minFilter;
                this.magFilter = magFilter;
                this.uWrap = uWrap;
                this.vWrap = vWrap;
            }
        }
    }
}

