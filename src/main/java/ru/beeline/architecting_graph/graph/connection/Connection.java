package ru.beeline.architecting_graph.graph.connection;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;

public class Connection {

    private GraphObject source;
    private GraphObject destination;
    private String relationshipType;
    private String level;
    private String cmdb;

    public GraphObject getSource() {
        return source;
    }

    public GraphObject getDestination() {
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

    public void setSource(GraphObject source) {
        this.source = source;
    }

    public void setDestination(GraphObject destination) {
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
