package org.balinhui.fpaplayer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.Launcher;
import org.balinhui.fpaplayer.info.SystemInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NativeLibraryLoader {
    private static final Logger log = LogManager.getLogger(NativeLibraryLoader.class);
    private static final String TEMP_DIR_PREFIX = "fpaplayer_native-";
    private static File tempDir;
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
                log.debug("库已加载: {}", libName);
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

        log.debug("本地库加载完成，共加载 {} 个库", loadedLibraries.size());
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

        tempDir = libFile.getParentFile();

        try {
            System.load(libFile.getAbsolutePath());
            log.debug("已加载已存在的库: {}", sweepUserPath(libFile.getAbsolutePath()));
            return true;
        } catch (UnsatisfiedLinkError e) {
            log.error("加载已存在的库失败: {}", sweepUserPath(path));
            log.error("错误: {}", e.getMessage());
            return false;
        }
    }

    private static String extractAndLoadLibrary(String libName, String suffix) {
        String resourceName = getResourceName(libName, suffix);
        String fileName = getFileName(libName, suffix);

        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream("/lib/" + resourceName)) {
            if (in == null) {
                log.error("找不到资源文件: /lib/{}", resourceName);
                Launcher.exitApplication(-4);
                return null;
            }

            byte[] bytes = in.readAllBytes();

            // 在临时目录中创建文件
            File tempFile = new File(tempDir, fileName);
            Files.write(tempFile.toPath(), bytes);
            tempFile.deleteOnExit();

            log.debug("创建临时文件: {}", sweepUserPath(tempFile.getAbsolutePath()));

            try {
                System.load(tempFile.getAbsolutePath());
                log.debug("已加载: {}", sweepUserPath(tempFile.getAbsolutePath()));
                return tempFile.getAbsolutePath();
            } catch (UnsatisfiedLinkError e) {
                log.error("加载失败: {}", fileName);
                log.error("错误: {}", e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("jportaudio")) {
                    log.error("提示: 需要 portaudio{}", suffix);
                }
                Launcher.exitApplication(-4);
                return null;
            }
        } catch (IOException e) {
            log.error("提取库文件失败: {}", libName);
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

        Config.set("app.tempLib", paths);
    }

    private static boolean needsLoad(List<SystemInfo.Name> supportedSystems) {
        for (SystemInfo.Name libName : supportedSystems) {
            if (libName == SystemInfo.systemName) {
                return true;
            }
        }
        return false;
    }

    private static String sweepUserPath(String absolutePath) {
        String[] tmp;
        if (SystemInfo.systemName == SystemInfo.Name.WINDOWS) {
            tmp = absolutePath.split("\\\\");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tmp.length; i++) {
                if (i > 3) sb.append("\\");
                if (i > 2) sb.append(tmp[i]);
            }
            return sb.toString();
        }
        return absolutePath;
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