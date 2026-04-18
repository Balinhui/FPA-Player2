package org.balinhui.fpaplayer.nativeapis;

public class MessageFlags {
    public static class Buttons {
        public static final long HELP = 0x00004000L;
        public static final long OK = 0x00000000L;
        public static final long OK_CANCEL = 0x00000001L;
        public static final long RETRY_CANCEL = 0x00000005L;
        public static final long YES_NO = 0x00000004L;
        public static final long YES_NO_CANCEL = 0x00000003L;
    }

    public static class Icons {
        public static final long WARNING = 0x00000030L;
        public static final long INFORMATION = 0x00000040L;
        public static final long STOP = 0x00000010L;
        public static final long ERROR = 0x00000010L;
        public static final long HAND = 0x00000010L;
    }

    public static class ReturnValue {
        public static final int ABORT = 3;
        public static final int CANCEL = 2;
        public static final int CONTINUE = 11;
        public static final int IGNORE = 5;
        public static final int NO = 7;
        public static final int OK = 1;
        public static final int RETRY = 4;
        public static final int TRYAGAIN = 10;
        public static final int YES = 6;
    }
}
