package org.cloud.sonic.agent.tests.handlers;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.thymeleaf.util.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SinovaAppiumService {
    private DefaultExecutor executor;
    private ExecuteWatchdog watchdog;
    private int appiumPort = 4723; // 默认端口

    /**
     * 启动 Appium 服务（支持自定义参数）
     */
    public void startAppium(String... args) throws IOException {
        // 构建命令
        CommandLine cmd = CommandLine.parse("appium");
        cmd.addArguments("--allow-cors");
        cmd.addArguments(args); // 添加参数（如 --allow-cors, --port 4724）

        // 配置执行器
        executor = new DefaultExecutor();
        watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT); // 无限超时
        executor.setWatchdog(watchdog);

        // 重定向日志到控制台和内存（实时输出 + 错误捕获）
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, System.err);
        executor.setStreamHandler(streamHandler);

        // 异步执行（避免阻塞主线程）
        new Thread(() -> {
            try {
                executor.execute(cmd);
            } catch (ExecuteException e) {
                System.err.println("🛑 Appium execute 异常: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("🛑 Appium IO 异常: " + e.getMessage());
            } finally {
                System.out.println("🛑 Appium 进程终止");
            }
        }).start();

        // 实时打印日志并检测启动成功标志
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待日志初始化
                while (isAppiumRunning()) {
                    String log = outputStream.toString();
                    if (!log.trim().isEmpty()){
                        System.out.print("[Appium] "+log); // 实时输出
                    }
                    outputStream.reset();
                    if (log.contains("Appium REST http interface listener started")) {
                        System.out.println("✅ Appium 服务已启动");
                    }
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 停止 Appium 服务
     */
    public void stopAppium() {
        if (watchdog != null) {
            watchdog.destroyProcess();
            System.out.println("🛑 Appium 服务已终止");
        }
    }

    /**
     * 检查 Appium 是否运行中（基于端口检测）
     */
    public boolean isAppiumRunning() {
        try (Socket socket = new Socket("127.0.0.1", appiumPort)) {
            return true; // 端口可达说明服务运行
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 示例用法
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        SinovaAppiumService manager = new SinovaAppiumService();
        manager.startAppium();

        // 等待服务启动
        Thread.sleep(3000);

//        // 模拟运行后停止
//        Thread.sleep(5000);
//
//        manager.stopAppium();
    }
}
