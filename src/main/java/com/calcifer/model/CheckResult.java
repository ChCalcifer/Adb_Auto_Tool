package com.calcifer.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author CYC
 */
public class CheckResult {
    private final SimpleIntegerProperty index = new SimpleIntegerProperty();
    private final SimpleStringProperty category = new SimpleStringProperty();
    private final SimpleStringProperty itemName = new SimpleStringProperty();
    private final SimpleStringProperty version = new SimpleStringProperty();
    private final SimpleStringProperty prerequisite = new SimpleStringProperty();
    private final SimpleStringProperty propertyValue = new SimpleStringProperty();
    private final SimpleStringProperty expectedValue = new SimpleStringProperty();
    private final SimpleStringProperty resultValue = new SimpleStringProperty();
    private final SimpleStringProperty status = new SimpleStringProperty();
    private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);
    private final SimpleStringProperty note = new SimpleStringProperty();

    public CheckResult(int index, String category, String itemName,
                       String version, String prerequisite,
                       String displayCommand, String expectedValue, String note) {
        this.index.set(index);
        this.category.set(category);
        this.itemName.set(itemName);
        this.version.set(version);
        this.prerequisite.set(prerequisite);
        this.propertyValue.set(displayCommand);
        this.expectedValue.set(expectedValue);
        this.note.set(note);
        this.resultValue.set("");
        this.status.set("Pending");
        this.running.set(false);
    }

    public int getIndex() { return index.get(); }
    public String getCategory() { return category.get(); }
    public String getItemName() { return itemName.get(); }
    public String getVersion() { return version.get(); }
    public String getPrerequisite() { return prerequisite.get(); }
    public String getPropertyValue() { return propertyValue.get(); }
    public String getExpectedValue() { return expectedValue.get(); }
    public String getResultValue() { return resultValue.get(); }
    public String getStatus() { return status.get(); }
    public boolean isRunning() { return running.get(); }
    public String getNote() { return note.get(); }

    public void setResultValue(String value) { resultValue.set(value); }
    public void setStatus(String value) { status.set(value); }
    public void setRunning(boolean value) { running.set(value); }
    public void setNote(String value) { note.set(value); }
    public void setVersion(String value) { version.set(value); }
    public void setPrerequisite(String value) { prerequisite.set(value); }

    public SimpleIntegerProperty indexProperty() { return index; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty itemNameProperty() { return itemName; }
    public SimpleStringProperty versionProperty() { return version; }
    public SimpleStringProperty prerequisiteProperty() { return prerequisite; }
    public SimpleStringProperty propertyValueProperty() { return propertyValue; }
    public SimpleStringProperty expectedValueProperty() { return expectedValue; }
    public SimpleStringProperty resultValueProperty() { return resultValue; }
    public SimpleStringProperty statusProperty() { return status; }
    public SimpleStringProperty noteProperty() { return note; }
}