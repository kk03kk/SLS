/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

class SteamSharedLibraryLoader {
    private static final PLATFORM OS;
    private static final boolean IS_64_BIT;
    private static final String SHARED_LIBRARY_EXTRACT_DIRECTORY;
    private static final String SHARED_LIBRARY_EXTRACT_PATH;
    private static final String SDK_REDISTRIBUTABLE_BIN_PATH;
    private static final String SDK_LIBRARY_PATH;
    static final boolean DEBUG;

    SteamSharedLibraryLoader() {
    }

    private static String getPlatformLibName(String libName) {
        switch (OS) {
            case Windows: {
                return libName + (IS_64_BIT ? "64" : "") + ".dll";
            }
            case Linux: {
                return "lib" + libName + ".so";
            }
            case MacOS: {
                return "lib" + libName + ".dylib";
            }
        }
        throw new RuntimeException("Unknown host architecture");
    }

    static String getSdkRedistributableBinPath() {
        File path;
        switch (OS) {
            case Windows: {
                path = new File(SDK_REDISTRIBUTABLE_BIN_PATH, IS_64_BIT ? "win64" : "");
                break;
            }
            case Linux: {
                path = new File(SDK_REDISTRIBUTABLE_BIN_PATH, "linux64");
                break;
            }
            case MacOS: {
                path = new File(SDK_REDISTRIBUTABLE_BIN_PATH, "osx");
                break;
            }
            default: {
                return null;
            }
        }
        return path.exists() ? path.getPath() : null;
    }

    static String getSdkLibraryPath() {
        File path;
        switch (OS) {
            case Windows: {
                path = new File(SDK_LIBRARY_PATH, IS_64_BIT ? "win64" : "win32");
                break;
            }
            case Linux: {
                path = new File(SDK_LIBRARY_PATH, "linux64");
                break;
            }
            case MacOS: {
                path = new File(SDK_LIBRARY_PATH, "osx");
                break;
            }
            default: {
                return null;
            }
        }
        return path.exists() ? path.getPath() : null;
    }

    static void loadLibrary(String libraryName, String libraryPath) throws SteamException {
        try {
            String librarySystemName = SteamSharedLibraryLoader.getPlatformLibName(libraryName);
            File librarySystemPath = SteamSharedLibraryLoader.discoverExtractLocation(SHARED_LIBRARY_EXTRACT_DIRECTORY + "/" + Version.getVersion(), librarySystemName);
            if (libraryPath == null) {
                SteamSharedLibraryLoader.extractLibrary(librarySystemPath, librarySystemName);
            } else {
                File librarySourcePath = new File(libraryPath, librarySystemName);
                if (OS != PLATFORM.Windows) {
                    SteamSharedLibraryLoader.extractLibrary(librarySystemPath, librarySourcePath);
                } else {
                    librarySystemPath = librarySourcePath;
                }
            }
            String absolutePath = librarySystemPath.getCanonicalPath();
            System.load(absolutePath);
        }
        catch (IOException e) {
            throw new SteamException(e);
        }
    }

    private static void extractLibrary(File librarySystemPath, String librarySystemName) throws IOException {
        SteamSharedLibraryLoader.extractLibrary(librarySystemPath, SteamSharedLibraryLoader.class.getResourceAsStream("/" + librarySystemName));
    }

    private static void extractLibrary(File librarySystemPath, File librarySourcePath) throws IOException {
        SteamSharedLibraryLoader.extractLibrary(librarySystemPath, new FileInputStream(librarySourcePath));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void extractLibrary(File librarySystemPath, InputStream input) throws IOException {
        block20: {
            if (input != null) {
                try (FileOutputStream output = new FileOutputStream(librarySystemPath);){
                    int length;
                    byte[] buffer = new byte[4096];
                    while ((length = input.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                    }
                    output.close();
                    break block20;
                }
                catch (IOException e) {
                    if (!librarySystemPath.exists()) {
                        throw e;
                    }
                    break block20;
                }
                finally {
                    input.close();
                }
            }
            throw new IOException("Failed to read input stream for " + librarySystemPath.getCanonicalPath());
        }
    }

    private static File discoverExtractLocation(String folderName, String fileName) throws IOException {
        File path;
        if (SHARED_LIBRARY_EXTRACT_PATH != null && SteamSharedLibraryLoader.canWrite(path = new File(SHARED_LIBRARY_EXTRACT_PATH, fileName))) {
            return path;
        }
        path = new File(System.getProperty("java.io.tmpdir") + "/" + folderName, fileName);
        if (SteamSharedLibraryLoader.canWrite(path)) {
            return path;
        }
        try {
            File file = File.createTempFile(folderName, null);
            if (file.delete() && SteamSharedLibraryLoader.canWrite(path = new File(file, fileName))) {
                return path;
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        path = new File(System.getProperty("user.home") + "/." + folderName, fileName);
        if (SteamSharedLibraryLoader.canWrite(path)) {
            return path;
        }
        path = new File(".tmp/" + folderName, fileName);
        if (SteamSharedLibraryLoader.canWrite(path)) {
            return path;
        }
        throw new IOException("No suitable extraction path found");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean canWrite(File file) {
        File folder = file.getParentFile();
        if (file.exists()) {
            if (!file.canWrite() || !SteamSharedLibraryLoader.canExecute(file)) {
                return false;
            }
        } else {
            if (!folder.exists() && !folder.mkdirs()) {
                return false;
            }
            if (!folder.isDirectory()) {
                return false;
            }
        }
        File testFile = new File(folder, UUID.randomUUID().toString());
        try {
            new FileOutputStream(testFile).close();
            boolean bl = SteamSharedLibraryLoader.canExecute(testFile);
            return bl;
        }
        catch (IOException e) {
            boolean bl = false;
            return bl;
        }
        finally {
            testFile.delete();
        }
    }

    private static boolean canExecute(File file) {
        try {
            if (file.canExecute()) {
                return true;
            }
            if (file.setExecutable(true)) {
                return file.canExecute();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    static {
        SHARED_LIBRARY_EXTRACT_DIRECTORY = System.getProperty("com.codedisaster.steamworks.SharedLibraryExtractDirectory", "steamworks4j");
        SHARED_LIBRARY_EXTRACT_PATH = System.getProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", null);
        SDK_REDISTRIBUTABLE_BIN_PATH = System.getProperty("com.codedisaster.steamworks.SDKRedistributableBinPath", "sdk/redistributable_bin");
        SDK_LIBRARY_PATH = System.getProperty("com.codedisaster.steamworks.SDKLibraryPath", "sdk/public/steam/lib");
        DEBUG = Boolean.parseBoolean(System.getProperty("com.codedisaster.steamworks.Debug", "false"));
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        if (osName.contains("Windows")) {
            OS = PLATFORM.Windows;
        } else if (osName.contains("Linux")) {
            OS = PLATFORM.Linux;
        } else if (osName.contains("Mac")) {
            OS = PLATFORM.MacOS;
        } else {
            throw new RuntimeException("Unknown host architecture: " + osName + ", " + osArch);
        }
        IS_64_BIT = osArch.equals("amd64") || osArch.equals("x86_64");
    }

    static enum PLATFORM {
        Windows,
        Linux,
        MacOS;

    }
}

