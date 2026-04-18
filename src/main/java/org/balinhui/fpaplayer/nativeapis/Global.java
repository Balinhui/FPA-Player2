package org.balinhui.fpaplayer.nativeapis;

import java.util.List;

public class Global {
    private Global() {}

    public static native long getHwnd(String windowTitle);

    public static native String[] chooseFiles(long hwnd, List<String> suffixNames);

    public static native int messageOf(long hwnd, String title, String msg, long type);
}
