package com.calcifer.model;

import com.calcifer.service.CheckService.ConditionType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CYC
 */
public class CheckItem {
    private final CheckCategory category;
    private final String name;
    private final String version;
    private final String prerequisite;
    private final List<String> adbCommands;
    private final List<String> expectedValues;
    private final Map<Integer, List<String>> commandExpectedMap;
    private final ConditionType conditionType;
    private final String note;
    private final boolean allMustPass;
    private final String launchApp;
    private final String logFilter;
    private final boolean shellSessionRequired;

    public CheckItem(CheckCategory category, String name,
                     String version, String prerequisite,
                     List<String> adbCommands, Map<Integer, List<String>> commandExpectedMap,
                     ConditionType conditionType, boolean allMustPass, String note,
                     String launchApp, String logFilter, boolean shellSessionRequired) {
        this.category = category;
        this.name = name;
        this.version = version;
        this.prerequisite = prerequisite;
        this.adbCommands = adbCommands;
        this.commandExpectedMap = commandExpectedMap;
        this.conditionType = conditionType;
        this.allMustPass = allMustPass;
        this.note = note;
        this.launchApp = launchApp;
        this.logFilter = logFilter;
        this.shellSessionRequired = shellSessionRequired;

        // 初始化兼容字段
        this.expectedValues = commandExpectedMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public CheckCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getPrerequisite() {
        return prerequisite;
    }

    public List<String> getExpectedValues() {
        return expectedValues;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public String getNote() {
        return note;
    }

    public List<String> getAdbCommands() {
        return adbCommands;
    }

    public boolean isAllMustPass() {
        return allMustPass;
    }

    public String getLaunchApp() {
        return launchApp;
    }

    public String getLogFilter() {
        return logFilter;
    }

    public List<String> getExpectedValuesForCommand(int commandIndex) {
        return commandExpectedMap.getOrDefault(commandIndex, Collections.emptyList());
    }

    public Map<Integer, List<String>> getCommandExpectedMap() {
        return commandExpectedMap;
    }

    /**
     * 新增方法
     * */
    public boolean isShellSessionRequired() {
        return shellSessionRequired;
    }
}