/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;

public class ParticleEffect
implements Disposable {
    private final Array<ParticleEmitter> emitters;
    private BoundingBox bounds;
    private boolean ownsTexture;

    public ParticleEffect() {
        this.emitters = new Array(8);
    }

    public ParticleEffect(ParticleEffect effect) {
        this.emitters = new Array(true, effect.emitters.size);
        int n = effect.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.add(new ParticleEmitter(effect.emitters.get(i)));
        }
    }

    public void start() {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).start();
        }
    }

    public void reset() {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).reset();
        }
    }

    public void update(float delta) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).update(delta);
        }
    }

    public void draw(Batch spriteBatch) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).draw(spriteBatch);
        }
    }

    public void draw(Batch spriteBatch, float delta) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).draw(spriteBatch, delta);
        }
    }

    public void allowCompletion() {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).allowCompletion();
        }
    }

    public boolean isComplete() {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            if (emitter.isComplete()) continue;
            return false;
        }
        return true;
    }

    public void setDuration(int duration) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            emitter.setContinuous(false);
            emitter.duration = duration;
            emitter.durationTimer = 0.0f;
        }
    }

    public void setPosition(float x, float y) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).setPosition(x, y);
        }
    }

    public void setFlip(boolean flipX, boolean flipY) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).setFlip(flipX, flipY);
        }
    }

    public void flipY() {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).flipY();
        }
    }

    public Array<ParticleEmitter> getEmitters() {
        return this.emitters;
    }

    public ParticleEmitter findEmitter(String name) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            if (!emitter.getName().equals(name)) continue;
            return emitter;
        }
        return null;
    }

    public void save(Writer output) throws IOException {
        int index = 0;
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            if (index++ > 0) {
                output.write("\n\n");
            }
            emitter.save(output);
        }
    }

    public void load(FileHandle effectFile, FileHandle imagesDir) {
        this.loadEmitters(effectFile);
        this.loadEmitterImages(imagesDir);
    }

    public void load(FileHandle effectFile, TextureAtlas atlas) {
        this.load(effectFile, atlas, null);
    }

    public void load(FileHandle effectFile, TextureAtlas atlas, String atlasPrefix) {
        this.loadEmitters(effectFile);
        this.loadEmitterImages(atlas, atlasPrefix);
    }

    public void loadEmitters(FileHandle effectFile) {
        InputStream input = effectFile.read();
        this.emitters.clear();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input), 512);
            do {
                ParticleEmitter emitter = new ParticleEmitter(reader);
                this.emitters.add(emitter);
            } while (reader.readLine() != null && reader.readLine() != null);
        }
        catch (IOException ex) {
            try {
                throw new GdxRuntimeException("Error loading effect: " + effectFile, ex);
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(reader);
                throw throwable;
            }
        }
        StreamUtils.closeQuietly(reader);
    }

    public void loadEmitterImages(TextureAtlas atlas) {
        this.loadEmitterImages(atlas, null);
    }

    public void loadEmitterImages(TextureAtlas atlas, String atlasPrefix) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            Sprite sprite;
            ParticleEmitter emitter = this.emitters.get(i);
            String imagePath = emitter.getImagePath();
            if (imagePath == null) continue;
            String imageName = new File(imagePath.replace('\\', '/')).getName();
            int lastDotIndex = imageName.lastIndexOf(46);
            if (lastDotIndex != -1) {
                imageName = imageName.substring(0, lastDotIndex);
            }
            if (atlasPrefix != null) {
                imageName = atlasPrefix + imageName;
            }
            if ((sprite = atlas.createSprite(imageName)) == null) {
                throw new IllegalArgumentException("SpriteSheet missing image: " + imageName);
            }
            emitter.setSprite(sprite);
        }
    }

    public void loadEmitterImages(FileHandle imagesDir) {
        this.ownsTexture = true;
        HashMap<String, Sprite> loadedSprites = new HashMap<String, Sprite>(this.emitters.size);
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            String imagePath = emitter.getImagePath();
            if (imagePath == null) continue;
            String imageName = new File(imagePath.replace('\\', '/')).getName();
            Sprite sprite = (Sprite)loadedSprites.get(imageName);
            if (sprite == null) {
                sprite = new Sprite(this.loadTexture(imagesDir.child(imageName)));
                loadedSprites.put(imageName, sprite);
            }
            emitter.setSprite(sprite);
        }
    }

    protected Texture loadTexture(FileHandle file) {
        return new Texture(file, false);
    }

    @Override
    public void dispose() {
        if (!this.ownsTexture) {
            return;
        }
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            ParticleEmitter emitter = this.emitters.get(i);
            emitter.getSprite().getTexture().dispose();
        }
    }

    public BoundingBox getBoundingBox() {
        if (this.bounds == null) {
            this.bounds = new BoundingBox();
        }
        BoundingBox bounds = this.bounds;
        bounds.inf();
        for (ParticleEmitter emitter : this.emitters) {
            bounds.ext(emitter.getBoundingBox());
        }
        return bounds;
    }

    public void scaleEffect(float scaleFactor) {
        for (ParticleEmitter particleEmitter : this.emitters) {
            particleEmitter.getScale().setHigh(particleEmitter.getScale().getHighMin() * scaleFactor, particleEmitter.getScale().getHighMax() * scaleFactor);
            particleEmitter.getScale().setLow(particleEmitter.getScale().getLowMin() * scaleFactor, particleEmitter.getScale().getLowMax() * scaleFactor);
            particleEmitter.getVelocity().setHigh(particleEmitter.getVelocity().getHighMin() * scaleFactor, particleEmitter.getVelocity().getHighMax() * scaleFactor);
            particleEmitter.getVelocity().setLow(particleEmitter.getVelocity().getLowMin() * scaleFactor, particleEmitter.getVelocity().getLowMax() * scaleFactor);
            particleEmitter.getGravity().setHigh(particleEmitter.getGravity().getHighMin() * scaleFactor, particleEmitter.getGravity().getHighMax() * scaleFactor);
            particleEmitter.getGravity().setLow(particleEmitter.getGravity().getLowMin() * scaleFactor, particleEmitter.getGravity().getLowMax() * scaleFactor);
            particleEmitter.getWind().setHigh(particleEmitter.getWind().getHighMin() * scaleFactor, particleEmitter.getWind().getHighMax() * scaleFactor);
            particleEmitter.getWind().setLow(particleEmitter.getWind().getLowMin() * scaleFactor, particleEmitter.getWind().getLowMax() * scaleFactor);
            particleEmitter.getSpawnWidth().setHigh(particleEmitter.getSpawnWidth().getHighMin() * scaleFactor, particleEmitter.getSpawnWidth().getHighMax() * scaleFactor);
            particleEmitter.getSpawnWidth().setLow(particleEmitter.getSpawnWidth().getLowMin() * scaleFactor, particleEmitter.getSpawnWidth().getLowMax() * scaleFactor);
            particleEmitter.getSpawnHeight().setHigh(particleEmitter.getSpawnHeight().getHighMin() * scaleFactor, particleEmitter.getSpawnHeight().getHighMax() * scaleFactor);
            particleEmitter.getSpawnHeight().setLow(particleEmitter.getSpawnHeight().getLowMin() * scaleFactor, particleEmitter.getSpawnHeight().getLowMax() * scaleFactor);
            particleEmitter.getXOffsetValue().setLow(particleEmitter.getXOffsetValue().getLowMin() * scaleFactor, particleEmitter.getXOffsetValue().getLowMax() * scaleFactor);
            particleEmitter.getYOffsetValue().setLow(particleEmitter.getYOffsetValue().getLowMin() * scaleFactor, particleEmitter.getYOffsetValue().getLowMax() * scaleFactor);
        }
    }

    public void setEmittersCleanUpBlendFunction(boolean cleanUpBlendFunction) {
        int n = this.emitters.size;
        for (int i = 0; i < n; ++i) {
            this.emitters.get(i).setCleansUpBlendFunction(cleanUpBlendFunction);
        }
    }
}

