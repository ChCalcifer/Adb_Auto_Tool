package com.calcifer.model;

/**
 * Author: CYC
 * Time: 2025/7/23 16:16:05
 * Description:
 * Branch:
 * Version: 1.0
 * @author CYC
 */

public class CheckCategory {
    private final String name;
    private final String description;

    public CheckCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
