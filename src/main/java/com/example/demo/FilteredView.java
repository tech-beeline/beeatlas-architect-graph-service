package com.example.demo;

import java.util.List;
import java.util.Map;

public class FilteredView {

    private String key;
    private Integer order;
    private String title;
    private String description;
    private Map<String, Object> properties;
    private String baseViewKey;
    private Mode mode;
    private List<String> tags;

    // Getters and Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getBaseViewKey() {
        return baseViewKey;
    }

    public void setBaseViewKey(String baseViewKey) {
        this.baseViewKey = baseViewKey;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    // Enum for Mode
    public enum Mode {
        Include,
        Exclude
    }
}
