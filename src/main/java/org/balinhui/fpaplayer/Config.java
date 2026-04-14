package org.balinhui.fpaplayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final Logger log = LogManager.getLogger(Config.class);
    private static final String configDir = "." + File.separator + "config";
    private static final String configFileName = "config.properties";
    private static Properties ppt;
    private static boolean loaded = false;
    private static final Map<String, ConfigPreference> configList = Map.ofEntries(
            Map.entry("app.height", new ConfigPreference(ConfigType.DOUBLE, new ConfigValue("-1"))),
            Map.entry("app.width", new ConfigPreference(ConfigType.DOUBLE, new ConfigValue("-1"))),
            Map.entry("app.x", new ConfigPreference(ConfigType.DOUBLE, new ConfigValue("-1"))),
            Map.entry("app.y", new ConfigPreference(ConfigType.DOUBLE, new ConfigValue("-1"))),
            Map.entry("app.tempLib", new ConfigPreference(ConfigType.STR, new ConfigValue("null"))),
            Map.entry("app.darkMode", new ConfigPreference(ConfigType.BOOL, new ConfigValue("false"))),
            Map.entry("app.taskbar", new ConfigPreference(ConfigType.BOOL, new ConfigValue("false"))),
            Map.entry("app.fullScreen", new ConfigPreference(ConfigType.BOOL, new ConfigValue("false"))),
            Map.entry("lyric.position", new ConfigPreference(ConfigType.STR, new ConfigValue("center"))),
            Map.entry("lyric.translate", new ConfigPreference(ConfigType.BOOL, new ConfigValue("true")))
    );

    private Config() {}

    public static void loadConfig() {
        if (loaded) return;
        log.debug("配置加载中...");
        File file = checkFile();
        try (FileInputStream in = new FileInputStream(file)) {
            ppt = new Properties();
            ppt.load(in);
        } catch (IOException e) {
            log.error("读取配置文件失败");
            Launcher.exitApplication(-2);
        }
        configList.forEach((name, preference) -> {
            switch (preference.type) {
                case BOOL -> preference.value.set(readForBoolean(name, preference.value.defaultValue));
                case DOUBLE -> preference.value.set(readForDouble(name, preference.value.defaultValue));
                case STR -> preference.value.set(readForString(name, preference.value.defaultValue));
            }
        });
        loaded = true;
        log.debug("配置加载完成");
    }

    public static void storeConfig() {
        if (!loaded) return;
        configList.forEach((name, preference) -> {
            switch (preference.type) {
                case BOOL -> ppt.setProperty(name, preference.value.bValue + "");
                case DOUBLE -> ppt.setProperty(name, preference.value.dValue + "");
                case STR -> ppt.setProperty(name, preference.value.sValue);
            }
        });
        try {
            ppt.store(new FileWriter(configDir + File.separator + configFileName), "FPA Player Settings");
        } catch (IOException e) {
            log.error("储存配置失败");
            System.exit(-2);
        }
        log.debug("储存配置成功");
    }

    public static ConfigPreference get(String key) {
        if (!loaded) {
            log.error("配置未加载");
            return new ConfigPreference(null, null);
        }
        return configList.get(key);
    }

    public static void set(String key, boolean value) {
        if (!loaded) return;
        if (!configList.containsKey(key)) return;
        ConfigPreference preference = configList.get(key);
        if (checkType(preference, ConfigType.BOOL)) return;
        preference.value.set(value);
    }

    public static void set(String key, double value) {
        if (!loaded) return;
        if (!configList.containsKey(key)) return;
        ConfigPreference preference = configList.get(key);
        if (checkType(preference, ConfigType.DOUBLE)) return;
        preference.value.set(value);
    }

    public static void set(String key, String value) {
        if (!loaded) return;
        if (!configList.containsKey(key)) return;
        ConfigPreference preference = configList.get(key);
        if (checkType(preference, ConfigType.STR)) return;
        preference.value.set(value);
    }

    private static File checkFile() {
        File dir = new File(configDir);
        if (!dir.exists()) {
            if (dir.getParentFile() == null || !dir.getParentFile().canWrite()) {
                log.error("父目录不存在或没有创建文件夹的权限");
                Launcher.exitApplication(-3);
            }
            if (!dir.mkdir()) {
                log.error("无法创建config文件夹");
                Launcher.exitApplication(-2);
            }
        } else {
            if (!dir.isDirectory()) {
                log.error("config为一个文件，无法创建文件夹");
                Launcher.exitApplication(-2);
            }
        }
        File file = new File(configDir + File.separator + configFileName);
        if (!file.exists()) {
            try {
                if (!file.createNewFile())
                    throw new IOException("创建文件失败");
            } catch (IOException e) {
                log.error("无法创建config文件: {}", e.getMessage());
                Launcher.exitApplication(-2);
            }
        }
        return file;
    }

    private static boolean readForBoolean(String name, String defaultValue) {
        String v = ppt.getProperty(name, defaultValue);
        return Boolean.parseBoolean(v);
    }

    private static double readForDouble(String name, String defaultValue) {
        String v = ppt.getProperty(name, defaultValue);
        return Double.parseDouble(v);
    }

    private static String readForString(String name, String defaultValue) {
        if (defaultValue != null)
            return ppt.getProperty(name, defaultValue);
        return ppt.getProperty(name);
    }

    private static boolean checkType(ConfigPreference preference, ConfigType type) {
        if (preference.type != type) {
            log.warn("需要储存的数据类型和实际数据类型不符");
            return true;
        }
        return false;
    }

    public record ConfigPreference(ConfigType type, ConfigValue value) {}

    public static class ConfigValue {
        public boolean bValue;
        public double dValue;
        public String sValue;
        public final String defaultValue;

        public ConfigValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public void set(boolean b) {
            bValue = b;
        }

        public void set(double d) {
            dValue = d;
        }

        public void set(String s) {
            sValue = s;
        }

        @Override
        public String toString() {
            return bValue + " " + dValue + " " + sValue;
        }
    }

    public enum ConfigType {
        BOOL, DOUBLE, STR
    }
}
