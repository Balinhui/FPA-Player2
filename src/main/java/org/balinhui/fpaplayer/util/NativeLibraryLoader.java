package org.balinhui.fpaplayer.util;

import org.balinhui.fpaplayer.Launcher;
import org.balinhui.fpaplayer.SystemInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NativeLibraryLoader {

    private static final String TEMP_DIR_PREFIX = "fpaplayer_native";
    private static final File tempDir;
    private static final Set<String> loadedLibraries = ConcurrentHashMap.newKeySet();

    private NativeLibraryLoader() {}

    static {
        try {
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX).toFile();
            tempDir.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时目录", e);
        }
    }

    public static void load(Map<String, List<SystemInfo.Name>> libs) {
        String tempLibs = Config.get("app.tempLib").value().sValue;
        Map<String, String> existingLibPaths = parseExistingPaths(tempLibs);

        String suffix = getSuffix();
        List<String> loadedPaths = new ArrayList<>();

        for (Map.Entry<String, List<SystemInfo.Name>> entry : libs.entrySet()) {
            String libName = entry.getKey();
            List<SystemInfo.Name> supportedSystems = entry.getValue();

            if (!needsLoad(supportedSystems)) {
                continue;
            }

            if (loadedLibraries.contains(libName)) {
                System.out.println("库已加载: " + libName);
                continue;
            }

            String existingPath = existingLibPaths.get(libName);
            if (existingPath != null && loadExistingLibrary(existingPath)) {
                loadedPaths.add(existingPath);
                loadedLibraries.add(libName);
                continue;
            }

            String loadedPath = extractAndLoadLibrary(libName, suffix);
            if (loadedPath != null) {
                loadedPaths.add(loadedPath);
                loadedLibraries.add(libName);
            }
        }

        if (!loadedPaths.isEmpty()) {
            saveLoadedPaths(loadedPaths);
        }

        System.out.println("本地库加载完成，共加载 " + loadedLibraries.size() + " 个库");
    }

    private static Map<String, String> parseExistingPaths(String tempLibs) {
        Map<String, String> pathMap = new HashMap<>();
        if (tempLibs == null || tempLibs.equals("null") || tempLibs.isEmpty()) {
            return pathMap;
        }

        String[] paths = tempLibs.split(File.pathSeparator);
        for (String path : paths) {
            if (path == null || path.isEmpty()) continue;
            File libFile = new File(path);
            String fileName = libFile.getName();
            String libName = extractLibName(fileName);
            if (libName != null) {
                pathMap.put(libName, path);
            }
        }
        return pathMap;
    }

    private static String extractLibName(String fileName) {
        String name = fileName;
        if (name.startsWith("lib")) {
            name = name.substring(3);
        }
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        return name;
    }

    private static String getSuffix() {
        return switch (SystemInfo.systemName) {
            case WINDOWS -> ".dll";
            case MACOS -> ".dylib";
            case LINUX -> ".so";
            default -> throw new UnsupportedOperationException("不支持的操作系统: " + SystemInfo.systemName);
        };
    }

    private static boolean loadExistingLibrary(String path) {
        File libFile = new File(path);
        if (!libFile.exists() || !libFile.isFile()) {
            return false;
        }

        try {
            System.load(libFile.getAbsolutePath());
            System.out.println("已加载已存在的库: " + libFile.getAbsolutePath());
            return true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("加载已存在的库失败: " + path);
            System.err.println("错误: " + e.getMessage());
            return false;
        }
    }

    private static String extractAndLoadLibrary(String libName, String suffix) {
        String resourceName = getResourceName(libName, suffix);
        String fileName = getFileName(libName, suffix);

        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream("/lib/" + resourceName)) {
            if (in == null) {
                System.err.println("找不到资源文件: /lib/" + resourceName);
                Launcher.exitApplication(-4);
                return null;
            }

            byte[] bytes = in.readAllBytes();

            // 在临时目录中创建文件
            File tempFile = new File(tempDir, fileName);
            Files.write(tempFile.toPath(), bytes);
            tempFile.deleteOnExit();

            System.out.println("创建临时文件: " + tempFile.getAbsolutePath());

            try {
                System.load(tempFile.getAbsolutePath());
                System.out.println("已加载: " + tempFile.getAbsolutePath());
                return tempFile.getAbsolutePath();
            } catch (UnsatisfiedLinkError e) {
                System.err.println("加载失败: " + fileName);
                System.err.println("错误: " + e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("jportaudio")) {
                    System.err.println("提示: 需要 portaudio" + suffix);
                }
                Launcher.exitApplication(-4);
                return null;
            }
        } catch (IOException e) {
            System.err.println("提取库文件失败: " + libName);
            e.printStackTrace();
            Launcher.exitApplication(-4);
            return null;
        }
    }

    private static String getResourceName(String libName, String suffix) {
        if (SystemInfo.systemName == SystemInfo.Name.MACOS) {
            return "lib" + libName + suffix;
        }
        return libName + suffix;
    }

    private static String getFileName(String libName, String suffix) {
        return libName + suffix;
    }

    private static void saveLoadedPaths(List<String> loadedPaths) {
        String paths = String.join(File.pathSeparator, loadedPaths);

        // 合并已存在的路径
        String existing = Config.get("app.tempLib").value().sValue;
        if (existing != null && !existing.equals("null") && !existing.isEmpty()) {
            Set<String> allPaths = new LinkedHashSet<>();
            allPaths.addAll(Arrays.asList(existing.split(File.pathSeparator)));
            allPaths.addAll(loadedPaths);
            paths = String.join(File.pathSeparator, allPaths);
        }

        Config.set("app.tempLib", paths);
        System.out.println("已保存库路径: " + paths);
    }

    private static boolean needsLoad(List<SystemInfo.Name> supportedSystems) {
        for (SystemInfo.Name libName : supportedSystems) {
            if (libName == SystemInfo.systemName) {
                return true;
            }
        }
        return false;
    }


    public static void cleanup() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
    }
}