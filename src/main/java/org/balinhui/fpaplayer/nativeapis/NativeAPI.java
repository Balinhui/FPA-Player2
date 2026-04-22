package org.balinhui.fpaplayer.nativeapis;

import java.util.List;

public class NativeAPI {
    private static long hwnd;

    private NativeAPI() {}

    public static boolean getHWND(String windowTitle) {
        try {
            hwnd = Global.getHwnd(windowTitle);
            return true;
        } catch (UnsatisfiedLinkError e) {
            hwnd = -1;
            return false;
        }
    }

    public static String[] getChoseFiles(List<String> suffixNames) {
        return Global.chooseFiles(hwnd, suffixNames);
    }

    public static int displayMessage(String title, String msg, long type) {
        return Global.messageOf(hwnd, title, msg, type);
    }
}
