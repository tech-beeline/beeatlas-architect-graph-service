package com.example.architecting_graph;

import java.util.List;
import java.util.Map;

public class ComponentView {

    private String key;
    private Integer order;
    private String title;
    private String description;
    private Map<String, Object> properties;
    private String containerId;
    private PaperSize paperSize;
    private Dimensions dimensions;
    private AutomaticLayout automaticLayout;
    private List<ElementView> elements;
    private List<RelationshipView> relationships;
    private List<AnimationStep> animations;
    private Boolean externalContainerBoundariesVisible;

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

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public PaperSize getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(PaperSize paperSize) {
        this.paperSize = paperSize;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public AutomaticLayout getAutomaticLayout() {
        return automaticLayout;
    }

    public void setAutomaticLayout(AutomaticLayout automaticLayout) {
        this.automaticLayout = automaticLayout;
    }

    public List<ElementView> getElements() {
        return elements;
    }

    public void setElements(List<ElementView> elements) {
        this.elements = elements;
    }

    public List<RelationshipView> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipView> relationships) {
        this.relationships = relationships;
    }

    public List<AnimationStep> getAnimations() {
        return animations;
    }

    public void setAnimations(List<AnimationStep> animations) {
        this.animations = animations;
    }

    public Boolean getExternalContainerBoundariesVisible() {
        return externalContainerBoundariesVisible;
    }

    public void setExternalContainerBoundariesVisible(Boolean externalContainerBoundariesVisible) {
        this.externalContainerBoundariesVisible = externalContainerBoundariesVisible;
    }

    // Enum for PaperSize
    public enum PaperSize {
        A6_Portrait, A6_Landscape,
        A5_Portrait, A5_Landscape,
        A4_Portrait, A4_Landscape,
        A3_Portrait, A3_Landscape,
        A2_Portrait, A2_Landscape,
        A1_Portrait, A1_Landscape,
        A0_Portrait, A0_Landscape,
        Letter_Portrait, Letter_Landscape,
        Legal_Portrait, Legal_Landscape,
        Slide_4_3, Slide_16_9, Slide_16_10
    }
}