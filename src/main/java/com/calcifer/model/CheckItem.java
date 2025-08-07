package com.calcifer.modedl;

/**
 * Author: CYC
 * Time: 2025/7/23 16:17:01
 * Description:
 * Branch:
 * Version: 1.0
 */

import com.calcifer.service.CheckService.ConditionType;

public class CheckItem {
    private final CheckCategory category;
    private final String name;
    private final String adbCommand;
    private final String expectedValue;
    private final ConditionType conditionType;

    public CheckItem(CheckCategory category, String name, String adbCommand,
                     String expectedValue, ConditionType conditionType) {
        this.category = category;
        this.name = name;
        this.adbCommand = adbCommand;
        this.expectedValue = expectedValue;
        this.conditionType = conditionType;
    }

    public CheckCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getAdbCommand() {
        return adbCommand;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }
}
