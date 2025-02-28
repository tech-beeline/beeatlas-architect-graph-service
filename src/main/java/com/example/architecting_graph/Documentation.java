package com.example.architecting_graph;

import java.util.List;

public class Documentation {

    private List<DocumentationSection> sections;
    private List<Decision> decisions;
    private List<Image> images;

    // Getters and Setters

    public List<DocumentationSection> getSections() {
        return sections;
    }

    public void setSections(List<DocumentationSection> sections) {
        this.sections = sections;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }
}