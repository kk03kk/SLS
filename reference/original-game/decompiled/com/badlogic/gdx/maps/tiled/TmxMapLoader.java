/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
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

public class TmxMapLoader
extends BaseTmxMapLoader<Parameters> {
    public TmxMapLoader() {
        super(new InternalFileHandleResolver());
    }

    public TmxMapLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    public TiledMap load(String fileName) {
        return this.load(fileName, new Parameters());
    }

    public TiledMap load(String fileName, Parameters parameters) {
        try {
            this.convertObjectToTileSpace = parameters.convertObjectToTileSpace;
            this.flipY = parameters.flipY;
            FileHandle tmxFile = this.resolve(fileName);
            this.root = this.xml.parse(tmxFile);
            ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();
            Array<FileHandle> textureFiles = this.loadTilesets(this.root, tmxFile);
            textureFiles.addAll(this.loadImages(this.root, tmxFile));
            for (FileHandle textureFile : textureFiles) {
                Texture texture = new Texture(textureFile, parameters.generateMipMaps);
                texture.setFilter(parameters.textureMinFilter, parameters.textureMagFilter);
                textures.put(textureFile.path(), texture);
            }
            ImageResolver.DirectImageResolver imageResolver = new ImageResolver.DirectImageResolver(textures);
            TiledMap map = this.loadTilemap(this.root, tmxFile, imageResolver);
            map.setOwnedResources(textures.values().toArray());
            return map;
        }
        catch (IOException e) {
            throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
        }
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle tmxFile, Parameters parameter) {
        this.map = null;
        if (parameter != null) {
            this.convertObjectToTileSpace = parameter.convertObjectToTileSpace;
            this.flipY = parameter.flipY;
        } else {
            this.convertObjectToTileSpace = false;
            this.flipY = true;
        }
        try {
            this.map = this.loadTilemap(this.root, tmxFile, new ImageResolver.AssetManagerImageResolver(manager));
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
        }
    }

    @Override
    public TiledMap loadSync(AssetManager manager, String fileName, FileHandle file, Parameters parameter) {
        return this.map;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle tmxFile, Parameters parameter) {
        Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
        try {
            this.root = this.xml.parse(tmxFile);
            boolean generateMipMaps = parameter != null ? parameter.generateMipMaps : false;
            TextureLoader.TextureParameter texParams = new TextureLoader.TextureParameter();
            texParams.genMipMaps = generateMipMaps;
            if (parameter != null) {
                texParams.minFilter = parameter.textureMinFilter;
                texParams.magFilter = parameter.textureMagFilter;
            }
            for (FileHandle image : this.loadTilesets(this.root, tmxFile)) {
                dependencies.add(new AssetDescriptor<Texture>(image, Texture.class, texParams));
            }
            for (FileHandle image : this.loadImages(this.root, tmxFile)) {
                dependencies.add(new AssetDescriptor<Texture>(image, Texture.class, texParams));
            }
            return dependencies;
        }
        catch (IOException e) {
            throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
        }
    }

    protected TiledMap loadTilemap(XmlReader.Element root, FileHandle tmxFile, ImageResolver imageResolver) {
        XmlReader.Element properties;
        TiledMap map = new TiledMap();
        String mapOrientation = root.getAttribute("orientation", null);
        int mapWidth = root.getIntAttribute("width", 0);
        int mapHeight = root.getIntAttribute("height", 0);
        int tileWidth = root.getIntAttribute("tilewidth", 0);
        int tileHeight = root.getIntAttribute("tileheight", 0);
        int hexSideLength = root.getIntAttribute("hexsidelength", 0);
        String staggerAxis = root.getAttribute("staggeraxis", null);
        String staggerIndex = root.getAttribute("staggerindex", null);
        String mapBackgroundColor = root.getAttribute("backgroundcolor", null);
        MapProperties mapProperties = map.getProperties();
        if (mapOrientation != null) {
            mapProperties.put("orientation", mapOrientation);
        }
        mapProperties.put("width", mapWidth);
        mapProperties.put("height", mapHeight);
        mapProperties.put("tilewidth", tileWidth);
        mapProperties.put("tileheight", tileHeight);
        mapProperties.put("hexsidelength", hexSideLength);
        if (staggerAxis != null) {
            mapProperties.put("staggeraxis", staggerAxis);
        }
        if (staggerIndex != null) {
            mapProperties.put("staggerindex", staggerIndex);
        }
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
        if ((properties = root.getChildByName("properties")) != null) {
            this.loadProperties(map.getProperties(), properties);
        }
        Array<XmlReader.Element> tilesets = root.getChildrenByName("tileset");
        for (XmlReader.Element element : tilesets) {
            this.loadTileSet(map, element, tmxFile, imageResolver);
            root.removeChild(element);
        }
        int j = root.getChildCount();
        for (int i = 0; i < j; ++i) {
            XmlReader.Element element = root.getChild(i);
            String name = element.getName();
            if (name.equals("layer")) {
                this.loadTileLayer(map, element);
                continue;
            }
            if (name.equals("objectgroup")) {
                this.loadObjectGroup(map, element);
                continue;
            }
            if (!name.equals("imagelayer")) continue;
            this.loadImageLayer(map, element, tmxFile, imageResolver);
        }
        return map;
    }

    protected Array<FileHandle> loadTilesets(XmlReader.Element root, FileHandle tmxFile) throws IOException {
        Array<FileHandle> images = new Array<FileHandle>();
        for (XmlReader.Element tileset : root.getChildrenByName("tileset")) {
            String source = tileset.getAttribute("source", null);
            if (source != null) {
                FileHandle tsxFile = TmxMapLoader.getRelativeFileHandle(tmxFile, source);
                tileset = this.xml.parse(tsxFile);
                XmlReader.Element imageElement = tileset.getChildByName("image");
                if (imageElement != null) {
                    String imageSource = tileset.getChildByName("image").getAttribute("source");
                    FileHandle image = TmxMapLoader.getRelativeFileHandle(tsxFile, imageSource);
                    images.add(image);
                    continue;
                }
                for (XmlReader.Element tile : tileset.getChildrenByName("tile")) {
                    String imageSource = tile.getChildByName("image").getAttribute("source");
                    FileHandle image = TmxMapLoader.getRelativeFileHandle(tsxFile, imageSource);
                    images.add(image);
                }
                continue;
            }
            XmlReader.Element imageElement = tileset.getChildByName("image");
            if (imageElement != null) {
                String imageSource = tileset.getChildByName("image").getAttribute("source");
                FileHandle image = TmxMapLoader.getRelativeFileHandle(tmxFile, imageSource);
                images.add(image);
                continue;
            }
            for (XmlReader.Element tile : tileset.getChildrenByName("tile")) {
                String imageSource = tile.getChildByName("image").getAttribute("source");
                FileHandle image = TmxMapLoader.getRelativeFileHandle(tmxFile, imageSource);
                images.add(image);
            }
        }
        return images;
    }

    protected Array<FileHandle> loadImages(XmlReader.Element root, FileHandle tmxFile) throws IOException {
        Array<FileHandle> images = new Array<FileHandle>();
        for (XmlReader.Element imageLayer : root.getChildrenByName("imagelayer")) {
            FileHandle handle;
            XmlReader.Element image = imageLayer.getChildByName("image");
            String source = image.getAttribute("source", null);
            if (source == null || images.contains(handle = TmxMapLoader.getRelativeFileHandle(tmxFile, source), false)) continue;
            images.add(handle);
        }
        return images;
    }

    /*
     * Unable to fully structure code
     */
    protected void loadTileSet(TiledMap map, XmlReader.Element element, FileHandle tmxFile, ImageResolver imageResolver) {
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
                tsx = TmxMapLoader.getRelativeFileHandle(tmxFile, source);
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
                    image = TmxMapLoader.getRelativeFileHandle(tsx, imageSource);
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
                    image = TmxMapLoader.getRelativeFileHandle(tmxFile, imageSource);
                }
            }
