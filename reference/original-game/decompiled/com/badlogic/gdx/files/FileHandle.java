/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.files;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class FileHandle {
    protected File file;
    protected Files.FileType type;

    protected FileHandle() {
    }

    public FileHandle(String fileName) {
        this.file = new File(fileName);
        this.type = Files.FileType.Absolute;
    }

    public FileHandle(File file) {
        this.file = file;
        this.type = Files.FileType.Absolute;
    }

    protected FileHandle(String fileName, Files.FileType type) {
        this.type = type;
        this.file = new File(fileName);
    }

    protected FileHandle(File file, Files.FileType type) {
        this.file = file;
        this.type = type;
    }

    public String path() {
        return this.file.getPath().replace('\\', '/');
    }

    public String name() {
        return this.file.getName();
    }

    public String extension() {
        String name = this.file.getName();
        int dotIndex = name.lastIndexOf(46);
        if (dotIndex == -1) {
            return "";
        }
        return name.substring(dotIndex + 1);
    }

    public String nameWithoutExtension() {
        String name = this.file.getName();
        int dotIndex = name.lastIndexOf(46);
        if (dotIndex == -1) {
            return name;
        }
        return name.substring(0, dotIndex);
    }

    public String pathWithoutExtension() {
        String path = this.file.getPath().replace('\\', '/');
        int dotIndex = path.lastIndexOf(46);
        if (dotIndex == -1) {
            return path;
        }
        return path.substring(0, dotIndex);
    }

    public Files.FileType type() {
        return this.type;
    }

    public File file() {
        if (this.type == Files.FileType.External) {
            return new File(Gdx.files.getExternalStoragePath(), this.file.getPath());
        }
        return this.file;
    }

    public InputStream read() {
        if (this.type == Files.FileType.Classpath || this.type == Files.FileType.Internal && !this.file().exists() || this.type == Files.FileType.Local && !this.file().exists()) {
            InputStream input = FileHandle.class.getResourceAsStream("/" + this.file.getPath().replace('\\', '/'));
            if (input == null) {
                throw new GdxRuntimeException("File not found: " + this.file + " (" + (Object)((Object)this.type) + ")");
            }
            return input;
        }
        try {
            return new FileInputStream(this.file());
        }
        catch (Exception ex) {
            if (this.file().isDirectory()) {
                throw new GdxRuntimeException("Cannot open a stream to a directory: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
            }
            throw new GdxRuntimeException("Error reading file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
    }

    public BufferedInputStream read(int bufferSize) {
        return new BufferedInputStream(this.read(), bufferSize);
    }

    public Reader reader() {
        return new InputStreamReader(this.read());
    }

    public Reader reader(String charset) {
        InputStream stream = this.read();
        try {
            return new InputStreamReader(stream, charset);
        }
        catch (UnsupportedEncodingException ex) {
            StreamUtils.closeQuietly(stream);
            throw new GdxRuntimeException("Error reading file: " + this, ex);
        }
    }

    public BufferedReader reader(int bufferSize) {
        return new BufferedReader(new InputStreamReader(this.read()), bufferSize);
    }

    public BufferedReader reader(int bufferSize, String charset) {
        try {
            return new BufferedReader(new InputStreamReader(this.read(), charset), bufferSize);
        }
        catch (UnsupportedEncodingException ex) {
            throw new GdxRuntimeException("Error reading file: " + this, ex);
        }
    }

    public String readString() {
        return this.readString(null);
    }

    public String readString(String charset) {
        StringBuilder output = new StringBuilder(this.estimateLength());
        InputStreamReader reader = null;
        try {
            int length;
            reader = charset == null ? new InputStreamReader(this.read()) : new InputStreamReader(this.read(), charset);
            char[] buffer = new char[256];
            while ((length = reader.read(buffer)) != -1) {
                output.append(buffer, 0, length);
            }
        }
        catch (IOException ex) {
            try {
                throw new GdxRuntimeException("Error reading layout file: " + this, ex);
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(reader);
                throw throwable;
            }
        }
        StreamUtils.closeQuietly(reader);
        return output.toString();
    }

    public byte[] readBytes() {
        InputStream input = this.read();
        try {
            byte[] byArray = StreamUtils.copyStreamToByteArray(input, this.estimateLength());
            return byArray;
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Error reading file: " + this, ex);
        }
        finally {
            StreamUtils.closeQuietly(input);
        }
    }

    private int estimateLength() {
        int length = (int)this.length();
        return length != 0 ? length : 512;
    }

    public int readBytes(byte[] bytes, int offset, int size) {
        InputStream input = this.read();
        int position = 0;
        try {
            int count;
            while ((count = input.read(bytes, offset + position, size - position)) > 0) {
                position += count;
            }
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Error reading file: " + this, ex);
        }
        finally {
            StreamUtils.closeQuietly(input);
        }
        return position - offset;
    }

    public OutputStream write(boolean append) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot write to a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot write to an internal file: " + this.file);
        }
        this.parent().mkdirs();
        try {
            return new FileOutputStream(this.file(), append);
        }
        catch (Exception ex) {
            if (this.file().isDirectory()) {
                throw new GdxRuntimeException("Cannot open a stream to a directory: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
            }
            throw new GdxRuntimeException("Error writing file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
    }

    public OutputStream write(boolean append, int bufferSize) {
        return new BufferedOutputStream(this.write(append), bufferSize);
    }

    public void write(InputStream input, boolean append) {
        OutputStream output = null;
        try {
            output = this.write(append);
            StreamUtils.copyStream(input, output);
        }
        catch (Exception ex) {
            throw new GdxRuntimeException("Error stream writing to file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
        finally {
            StreamUtils.closeQuietly(input);
            StreamUtils.closeQuietly(output);
        }
    }

    public Writer writer(boolean append) {
        return this.writer(append, null);
    }

    public Writer writer(boolean append, String charset) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot write to a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot write to an internal file: " + this.file);
        }
        this.parent().mkdirs();
        try {
            FileOutputStream output = new FileOutputStream(this.file(), append);
            if (charset == null) {
                return new OutputStreamWriter(output);
            }
            return new OutputStreamWriter((OutputStream)output, charset);
        }
        catch (IOException ex) {
            if (this.file().isDirectory()) {
                throw new GdxRuntimeException("Cannot open a stream to a directory: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
            }
            throw new GdxRuntimeException("Error writing file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
    }

    public void writeString(String string, boolean append) {
        this.writeString(string, append, null);
    }

    public void writeString(String string, boolean append, String charset) {
        Writer writer = null;
        try {
            writer = this.writer(append, charset);
            writer.write(string);
        }
        catch (Exception ex) {
            try {
                throw new GdxRuntimeException("Error writing file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(writer);
                throw throwable;
            }
        }
        StreamUtils.closeQuietly(writer);
    }

    public void writeBytes(byte[] bytes, boolean append) {
        OutputStream output = this.write(append);
        try {
            output.write(bytes);
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Error writing file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
        finally {
            StreamUtils.closeQuietly(output);
        }
    }

    public void writeBytes(byte[] bytes, int offset, int length, boolean append) {
        OutputStream output = this.write(append);
        try {
            output.write(bytes, offset, length);
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Error writing file: " + this.file + " (" + (Object)((Object)this.type) + ")", ex);
        }
        finally {
            StreamUtils.closeQuietly(output);
        }
    }

    public FileHandle[] list() {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot list a classpath directory: " + this.file);
        }
        String[] relativePaths = this.file().list();
        if (relativePaths == null) {
            return new FileHandle[0];
        }
        FileHandle[] handles = new FileHandle[relativePaths.length];
        int n = relativePaths.length;
        for (int i = 0; i < n; ++i) {
            handles[i] = this.child(relativePaths[i]);
        }
        return handles;
    }

    public FileHandle[] list(FileFilter filter) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot list a classpath directory: " + this.file);
        }
        File file = this.file();
        String[] relativePaths = file.list();
        if (relativePaths == null) {
            return new FileHandle[0];
        }
        FileHandle[] handles = new FileHandle[relativePaths.length];
        int count = 0;
        int n = relativePaths.length;
        for (int i = 0; i < n; ++i) {
            String path = relativePaths[i];
            FileHandle child = this.child(path);
            if (!filter.accept(child.file())) continue;
            handles[count] = child;
            ++count;
        }
        if (count < relativePaths.length) {
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    public FileHandle[] list(FilenameFilter filter) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot list a classpath directory: " + this.file);
        }
        File file = this.file();
        String[] relativePaths = file.list();
        if (relativePaths == null) {
            return new FileHandle[0];
        }
        FileHandle[] handles = new FileHandle[relativePaths.length];
        int count = 0;
        int n = relativePaths.length;
        for (int i = 0; i < n; ++i) {
            String path = relativePaths[i];
            if (!filter.accept(file, path)) continue;
            handles[count] = this.child(path);
            ++count;
        }
        if (count < relativePaths.length) {
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    public FileHandle[] list(String suffix) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot list a classpath directory: " + this.file);
        }
        String[] relativePaths = this.file().list();
        if (relativePaths == null) {
            return new FileHandle[0];
        }
        FileHandle[] handles = new FileHandle[relativePaths.length];
        int count = 0;
        int n = relativePaths.length;
        for (int i = 0; i < n; ++i) {
            String path = relativePaths[i];
            if (!path.endsWith(suffix)) continue;
            handles[count] = this.child(path);
            ++count;
        }
        if (count < relativePaths.length) {
            FileHandle[] newHandles = new FileHandle[count];
            System.arraycopy(handles, 0, newHandles, 0, count);
            handles = newHandles;
        }
        return handles;
    }

    public boolean isDirectory() {
        if (this.type == Files.FileType.Classpath) {
            return false;
        }
        return this.file().isDirectory();
    }

    public FileHandle child(String name) {
        if (this.file.getPath().length() == 0) {
            return new FileHandle(new File(name), this.type);
        }
        return new FileHandle(new File(this.file, name), this.type);
    }

    public FileHandle sibling(String name) {
        if (this.file.getPath().length() == 0) {
            throw new GdxRuntimeException("Cannot get the sibling of the root.");
        }
        return new FileHandle(new File(this.file.getParent(), name), this.type);
    }

    public FileHandle parent() {
        File parent = this.file.getParentFile();
        if (parent == null) {
            parent = this.type == Files.FileType.Absolute ? new File("/") : new File("");
        }
        return new FileHandle(parent, this.type);
    }

    public void mkdirs() {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot mkdirs with a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot mkdirs with an internal file: " + this.file);
        }
        this.file().mkdirs();
    }

    public boolean exists() {
        switch (this.type) {
            case Internal: {
                if (this.file().exists()) {
                    return true;
                }
            }
            case Classpath: {
                return FileHandle.class.getResource("/" + this.file.getPath().replace('\\', '/')) != null;
            }
        }
        return this.file().exists();
    }

    public boolean delete() {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot delete a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot delete an internal file: " + this.file);
        }
        return this.file().delete();
    }

    public boolean deleteDirectory() {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot delete a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot delete an internal file: " + this.file);
        }
        return FileHandle.deleteDirectory(this.file());
    }

    public void emptyDirectory() {
        this.emptyDirectory(false);
    }

    public void emptyDirectory(boolean preserveTree) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot delete a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot delete an internal file: " + this.file);
        }
        FileHandle.emptyDirectory(this.file(), preserveTree);
    }

    public void copyTo(FileHandle dest) {
        boolean sourceDir = this.isDirectory();
        if (!sourceDir) {
            if (dest.isDirectory()) {
                dest = dest.child(this.name());
            }
            FileHandle.copyFile(this, dest);
            return;
        }
        if (dest.exists()) {
            if (!dest.isDirectory()) {
                throw new GdxRuntimeException("Destination exists but is not a directory: " + dest);
            }
        } else {
            dest.mkdirs();
            if (!dest.isDirectory()) {
                throw new GdxRuntimeException("Destination directory cannot be created: " + dest);
            }
        }
        if (!sourceDir) {
            dest = dest.child(this.name());
        }
        FileHandle.copyDirectory(this, dest);
    }

    public void moveTo(FileHandle dest) {
        if (this.type == Files.FileType.Classpath) {
            throw new GdxRuntimeException("Cannot move a classpath file: " + this.file);
        }
        if (this.type == Files.FileType.Internal) {
            throw new GdxRuntimeException("Cannot move an internal file: " + this.file);
        }
        this.copyTo(dest);
        this.delete();
        if (this.exists() && this.isDirectory()) {
            this.deleteDirectory();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long length() {
        if (this.type == Files.FileType.Classpath || this.type == Files.FileType.Internal && !this.file.exists()) {
            InputStream input = this.read();
            try {
                long l = input.available();
                return l;
            }
            catch (Exception exception) {
            }
            finally {
                StreamUtils.closeQuietly(input);
            }
            return 0L;
        }
        return this.file().length();
    }

    public long lastModified() {
        return this.file().lastModified();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof FileHandle)) {
            return false;
        }
        FileHandle other = (FileHandle)obj;
        return this.type == other.type && this.path().equals(other.path());
    }

    public int hashCode() {
        int hash = 1;
        hash = hash * 37 + this.type.hashCode();
        hash = hash * 67 + this.path().hashCode();
        return hash;
    }

    public String toString() {
        return this.file.getPath().replace('\\', '/');
    }

    public static FileHandle tempFile(String prefix) {
        try {
            return new FileHandle(File.createTempFile(prefix, null));
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Unable to create temp file.", ex);
        }
    }

    public static FileHandle tempDirectory(String prefix) {
        try {
            File file = File.createTempFile(prefix, null);
            if (!file.delete()) {
                throw new IOException("Unable to delete temp file: " + file);
            }
            if (!file.mkdir()) {
                throw new IOException("Unable to create temp directory: " + file);
            }
            return new FileHandle(file);
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Unable to create temp file.", ex);
        }
    }

    private static void emptyDirectory(File file, boolean preserveTree) {
        File[] files;
        if (file.exists() && (files = file.listFiles()) != null) {
            int n = files.length;
            for (int i = 0; i < n; ++i) {
                if (!files[i].isDirectory()) {
                    files[i].delete();
                    continue;
                }
                if (preserveTree) {
                    FileHandle.emptyDirectory(files[i], true);
                    continue;
                }
                FileHandle.deleteDirectory(files[i]);
            }
        }
    }

    private static boolean deleteDirectory(File file) {
        FileHandle.emptyDirectory(file, false);
        return file.delete();
    }

    private static void copyFile(FileHandle source, FileHandle dest) {
        try {
            dest.write(source.read(), false);
        }
        catch (Exception ex) {
            throw new GdxRuntimeException("Error copying source file: " + source.file + " (" + (Object)((Object)source.type) + ")\nTo destination: " + dest.file + " (" + (Object)((Object)dest.type) + ")", ex);
        }
    }

    private static void copyDirectory(FileHandle sourceDir, FileHandle destDir) {
        destDir.mkdirs();
        for (FileHandle srcFile : sourceDir.list()) {
            FileHandle destFile = destDir.child(srcFile.name());
            if (srcFile.isDirectory()) {
                FileHandle.copyDirectory(srcFile, destFile);
                continue;
            }
            FileHandle.copyFile(srcFile, destFile);
        }
    }
}

