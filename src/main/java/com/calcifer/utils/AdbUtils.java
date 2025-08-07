package com.calcifer.utils;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author CYC
 */
public class AdbUtils {

    public static final String FIND = "find ";
    public static final String DUMPSYS = "dumpsys ";
    public static final String SINGH_1= "| ";
    public static final String SINGH_2= "#";
    public static final String SINGH_3 = "$";
    public static final String SINGH_4 = "\"";
    public static final String GREP = "grep";


    /**
     * 原有方法保持不变
     * */
    public static String executeAdbCommand(String command) {

        // 特殊处理以'find'开头的命令
        if (command.trim().startsWith(FIND) || command.trim().startsWith(DUMPSYS)) {
            return executeShellCommand(command);
        }

        // 特殊处理包含管道的命令
        if (command.contains(SINGH_1) || command.contains(GREP)) {
            return executeShellCommand(command);
        }

        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            String fullCommand = "adb " + command;
            process = Runtime.getRuntime().exec(fullCommand);
            process.waitFor(15, TimeUnit.SECONDS);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    output.append("[ERROR] ").append(errorLine).append("\n");
                }
            }

            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            return "Error: " + e.getMessage();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 新增方法：执行需要保持 shell 会话的命令组
     * */
    public static String executeShellCommands(List<String> commands) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            // 启动 adb shell 进程
            ProcessBuilder pb = new ProcessBuilder("adb", "shell");
            process = pb.start();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(process.getInputStream()))) {

                // 读取并丢弃初始提示符
                String promptLine = reader.readLine();
                if (promptLine != null && (promptLine.contains(SINGH_3) || promptLine.contains(SINGH_2))) {
                    // 跳过提示符
                }

                // 执行所有命令
                for (String cmd : commands) {
                    writer.write(cmd);
                    writer.newLine();
                    writer.flush();

                    // 读取命令输出
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 检查是否出现新的提示符（表示命令结束）
                        if (line.trim().endsWith("$ ") || line.trim().endsWith("# ")) {
                            break;
                        }
                        output.append(line).append("\n");
                    }
                }

                // 退出 shell
                writer.write("exit");
                writer.newLine();
                writer.flush();

                // 等待进程结束
                process.waitFor(10, TimeUnit.SECONDS);
            }
            return output.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 新增方法
     * */
    private static String executeShellCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            // 处理带引号的命令
            if (command.startsWith(SINGH_4) && command.endsWith(SINGH_4)) {
                command = command.substring(1, command.length() - 1);
            }

            Process process = Runtime.getRuntime().exec(new String[]{"adb", "shell", command});
            process.waitFor(15, TimeUnit.SECONDS);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            return output.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}