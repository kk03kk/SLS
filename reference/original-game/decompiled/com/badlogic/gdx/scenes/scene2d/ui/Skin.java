/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public class Skin
implements Disposable {
    ObjectMap<Class, ObjectMap<String, Object>> resources = new ObjectMap();
    TextureAtlas atlas;

    public Skin() {
    }

    public Skin(FileHandle skinFile) {
        FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
        if (atlasFile.exists()) {
            this.atlas = new TextureAtlas(atlasFile);
            this.addRegions(this.atlas);
        }
        this.load(skinFile);
    }

    public Skin(FileHandle skinFile, TextureAtlas atlas) {
        this.atlas = atlas;
        this.addRegions(atlas);
        this.load(skinFile);
    }

    public Skin(TextureAtlas atlas) {
        this.atlas = atlas;
        this.addRegions(atlas);
    }

    public void load(FileHandle skinFile) {
        try {
            this.getJsonLoader(skinFile).fromJson(Skin.class, skinFile);
        }
        catch (SerializationException ex) {
            throw new SerializationException("Error reading file: " + skinFile, ex);
        }
    }

    public void addRegions(TextureAtlas atlas) {
        Array<TextureAtlas.AtlasRegion> regions = atlas.getRegions();
        int n = regions.size;
        for (int i = 0; i < n; ++i) {
            TextureAtlas.AtlasRegion region = regions.get(i);
            String name = region.name;
            if (region.index != -1) {
                name = name + "_" + region.index;
            }
            this.add(name, region, TextureRegion.class);
        }
    }

    public void add(String name, Object resource) {
        this.add(name, resource, resource.getClass());
    }

    public void add(String name, Object resource, Class type) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (resource == null) {
            throw new IllegalArgumentException("resource cannot be null.");
        }
        ObjectMap<String, Object> typeResources = this.resources.get(type);
        if (typeResources == null) {
            typeResources = new ObjectMap(type == TextureRegion.class || type == Drawable.class || type == Sprite.class ? 256 : 64);
            this.resources.put(type, typeResources);
        }
        typeResources.put(name, resource);
    }

    public void remove(String name, Class type) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        ObjectMap<String, Object> typeResources = this.resources.get(type);
        typeResources.remove(name);
    }

    public <T> T get(Class<T> type) {
        return this.get("default", type);
    }

    public <T> T get(String name, Class<T> type) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null.");
        }
        if (type == Drawable.class) {
            return (T)this.getDrawable(name);
        }
        if (type == TextureRegion.class) {
            return (T)this.getRegion(name);
        }
        if (type == NinePatch.class) {
            return (T)this.getPatch(name);
        }
        if (type == Sprite.class) {
            return (T)this.getSprite(name);
        }
        ObjectMap<String, Object> typeResources = this.resources.get(type);
        if (typeResources == null) {
            throw new GdxRuntimeException("No " + type.getName() + " registered with name: " + name);
        }
        Object resource = typeResources.get(name);
        if (resource == null) {
            throw new GdxRuntimeException("No " + type.getName() + " registered with name: " + name);
        }
        return (T)resource;
    }

    public <T> T optional(String name, Class<T> type) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null.");
        }
        ObjectMap<String, Object> typeResources = this.resources.get(type);
        if (typeResources == null) {
            return null;
        }
        return (T)typeResources.get(name);
    }

    public boolean has(String name, Class type) {
        ObjectMap<String, Object> typeResources = this.resources.get(type);
        if (typeResources == null) {
            return false;
        }
        return typeResources.containsKey(name);
    }

    public <T> ObjectMap<String, T> getAll(Class<T> type) {
        return this.resources.get(type);
    }

    public Color getColor(String name) {
        return this.get(name, Color.class);
    }

    public BitmapFont getFont(String name) {
        return this.get(name, BitmapFont.class);
    }

    public TextureRegion getRegion(String name) {
        TextureRegion region = this.optional(name, TextureRegion.class);
        if (region != null) {
            return region;
        }
        Texture texture = this.optional(name, Texture.class);
        if (texture == null) {
            throw new GdxRuntimeException("No TextureRegion or Texture registered with name: " + name);
        }
        region = new TextureRegion(texture);
        this.add(name, region, TextureRegion.class);
        return region;
    }

    public Array<TextureRegion> getRegions(String regionName) {
        Array<TextureRegion> regions = null;
        int i = 0;
        TextureRegion region = this.optional(regionName + "_" + i++, TextureRegion.class);
        if (region != null) {
            regions = new Array<TextureRegion>();
            while (region != null) {
                regions.add(region);
                region = this.optional(regionName + "_" + i++, TextureRegion.class);
            }
        }
        return regions;
    }

    public TiledDrawable getTiledDrawable(String name) {
        TiledDrawable tiled = this.optional(name, TiledDrawable.class);
        if (tiled != null) {
            return tiled;
        }
        tiled = new TiledDrawable(this.getRegion(name));
        tiled.setName(name);
        this.add(name, tiled, TiledDrawable.class);
        return tiled;
    }

    public NinePatch getPatch(String name) {
        NinePatch patch = this.optional(name, NinePatch.class);
        if (patch != null) {
            return patch;
        }
        try {
            int[] splits;
            TextureRegion region = this.getRegion(name);
            if (region instanceof TextureAtlas.AtlasRegion && (splits = ((TextureAtlas.AtlasRegion)region).splits) != null) {
                patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
                int[] pads = ((TextureAtlas.AtlasRegion)region).pads;
                if (pads != null) {
                    patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
                }
            }
            if (patch == null) {
                patch = new NinePatch(region);
            }
            this.add(name, patch, NinePatch.class);
            return patch;
        }
        catch (GdxRuntimeException ex) {
            throw new GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
        }
    }

    public Sprite getSprite(String name) {
        Sprite sprite = this.optional(name, Sprite.class);
        if (sprite != null) {
            return sprite;
        }
        try {
            TextureRegion textureRegion = this.getRegion(name);
            if (textureRegion instanceof TextureAtlas.AtlasRegion) {
                TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion)textureRegion;
                if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight) {
                    sprite = new TextureAtlas.AtlasSprite(region);
                }
            }
            if (sprite == null) {
                sprite = new Sprite(textureRegion);
            }
            this.add(name, sprite, Sprite.class);
            return sprite;
        }
        catch (GdxRuntimeException ex) {
            throw new GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
        }
    }

    public Drawable getDrawable(String name) {
        Drawable drawable = this.optional(name, Drawable.class);
        if (drawable != null) {
            return drawable;
        }
        try {
            TextureRegion textureRegion = this.getRegion(name);
            if (textureRegion instanceof TextureAtlas.AtlasRegion) {
                TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion)textureRegion;
                if (region.splits != null) {
                    drawable = new NinePatchDrawable(this.getPatch(name));
                } else if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight) {
                    drawable = new SpriteDrawable(this.getSprite(name));
                }
            }
            if (drawable == null) {
                drawable = new TextureRegionDrawable(textureRegion);
            }
        }
        catch (GdxRuntimeException textureRegion) {
            // empty catch block
        }
        if (drawable == null) {
            NinePatch patch = this.optional(name, NinePatch.class);
            if (patch != null) {
                drawable = new NinePatchDrawable(patch);
            } else {
                Sprite sprite = this.optional(name, Sprite.class);
                if (sprite != null) {
                    drawable = new SpriteDrawable(sprite);
                } else {
                    throw new GdxRuntimeException("No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name);
                }
            }
        }
        if (drawable instanceof BaseDrawable) {
            ((BaseDrawable)drawable).setName(name);
        }
        this.add(name, drawable, Drawable.class);
        return drawable;
    }

    public String find(Object resource) {
        if (resource == null) {
            throw new IllegalArgumentException("style cannot be null.");
        }
        ObjectMap<String, Object> typeResources = this.resources.get(resource.getClass());
        if (typeResources == null) {
            return null;
        }
        return typeResources.findKey(resource, true);
    }

    public Drawable newDrawable(String name) {
        return this.newDrawable(this.getDrawable(name));
    }

    public Drawable newDrawable(String name, float r, float g, float b, float a) {
        return this.newDrawable(this.getDrawable(name), new Color(r, g, b, a));
    }

    public Drawable newDrawable(String name, Color tint) {
        return this.newDrawable(this.getDrawable(name), tint);
    }

    public Drawable newDrawable(Drawable drawable) {
        if (drawable instanceof TiledDrawable) {
            return new TiledDrawable((TiledDrawable)drawable);
        }
        if (drawable instanceof TextureRegionDrawable) {
            return new TextureRegionDrawable((TextureRegionDrawable)drawable);
        }
        if (drawable instanceof NinePatchDrawable) {
            return new NinePatchDrawable((NinePatchDrawable)drawable);
        }
        if (drawable instanceof SpriteDrawable) {
            return new SpriteDrawable((SpriteDrawable)drawable);
        }
        throw new GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
    }

    public Drawable newDrawable(Drawable drawable, float r, float g, float b, float a) {
        return this.newDrawable(drawable, new Color(r, g, b, a));
    }

    public Drawable newDrawable(Drawable drawable, Color tint) {
        Drawable newDrawable;
        if (drawable instanceof TextureRegionDrawable) {
            newDrawable = ((TextureRegionDrawable)drawable).tint(tint);
        } else if (drawable instanceof NinePatchDrawable) {
            newDrawable = ((NinePatchDrawable)drawable).tint(tint);
        } else if (drawable instanceof SpriteDrawable) {
            newDrawable = ((SpriteDrawable)drawable).tint(tint);
        } else {
            throw new GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
        }
        if (newDrawable instanceof BaseDrawable) {
            BaseDrawable named = (BaseDrawable)newDrawable;
            if (drawable instanceof BaseDrawable) {
                named.setName(((BaseDrawable)drawable).getName() + " (" + tint + ")");
            } else {
                named.setName(" (" + tint + ")");
            }
        }
        return newDrawable;
    }

    public void setEnabled(Actor actor, boolean enabled) {
        Object style;
        Method method = Skin.findMethod(actor.getClass(), "getStyle");
        if (method == null) {
            return;
        }
        try {
            style = method.invoke(actor, new Object[0]);
        }
        catch (Exception ignored) {
            return;
        }
        String name = this.find(style);
        if (name == null) {
            return;
        }
        name = name.replace("-disabled", "") + (enabled ? "" : "-disabled");
        style = this.get(name, style.getClass());
        method = Skin.findMethod(actor.getClass(), "setStyle");
        if (method == null) {
            return;
        }
        try {
            method.invoke(actor, style);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public TextureAtlas getAtlas() {
        return this.atlas;
    }

    @Override
    public void dispose() {
        if (this.atlas != null) {
            this.atlas.dispose();
        }
        for (ObjectMap objectMap : this.resources.values()) {
            for (Object resource : objectMap.values()) {
                if (!(resource instanceof Disposable)) continue;
                ((Disposable)resource).dispose();
            }
        }
    }

    protected Json getJsonLoader(final FileHandle skinFile) {
        final Skin skin = this;
        Json json = new Json(){

            @Override
            public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData) {
                if (jsonData.isString() && !ClassReflection.isAssignableFrom(CharSequence.class, type)) {
                    return Skin.this.get(jsonData.asString(), type);
                }
                return super.readValue(type, elementType, jsonData);
            }
        };
        json.setTypeName(null);
        json.setUsePrototypes(false);
        json.setSerializer(Skin.class, new Json.ReadOnlySerializer<Skin>(){

            @Override
            public Skin read(Json json, JsonValue typeToValueMap, Class ignored) {
                JsonValue valueMap = typeToValueMap.child;
                while (valueMap != null) {
                    try {
                        this.readNamedObjects(json, ClassReflection.forName(valueMap.name()), valueMap);
                    }
                    catch (ReflectionException ex) {
                        throw new SerializationException(ex);
                    }
                    valueMap = valueMap.next;
                }
                return skin;
            }

            private void readNamedObjects(Json json, Class type, JsonValue valueMap) {
                Class addType = type == TintedDrawable.class ? Drawable.class : type;
                JsonValue valueEntry = valueMap.child;
                while (valueEntry != null) {
                    Object object = json.readValue(type, valueEntry);
                    if (object != null) {
                        try {
                            Skin.this.add(valueEntry.name, object, addType);
                            if (addType != Drawable.class && ClassReflection.isAssignableFrom(Drawable.class, addType)) {
                                Skin.this.add(valueEntry.name, object, Drawable.class);
                            }
                        }
                        catch (Exception ex) {
                            throw new SerializationException("Error reading " + ClassReflection.getSimpleName(type) + ": " + valueEntry.name, ex);
                        }
                    }
                    valueEntry = valueEntry.next;
                }
            }
        });
        json.setSerializer(BitmapFont.class, new Json.ReadOnlySerializer<BitmapFont>(){

            @Override
            public BitmapFont read(Json json, JsonValue jsonData, Class type) {
                String path = json.readValue("file", String.class, jsonData);
                int scaledSize = json.readValue("scaledSize", Integer.TYPE, Integer.valueOf(-1), jsonData);
                Boolean flip = json.readValue("flip", Boolean.class, Boolean.valueOf(false), jsonData);
                Boolean markupEnabled = json.readValue("markupEnabled", Boolean.class, Boolean.valueOf(false), jsonData);
                FileHandle fontFile = skinFile.parent().child(path);
                if (!fontFile.exists()) {
                    fontFile = Gdx.files.internal(path);
                }
                if (!fontFile.exists()) {
                    throw new SerializationException("Font file not found: " + fontFile);
                }
                String regionName = fontFile.nameWithoutExtension();
                try {
                    FileHandle imageFile;
                    TextureRegion region;
                    Array<TextureRegion> regions = skin.getRegions(regionName);
                    BitmapFont font = regions != null ? new BitmapFont(new BitmapFont.BitmapFontData(fontFile, flip), regions, true) : ((region = skin.optional(regionName, TextureRegion.class)) != null ? new BitmapFont(fontFile, region, (boolean)flip) : ((imageFile = fontFile.parent().child(regionName + ".png")).exists() ? new BitmapFont(fontFile, imageFile, (boolean)flip) : new BitmapFont(fontFile, flip)));
                    font.getData().markupEnabled = markupEnabled;
                    if (scaledSize != -1) {
                        font.getData().setScale((float)scaledSize / font.getCapHeight());
                    }
                    return font;
                }
                catch (RuntimeException ex) {
                    throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
                }
            }
        });
        json.setSerializer(Color.class, new Json.ReadOnlySerializer<Color>(){

            @Override
            public Color read(Json json, JsonValue jsonData, Class type) {
                if (jsonData.isString()) {
                    return Skin.this.get(jsonData.asString(), Color.class);
                }
                String hex = json.readValue("hex", String.class, (String)null, jsonData);
                if (hex != null) {
                    return Color.valueOf(hex);
                }
                float r = json.readValue("r", Float.TYPE, Float.valueOf(0.0f), jsonData).floatValue();
                float g = json.readValue("g", Float.TYPE, Float.valueOf(0.0f), jsonData).floatValue();
                float b = json.readValue("b", Float.TYPE, Float.valueOf(0.0f), jsonData).floatValue();
                float a = json.readValue("a", Float.TYPE, Float.valueOf(1.0f), jsonData).floatValue();
                return new Color(r, g, b, a);
            }
        });
        json.setSerializer(TintedDrawable.class, new Json.ReadOnlySerializer(){

            @Override
            public Object read(Json json, JsonValue jsonData, Class type) {
                Color color;
                String name = json.readValue("name", String.class, jsonData);
                Drawable drawable = Skin.this.newDrawable(name, color = json.readValue("color", Color.class, jsonData));
                if (drawable instanceof BaseDrawable) {
                    BaseDrawable named = (BaseDrawable)drawable;
                    named.setName(jsonData.name + " (" + name + ", " + color + ")");
                }
                return drawable;
            }
        });
        return json;
    }

    private static Method findMethod(Class type, String name) {
        for (Method method : ClassReflection.getMethods(type)) {
            if (!method.getName().equals(name)) continue;
            return method;
        }
        return null;
    }

    public static class TintedDrawable {
        public String name;
        public Color color;
    }
}

