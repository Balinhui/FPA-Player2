package org.balinhui.fpaplayer.nativeapis;

import java.util.List;

public class Global {
    private Global() {}

    protected static native long getHwnd(String windowTitle);

    protected static native String[] chooseFiles(long hwnd, List<String> suffixNames);

    protected static native int messageOf(long hwnd, String title, String msg, long type);
}
