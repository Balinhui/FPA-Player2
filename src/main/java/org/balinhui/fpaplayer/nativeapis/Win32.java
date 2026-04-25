package org.balinhui.fpaplayer.nativeapis;

public class Win32 {
    private Win32() {}

    protected static int SYSTEM_BACKGROUND = 38;
    protected static int DARK_MODE = 20;

    protected static native boolean applyWindowsEffect(long hwnd, int type, int attribute);

    public enum Effects {
        MICA(2),
        TRANS(3),
        TABBED(4);

        public final int nativeInt;

        Effects(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }
}
