package org.balinhui.fpaplayer.nativeapis;

import org.balinhui.fpaplayer.info.SystemInfo;

import java.util.List;

public class NativeAPI {
    private static long hwnd;

    private NativeAPI() {}

    public static boolean getHWND(String windowTitle) {
        try {
            hwnd = Global.getHwnd(windowTitle);
            return true;
        } catch (UnsatisfiedLinkError e) {
            hwnd = 0;
            return false;
        }
    }

    public static long getHWNDForOthers(String windowTitle) {
        try {
            return Global.getHwnd(windowTitle);
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static String[] getChoseFiles(List<String> suffixNames) {
        return Global.chooseFiles(hwnd, suffixNames);
    }

    public static int displayMessage(String title, String msg, long type) {
        return Global.messageOf(hwnd, title, msg, type);
    }

    public static boolean applyWindowsEffect(Win32.Effects effects) {
        if (SystemInfo.systemName != SystemInfo.Name.WINDOWS) return false;
        return Win32.applyWindowsEffect(hwnd, Win32.SYSTEM_BACKGROUND, effects.nativeInt);
    }

    public static boolean applyWindowsEffectForOthers(long hwnd, Win32.Effects effects) {
        if (SystemInfo.systemName != SystemInfo.Name.WINDOWS) return false;
        return Win32.applyWindowsEffect(hwnd, Win32.SYSTEM_BACKGROUND, effects.nativeInt);
    }

    public static boolean setDarkMode(boolean flag) {
        if (SystemInfo.systemName != SystemInfo.Name.WINDOWS) return false;
        if (hwnd <= 0) return false;
        return Win32.applyWindowsEffect(hwnd, Win32.DARK_MODE, flag ? 1 : 0);
    }

    public static boolean setDarkModeForOthers(long hwnd, boolean flag) {
        if (SystemInfo.systemName != SystemInfo.Name.WINDOWS) return false;
        if (hwnd <= 0) return false;
        return Win32.applyWindowsEffect(hwnd, Win32.DARK_MODE, flag ? 1 : 0);
    }
}
