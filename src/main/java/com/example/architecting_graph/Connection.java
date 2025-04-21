package com.example.architecting_graph;

public class Connection {

    private Node source;
    private Node destination;
    private String relationshipType;
    private String level;
    private String cmdb;

    public Node getSource() {
        return source;
    }

    public Node getDestination() {
        return destination;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getLevel() {
        return level;
    }

    public String getCmdb() {
        return cmdb;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public void setDestination(Node destination) {
        this.destination = destination;
    }

    public void setRelationshipType(String relationType) {
        this.relationshipType = relationType;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setCmdb(String cmdb) {
        this.cmdb = cmdb;
    }
}
