package com.example.architecting_graph;

public class Node {

    private String type;
    private String key;
    private String value;

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setParameters(String type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
}
