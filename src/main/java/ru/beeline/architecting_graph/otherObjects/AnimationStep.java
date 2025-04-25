package ru.beeline.architecting_graph.otherObjects;

import java.util.List;

public class AnimationStep {

    private Integer order;
    private List<String> elements;
    private List<String> relationships;

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public List<String> getElements() {
        return elements;
    }

    public void setElements(List<String> elements) {
        this.elements = elements;
    }

    public List<String> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<String> relationships) {
        this.relationships = relationships;
    }
}