lbl45:
            // 5 sources

            tileset = new TiledMapTileSet();
            tileset.setName(name);
            tileset.getProperties().put("firstgid", firstgid);
            if (image != null) {
                texture = imageResolver.getImage(image.path());
                props = tileset.getProperties();
                props.put("imagesource", imageSource);
                props.put("imagewidth", imageWidth);
                props.put("imageheight", imageHeight);
                props.put("tilewidth", tilewidth);
                props.put("tileheight", tileheight);
                props.put("margin", margin);
                props.put("spacing", spacing);
                stopWidth = texture.getRegionWidth() - tilewidth;
                stopHeight = texture.getRegionHeight() - tileheight;
                id = firstgid;
                for (y = margin; y <= stopHeight; y += tileheight + spacing) {
                    for (x = margin; x <= stopWidth; x += tilewidth + spacing) {
                        tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
                        tile = new StaticTiledMapTile(tileRegion);
                        tile.setId(id);
                        tile.setOffsetX(offsetX);
                        tile.setOffsetY(this.flipY != false ? (float)(-offsetY) : (float)offsetY);
                        tileset.putTile(id++, tile);
                    }
                }
            } else {
                tileElements = element.getChildrenByName("tile");
                for (XmlReader.Element tileElement : tileElements) {
                    imageElement = tileElement.getChildByName("image");
                    if (imageElement != null) {
                        imageSource = imageElement.getAttribute("source");
                        imageWidth = imageElement.getIntAttribute("width", 0);
                        imageHeight = imageElement.getIntAttribute("height", 0);
                        image = TmxMapLoader.getRelativeFileHandle(tmxFile, imageSource);
                    }
                    texture = imageResolver.getImage(image.path());
                    tile = new StaticTiledMapTile(texture);
                    tile.setId(firstgid + tileElement.getIntAttribute("id"));
                    tile.setOffsetX(offsetX);
                    tile.setOffsetY(this.flipY != false ? (float)(-offsetY) : (float)offsetY);
                    tileset.putTile(tile.getId(), tile);
                }
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

    public static class Parameters
    extends BaseTmxMapLoader.Parameters {
    }
}

