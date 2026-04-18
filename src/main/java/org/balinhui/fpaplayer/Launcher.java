package org.balinhui.fpaplayer;

import javafx.application.Application;
import org.balinhui.fpaplayer.util.Config;

public class Launcher {

    private static boolean readSystem() {
        return SystemInfo.systemName != SystemInfo.Name.UNKNOWN;
    }

    /**
     * 非正常退出，一般可能因为IO错误或应用自身无法处理的异常
     * <p>
     * exitCode:
     * <p>
     * -1 不能在其中运行的操作系统
     * <p>
     * -2 IO错误
     * <p>
     * -3 权限不足
     * <p>
     * -4 重要文件未找到
     * @param exitCode 有如上值
     */
    public static void exitApplication(int exitCode) {
        //TODO 将要在此释放因为意外中断或是异常退出不能正常释放的资源
        Config.storeConfig();
        System.exit(exitCode);
    }

    public static void main(String[] args) {
        if (!readSystem()) exitApplication(-1);
        Application.launch(FPAScreen.class, args);
    }
}
