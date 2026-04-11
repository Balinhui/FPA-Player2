package org.balinhui.fpaplayer;

import java.util.Locale;

public class SystemInfo {
    public static final Name systemName;
    public static final double systemVersion;
    public static final Arch systemArch;

    static {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (name.contains("windows")) systemName = Name.WINDOWS;
        else if (name.contains("mac")) systemName = Name.MACOS;
        else if (name.contains("linux")) systemName = Name.LINUX;
        else systemName = Name.UNKNOWN;

        String ver = System.getProperty("os.version");
        systemVersion = Double.parseDouble(ver.replaceAll("\\.", ""));

        String arch = System.getProperty("os.arch");
        if (arch.contains("amd64")) systemArch = Arch.AMD64;
        else if (arch.contains("x86")) systemArch = Arch.X86;
        else if (arch.contains("arm")) systemArch = Arch.ARM;
        else systemArch = Arch.UNKNOWN;
    }

    private SystemInfo() {}

    public enum Name {
        WINDOWS, MACOS, LINUX, UNKNOWN
    }

    public enum Arch {
        AMD64, X86, ARM, UNKNOWN
    }
}
