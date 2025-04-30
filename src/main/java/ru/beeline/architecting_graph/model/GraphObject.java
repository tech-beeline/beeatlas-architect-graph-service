package ru.beeline.architecting_graph.model;

public class GraphObject {

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

    public static GraphObject createGraphObject(String type, String key, String value) {
        GraphObject node = new GraphObject();
        node.setParameters(type, key, value);
        return node;
    }
}
