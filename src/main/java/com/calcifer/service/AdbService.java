package com.calcifer.service;

import com.calcifer.model.CheckCategory;
import com.calcifer.model.CheckItem;
import com.calcifer.service.CheckService.ConditionType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author CYC
 */
public class AdbService {
    private static final Map<String, CheckCategory> CATEGORIES = new HashMap<>();
    private static final String EXTERNAL_CONFIG_PATH = "config/check.json";
    private static final String INTERNAL_CONFIG_PATH = "/check.json";

    public static List<CheckItem> createCheckItems() {
        // 1. 优先尝试加载外部配置文件
        List<CheckItem> items = loadExternalConfig();
        if (!items.isEmpty()) {
            return items;
        }

        // 2. 其次尝试加载内置配置文件
        items = loadInternalConfig();
        if (!items.isEmpty()) {
            return items;
        }

        // 3. 最后使用默认配置
        return getDefaultItems();
    }

    private static List<CheckItem> loadExternalConfig() {
        try {
            Path configPath = Paths.get(EXTERNAL_CONFIG_PATH);
            if (!Files.exists(configPath)) {
                return Collections.emptyList();
            }

            try (Reader reader = Files.newBufferedReader(configPath)) {
                return parseConfig(reader);
            }
        } catch (Exception e) {
            System.err.println("加载外部配置文件失败，将尝试内置配置: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<CheckItem> loadInternalConfig() {
        try (InputStream is = AdbService.class.getResourceAsStream(INTERNAL_CONFIG_PATH)) {
            if (is == null) {
                throw new FileNotFoundException("内置配置文件未找到: " + INTERNAL_CONFIG_PATH);
            }

            try (Reader reader = new InputStreamReader(is, "UTF-8")) {
                return parseConfig(reader);
            }
        } catch (Exception e) {
            System.err.println("加载内置配置文件失败，将使用默认配置: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<CheckItem> parseConfig(Reader reader) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<CheckItemConfig>>(){}.getType();
        List<CheckItemConfig> configs = gson.fromJson(reader, listType);

        return configs.stream()
                .map(AdbService::convertToCheckItem)
                .collect(Collectors.toList());
    }

    private static CheckItem convertToCheckItem(CheckItemConfig config) {
        CheckCategory category = CATEGORIES.computeIfAbsent(
                config.category,
                k -> new CheckCategory(config.category, config.category)
        );

        ConditionType conditionType = ConditionType.valueOf(config.conditionType);

        // 处理期待值映射
        Map<Integer, List<String>> expectedMap = new HashMap<>();
        if (config.expectedValues != null) {
            // 旧格式兼容
            for (int i = 0; i < config.adbCommands.size() && i < config.expectedValues.size(); i++) {
                expectedMap.put(i, Collections.singletonList(config.expectedValues.get(i)));
            }
        } else if (config.commandExpectedMap != null) {
            // 新格式
            expectedMap = config.commandExpectedMap;
        }

        // 判断是否需要shell会话
        boolean shellRequired = config.adbCommands.size() > 1 &&
                config.adbCommands.getFirst().equals("shell");

        return new CheckItem(
                category,
                config.name,
                config.version,
                config.prerequisite,
                config.adbCommands,
                expectedMap,
                conditionType,
                config.allMustPass,
                config.note,
                config.launchApp,
                config.logFilter,
                shellRequired
        );
    }

    private static List<CheckItem> getDefaultItems() {
        Map<Integer, List<String>> expectedMap = new HashMap<>();
        expectedMap.put(2, Arrays.asList(
                "libMEOW_gift:open /vendor/etc/arc.ini",
                "libMEOW_gift:open /data/performance/gift/arc.ini"
        ));

        return Arrays.asList(
                new CheckItem(
                        new CheckCategory("Browser", "浏览器检查"),
                        "Chrome_ARC_Support",
                        "1.0",
                        "Chrome installed",
                        Arrays.asList(
                                "shell logcat -c",
                                "shell am start -n com.android.chrome/com.google.android.apps.chrome.Main",
                                "shell \"logcat -d | grep -e MEOW -e libARC\""
                        ),
                        expectedMap,
                        ConditionType.LOG_FILTER,
                        false,
                        "检查Chrome的ARC支持状态",
                        "com.android.chrome",
                        "grep -e MEOW -e libARC",
                        false
                )
        );
    }

    /**
     * 配置初始化检查
     * */
    public static void initializeConfigDirectory() {
        Path configDir = Paths.get("config");
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                // 可选：将内置配置复制到外部目录作为模板
                copyInternalConfigToExternal();
            } catch (IOException e) {
                System.err.println("创建配置目录失败: " + e.getMessage());
            }
        }
    }

    private static void copyInternalConfigToExternal() {
        try (InputStream is = AdbService.class.getResourceAsStream(INTERNAL_CONFIG_PATH);
             OutputStream os = Files.newOutputStream(Paths.get(EXTERNAL_CONFIG_PATH))) {
            if (is == null) {return;}

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            System.err.println("复制内置配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 配置项内部类
     * */
    private static class CheckItemConfig {
        String category;
        String name;
        String version;
        String prerequisite;
        List<String> adbCommands;
        List<String> expectedValues;
        String conditionType;
        boolean allMustPass;
        String note;
        String launchApp;
        String logFilter;
        Map<Integer, List<String>> commandExpectedMap;
    }
}