package com.example.architecting_graph;

import java.util.List;
import java.util.Map;

public class Relationship {

    private String id;
    private String description;
    private String tags;
    private String url;
    private Map<String, Object> properties;
    private List<Perspective> perspectives;
    private String sourceId;
    private String destinationId;
    private String technology;
    private InteractionStyle interactionStyle;
    private String linkedRelationshipId;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<Perspective> getPerspectives() {
        return perspectives;
    }

    public void setPerspectives(List<Perspective> perspectives) {
        this.perspectives = perspectives;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public InteractionStyle getInteractionStyle() {
        return interactionStyle;
    }

    public void setInteractionStyle(InteractionStyle interactionStyle) {
        this.interactionStyle = interactionStyle;
    }

    public String getLinkedRelationshipId() {
        return linkedRelationshipId;
    }

    public void setLinkedRelationshipId(String linkedRelationshipId) {
        this.linkedRelationshipId = linkedRelationshipId;
    }

    // Enum for InteractionStyle
    public enum InteractionStyle {
        Synchronous,
        Asynchronous
    }
}