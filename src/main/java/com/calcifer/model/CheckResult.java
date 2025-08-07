package com.calcifer.modedl;

/**
 * Author: CYC
 * Time: 2025/7/23 16:18:05
 * Description:
 * Branch:
 * Version: 1.0
 */

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CheckResult {
    private final SimpleIntegerProperty index = new SimpleIntegerProperty();
    private final SimpleStringProperty category = new SimpleStringProperty();
    private final SimpleStringProperty itemName = new SimpleStringProperty();
    private final SimpleStringProperty propertyValue = new SimpleStringProperty();
    private final SimpleStringProperty expectedValue = new SimpleStringProperty();
    private final SimpleStringProperty resultValue = new SimpleStringProperty();
    private final SimpleStringProperty status = new SimpleStringProperty();
    private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);

    public CheckResult(int index, String category, String itemName,
                       String propertyValue, String expectedValue) {
        this.index.set(index);
        this.category.set(category);
        this.itemName.set(itemName);
        this.propertyValue.set(propertyValue);
        this.expectedValue.set(expectedValue);
        this.resultValue.set("");
        this.status.set("Pending");
        this.running.set(false);
    }

    // Getters and setters
    public int getIndex() { return index.get(); }
    public String getCategory() { return category.get(); }
    public String getItemName() { return itemName.get(); }
    public String getPropertyValue() { return propertyValue.get(); }
    public String getExpectedValue() { return expectedValue.get(); }
    public String getResultValue() { return resultValue.get(); }
    public String getStatus() { return status.get(); }
    public boolean isRunning() { return running.get(); }

    public void setResultValue(String value) { resultValue.set(value); }
    public void setStatus(String value) { status.set(value); }
    public void setRunning(boolean value) { running.set(value); }

    // Property getters
    public SimpleIntegerProperty indexProperty() { return index; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty itemNameProperty() { return itemName; }
    public SimpleStringProperty propertyValueProperty() { return propertyValue; }
    public SimpleStringProperty expectedValueProperty() { return expectedValue; }
    public SimpleStringProperty resultValueProperty() { return resultValue; }
    public SimpleStringProperty statusProperty() { return status; }
    public SimpleBooleanProperty runningProperty() { return running; }
}
