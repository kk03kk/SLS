/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;
import java.io.IOException;

public class AtlasTmxMapLoader
extends BaseTmxMapLoader<AtlasTiledMapLoaderParameters> {
    protected Array<Texture> trackedTextures = new Array();

    public AtlasTmxMapLoader() {
        super(new InternalFileHandleResolver());
    }

    public AtlasTmxMapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    public TiledMap load(String fileName) {
        return this.load(fileName, new AtlasTiledMapLoaderParameters());
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle tmxFile, AtlasTiledMapLoaderParameters parameter) {
        Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
        try {
            this.root = this.xml.parse(tmxFile);
            XmlReader.Element properties = this.root.getChildByName("properties");
            if (properties != null) {
                for (XmlReader.Element property : properties.getChildrenByName("property")) {
                    String name = property.getAttribute("name");
                    String value = property.getAttribute("value");
                    if (!name.startsWith("atlas")) continue;
                    FileHandle atlasHandle = AtlasTmxMapLoader.getRelativeFileHandle(tmxFile, value);
                    dependencies.add(new AssetDescriptor<TextureAtlas>(atlasHandle, TextureAtlas.class));
                }
            }
        }
        catch (IOException e) {
            throw new GdxRuntimeException("Unable to parse .tmx file.");
        }
        return dependencies;
    }

    public TiledMap load(String fileName, AtlasTiledMapLoaderParameters parameter) {
        try {
            if (parameter != null) {
                this.convertObjectToTileSpace = parameter.convertObjectToTileSpace;
                this.flipY = parameter.flipY;
            } else {
                this.convertObjectToTileSpace = false;
                this.flipY = true;
            }
            FileHandle tmxFile = this.resolve(fileName);
            this.root = this.xml.parse(tmxFile);
            ObjectMap<String, TextureAtlas> atlases = new ObjectMap<String, TextureAtlas>();
            FileHandle atlasFile = this.loadAtlas(this.root, tmxFile);
            if (atlasFile == null) {
                throw new GdxRuntimeException("Couldn't load atlas");
            }
            TextureAtlas atlas = new TextureAtlas(atlasFile);
            atlases.put(atlasFile.path(), atlas);
            AtlasResolver.DirectAtlasResolver atlasResolver = new AtlasResolver.DirectAtlasResolver(atlases);
            TiledMap map = this.loadMap(this.root, tmxFile, atlasResolver);
            map.setOwnedResources(atlases.values().toArray());
            this.setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter);
            return map;
        }
        catch (IOException e) {
            throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
        }
    }

    protected FileHandle loadAtlas(XmlReader.Element root, FileHandle tmxFile) throws IOException {
        FileHandle atlasFile;
        XmlReader.Element e = root.getChildByName("properties");
        if (e != null) {
            for (XmlReader.Element property : e.getChildrenByName("property")) {
                String name = property.getAttribute("name", null);
                String value = property.getAttribute("value", null);
                if (!name.equals("atlas")) continue;
                if (value == null) {
                    value = property.getText();
                }
                if (value == null || value.length() == 0) continue;
                return AtlasTmxMapLoader.getRelativeFileHandle(tmxFile, value);
            }
        }
        return (atlasFile = tmxFile.sibling(tmxFile.nameWithoutExtension() + ".atlas")).exists() ? atlasFile : null;
    }

    private void setTextureFilters(Texture.TextureFilter min, Texture.TextureFilter mag) {
        for (Texture texture : this.trackedTextures) {
            texture.setFilter(min, mag);
        }
        this.trackedTextures.clear();
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle tmxFile, AtlasTiledMapLoaderParameters parameter) {
        this.map = null;
        if (parameter != null) {
            this.convertObjectToTileSpace = parameter.convertObjectToTileSpace;
            this.flipY = parameter.flipY;
        } else {
            this.convertObjectToTileSpace = false;
            this.flipY = true;
        }
        try {
            this.map = this.loadMap(this.root, tmxFile, new AtlasResolver.AssetManagerAtlasResolver(manager));
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
        }
    }

    @Override
    public TiledMap loadSync(AssetManager manager, String fileName, FileHandle file, AtlasTiledMapLoaderParameters parameter) {
        if (parameter != null) {
            this.setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter);
        }
        return this.map;
    }

    protected TiledMap loadMap(XmlReader.Element root, FileHandle tmxFile, AtlasResolver resolver) {
        TiledMap map = new TiledMap();
        String mapOrientation = root.getAttribute("orientation", null);
        int mapWidth = root.getIntAttribute("width", 0);
        int mapHeight = root.getIntAttribute("height", 0);
        int tileWidth = root.getIntAttribute("tilewidth", 0);
        int tileHeight = root.getIntAttribute("tileheight", 0);
        String mapBackgroundColor = root.getAttribute("backgroundcolor", null);
        MapProperties mapProperties = map.getProperties();
        if (mapOrientation != null) {
            mapProperties.put("orientation", mapOrientation);
        }
        mapProperties.put("width", mapWidth);
        mapProperties.put("height", mapHeight);
        mapProperties.put("tilewidth", tileWidth);
        mapProperties.put("tileheight", tileHeight);
        if (mapBackgroundColor != null) {
            mapProperties.put("backgroundcolor", mapBackgroundColor);
        }
        this.mapTileWidth = tileWidth;
        this.mapTileHeight = tileHeight;
        this.mapWidthInPixels = mapWidth * tileWidth;
        this.mapHeightInPixels = mapHeight * tileHeight;
        if (mapOrientation != null && "staggered".equals(mapOrientation) && mapHeight > 1) {
            this.mapWidthInPixels += tileWidth / 2;
            this.mapHeightInPixels = this.mapHeightInPixels / 2 + tileHeight / 2;
        }
        int j = root.getChildCount();
        for (int i = 0; i < j; ++i) {
            XmlReader.Element element = root.getChild(i);
            String elementName = element.getName();
            if (elementName.equals("properties")) {
                this.loadProperties(map.getProperties(), element);
                continue;
            }
            if (elementName.equals("tileset")) {
                this.loadTileset(map, element, tmxFile, resolver);
                continue;
            }
            if (elementName.equals("layer")) {
                this.loadTileLayer(map, element);
                continue;
            }
            if (!elementName.equals("objectgroup")) continue;
            this.loadObjectGroup(map, element);
        }
        return map;
    }

    /*
     * Unable to fully structure code
     */
    protected void loadTileset(TiledMap map, XmlReader.Element element, FileHandle tmxFile, AtlasResolver resolver) {
        if (element.getName().equals("tileset")) {
            name = element.get("name", null);
            firstgid = element.getIntAttribute("firstgid", 1);
            tilewidth = element.getIntAttribute("tilewidth", 0);
            tileheight = element.getIntAttribute("tileheight", 0);
            spacing = element.getIntAttribute("spacing", 0);
            margin = element.getIntAttribute("margin", 0);
            source = element.getAttribute("source", null);
            offsetX = 0;
            offsetY = 0;
            imageSource = "";
            imageWidth = 0;
            imageHeight = 0;
            image = null;
            if (source != null) {
                tsx = AtlasTmxMapLoader.getRelativeFileHandle(tmxFile, source);
                try {
                    element = this.xml.parse(tsx);
                    name = element.get("name", null);
                    tilewidth = element.getIntAttribute("tilewidth", 0);
                    tileheight = element.getIntAttribute("tileheight", 0);
                    spacing = element.getIntAttribute("spacing", 0);
                    margin = element.getIntAttribute("margin", 0);
                    offset = element.getChildByName("tileoffset");
                    if (offset != null) {
                        offsetX = offset.getIntAttribute("x", 0);
                        offsetY = offset.getIntAttribute("y", 0);
                    }
                    if ((imageElement = element.getChildByName("image")) == null) ** GOTO lbl45
                    imageSource = imageElement.getAttribute("source");
                    imageWidth = imageElement.getIntAttribute("width", 0);
                    imageHeight = imageElement.getIntAttribute("height", 0);
                    image = AtlasTmxMapLoader.getRelativeFileHandle(tsx, imageSource);
                }
                catch (IOException e) {
                    throw new GdxRuntimeException("Error parsing external tileset.");
                }
            } else {
                offset = element.getChildByName("tileoffset");
                if (offset != null) {
                    offsetX = offset.getIntAttribute("x", 0);
                    offsetY = offset.getIntAttribute("y", 0);
                }
                if ((imageElement = element.getChildByName("image")) != null) {
                    imageSource = imageElement.getAttribute("source");
                    imageWidth = imageElement.getIntAttribute("width", 0);
                    imageHeight = imageElement.getIntAttribute("height", 0);
                    image = AtlasTmxMapLoader.getRelativeFileHandle(tmxFile, imageSource);
                }
            }
lbl45:
            // 5 sources

            if ((atlasFilePath = map.getProperties().get("atlas", String.class)) == null && (atlasFile = tmxFile.sibling(tmxFile.nameWithoutExtension() + ".atlas")).exists()) {
                atlasFilePath = atlasFile.name();
            }
            if (atlasFilePath == null) {
                throw new GdxRuntimeException("The map is missing the 'atlas' property");
            }
            atlasHandle = AtlasTmxMapLoader.getRelativeFileHandle(tmxFile, atlasFilePath);
            atlasHandle = this.resolve(atlasHandle.path());
            atlas = resolver.getAtlas(atlasHandle.path());
            regionsName = name;
            for (Texture texture : atlas.getTextures()) {
                this.trackedTextures.add(texture);
            }
            tileset = new TiledMapTileSet();
            props = tileset.getProperties();
            tileset.setName(name);
            props.put("firstgid", firstgid);
            props.put("imagesource", imageSource);
            props.put("imagewidth", imageWidth);
            props.put("imageheight", imageHeight);
            props.put("tilewidth", tilewidth);
            props.put("tileheight", tileheight);
            props.put("margin", margin);
            props.put("spacing", spacing);
            if (imageSource != null && imageSource.length() > 0) {
                lastgid = firstgid + imageWidth / tilewidth * (imageHeight / tileheight) - 1;
                for (TextureAtlas.AtlasRegion region : atlas.findRegions(regionsName)) {
                    if (region == null || (tileid = region.index + 1) < firstgid || tileid > lastgid) continue;
                    tile = new StaticTiledMapTile(region);
                    tile.setId(tileid);
                    tile.setOffsetX(offsetX);
                    tile.setOffsetY(this.flipY != false ? (float)(-offsetY) : (float)offsetY);
                    tileset.putTile(tileid, tile);
                }
            }
            for (XmlReader.Element tileElement : element.getChildrenByName("tile")) {
                tileid = firstgid + tileElement.getIntAttribute("id", 0);
                tile = tileset.getTile(tileid);
                if (tile == null && (imageElement = tileElement.getChildByName("image")) != null) {
                    regionName = imageElement.getAttribute("source");
                    region = atlas.findRegion(regionName = regionName.substring(0, regionName.lastIndexOf(46)));
                    if (region == null) {
                        throw new GdxRuntimeException("Tileset region not found: " + regionName);
                    }
                    tile = new StaticTiledMapTile(region);
                    tile.setId(tileid);
                    tile.setOffsetX(offsetX);
                    tile.setOffsetY(this.flipY != false ? (float)(-offsetY) : (float)offsetY);
                    tileset.putTile(tileid, tile);
                }
                if (tile == null) continue;
                terrain = tileElement.getAttribute("terrain", null);
                if (terrain != null) {
                    tile.getProperties().put("terrain", terrain);
                }
                if ((probability = tileElement.getAttribute("probability", null)) != null) {
                    tile.getProperties().put("probability", probability);
                }
                if ((properties = tileElement.getChildByName("properties")) == null) continue;
                this.loadProperties(tile.getProperties(), properties);
            }
            tileElements = element.getChildrenByName("tile");
            animatedTiles = new Array<AnimatedTiledMapTile>();
            for (XmlReader.Element tileElement : tileElements) {
                localtid = tileElement.getIntAttribute("id", 0);
                tile = tileset.getTile(firstgid + localtid);
                if (tile == null) continue;
                animationElement = tileElement.getChildByName("animation");
                if (animationElement != null) {
                    staticTiles = new Array<StaticTiledMapTile>();
                    intervals = new IntArray();
                    for (XmlReader.Element frameElement : animationElement.getChildrenByName("frame")) {
                        staticTiles.add((StaticTiledMapTile)tileset.getTile(firstgid + frameElement.getIntAttribute("tileid")));
                        intervals.add(frameElement.getIntAttribute("duration"));
                    }
                    animatedTile = new AnimatedTiledMapTile(intervals, staticTiles);
                    animatedTile.setId(tile.getId());
                    animatedTiles.add(animatedTile);
                    tile = animatedTile;
                }
                if ((terrain = tileElement.getAttribute("terrain", null)) != null) {
                    tile.getProperties().put("terrain", terrain);
                }
                if ((probability = tileElement.getAttribute("probability", null)) != null) {
                    tile.getProperties().put("probability", probability);
                }
                if ((properties = tileElement.getChildByName("properties")) == null) continue;
                this.loadProperties(tile.getProperties(), properties);
            }
            for (AnimatedTiledMapTile tile : animatedTiles) {
                tileset.putTile(tile.getId(), tile);
            }
            properties = element.getChildByName("properties");
            if (properties != null) {
                this.loadProperties(tileset.getProperties(), properties);
            }
            map.getTileSets().addTileSet(tileset);
        }
    }

    private static interface AtlasResolver {
        public TextureAtlas getAtlas(String var1);

        public static class AssetManagerAtlasResolver
        implements AtlasResolver {
            private final AssetManager assetManager;

            public AssetManagerAtlasResolver(AssetManager assetManager) {
                this.assetManager = assetManager;
            }

            @Override
            public TextureAtlas getAtlas(String name) {
                return this.assetManager.get(name, TextureAtlas.class);
            }
        }

        public static class DirectAtlasResolver
        implements AtlasResolver {
            private final ObjectMap<String, TextureAtlas> atlases;

            public DirectAtlasResolver(ObjectMap<String, TextureAtlas> atlases) {
                this.atlases = atlases;
            }

            @Override
            public TextureAtlas getAtlas(String name) {
                return this.atlases.get(name);
            }
        }
    }

    public static class AtlasTiledMapLoaderParameters
    extends BaseTmxMapLoader.Parameters {
        public boolean forceTextureFilters = false;
    }
}

