package org.balinhui.fpaplayer.util;

import org.balinhui.fpaplayer.Launcher;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.nativeapis.NativeAPI;

public class ErrorHandler {
    private ErrorHandler() {}

    public static void displayErrorMessageAndExit(Exception errorMsg, String comment, int exitCode) {
        displayErrorMessage(errorMsg, comment);
        Launcher.exitApplication(exitCode);
    }

    public static void displayErrorMessageAndExit(Error errorMsg, String comment, int exitCode) {
        displayErrorMessage(errorMsg, comment);
        Launcher.exitApplication(exitCode);
    }


    /**
     * 显示java异常信息，只显示栈追踪的前5行信息
     * @param errorMsg 异常对象
     * @param comment 附加信息
     */
    public static void displayErrorMessage(Exception errorMsg, String comment) {
        StringBuilder sb = new StringBuilder();
        if (errorMsg != null) {
            if (comment != null)
                sb.append(comment).append('\n');
            sb.append(errorMsg.getMessage()).append('\n');
            for (int i = 0; i < 5; i++) {
                sb.append(errorMsg.getStackTrace()[i]).append('\n');
            }
        } else {
            if (comment != null)
                sb.append(comment);
            else return;
        }
        sb.append("......");
        NativeAPI.displayMessage("错误", sb.toString(), MessageFlags.Icons.ERROR | MessageFlags.Buttons.OK);
    }

    public static void displayErrorMessage(Error errorMsg, String comment) {
        StringBuilder sb = new StringBuilder();
        if (errorMsg != null) {
            if (comment != null)
                sb.append(comment).append('\n');
            sb.append(errorMsg.getMessage()).append('\n');
            for (int i = 0; i < 5; i++) {
                sb.append(errorMsg.getStackTrace()[i]).append('\n');
            }
        } else {
            if (comment != null)
                sb.append(comment);
            else return;
        }
        sb.append("......");
        NativeAPI.displayMessage("错误", sb.toString(), MessageFlags.Icons.ERROR | MessageFlags.Buttons.OK);
    }
}
